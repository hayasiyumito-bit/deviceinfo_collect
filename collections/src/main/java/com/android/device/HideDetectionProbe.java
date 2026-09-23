package com.android.device;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.android.device.Jni.JniPropertyHelper;
import com.android.utils.Cmd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 反-隐藏检测：专门识别 Xposed/LSPosed 反检测模块（如 YumyHook）的隐藏手法。
 *
 * <ul>
 *   <li>proc 重定向：native open(/proc/self/maps) 后 fd 指向普通临时文件而非 /proc。</li>
 *   <li>过滤临时文件残留：模块在自身缓存目录写入 *_filtered_* / .yh_p* 快照文件。</li>
 *   <li>PackageManager 隐藏：API 列表缺失但 shell `pm list packages` 可见的敏感包。</li>
 * </ul>
 */
public final class HideDetectionProbe {

    private static final String TAG = "HideDetectionProbe";

    /** 过滤临时文件的文件名特征（YumyHook Java 侧 createTempFile + native .yh_p*）。 */
    private static final String[] FILTER_TEMP_PREFIXES = {
            "maps_filtered_",
            "smaps_filtered_",
            "status_filtered_",
            "mountinfo_filtered_",
            "mounts_filtered_",
            ".yh_p",
    };

    /** 高信号：root / hook / 抓包 / 多开 / 代理 管理器包名，用于 API vs shell 交叉核对。 */
    private static final String[] SENSITIVE_PACKAGES = {
            "com.topjohnwu.magisk",
            "io.github.huskydg.magisk",
            "io.github.vvb2060.magisk",
            "me.weishu.kernelsu",
            "com.rifsxd.ksunext",
            "me.bmax.apatch",
            "org.lsposed.manager",
            "org.lsposed.lspd",
            "de.robv.android.xposed.installer",
            "com.tsng.hidemyapplist",
            "io.github.suika.hidemyapplist",
            "cn.geektang.privacyspace",
            "com.omarea.vtools",
            "eu.chainfire.supersu",
            "com.speedsoftware.rootexplorer",
            "com.guoshi.httpcanary",
            "com.applisto.appcloner",
            "com.lody.virtual",
            "com.gameguardian.devtools",
            "com.v2ray.ang",
            "com.github.kr328.clash",
            "com.github.metacubex.clash_meta",
            "com.github.shadowsocks",
    };

    private HideDetectionProbe() {
    }

    public static JSONObject probe(Context context) {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        boolean detected = false;
        try {
            JSONObject redirect = probeProcRedirect(reasons);
            result.put("procRedirect", redirect);
            detected |= redirect.optBoolean("anomaly", false);

            // inline-svc 直读 maps：绕过 libc Hook，暴露被隐藏的注入库（最强信号）
            JSONObject svcMaps = probeInlineSvcMaps(reasons);
            result.put("inlineSvcMaps", svcMaps);
            detected |= svcMaps.optBoolean("hooked", false);

            JSONArray tempArtifacts = scanFilterTempArtifacts(context);
            result.put("filterTempArtifacts", tempArtifacts);
            if (tempArtifacts.length() > 0) {
                detected = true;
                for (int i = 0; i < tempArtifacts.length(); i++) {
                    reasons.put(com.android.device.i18n.AppLocale.tr("proc 过滤临时文件残留: ", "proc-filter temp-file residue: ") + tempArtifacts.optString(i));
                }
            }

            JSONObject pmHide = probePackageManagerHiding(context, reasons);
            result.put("packageManagerHiding", pmHide);
            detected |= pmHide.optBoolean("hidingDetected", false);

            result.put("hookHideDetected", detected);
            result.put("reasons", reasons);
        } catch (JSONException e) {
            Log.e(TAG, "HideDetectionProbe failed", e);
            try {
                result.put("error", e.getMessage());
            } catch (JSONException ignored) {
            }
        }
        return result;
    }

    // --- A. proc 重定向探测（native）---
    private static JSONObject probeProcRedirect(JSONArray reasons) throws JSONException {
        JSONObject probe;
        String raw = JniPropertyHelper.getProcRedirectProbe();
        if (raw != null && raw.trim().startsWith("{")) {
            probe = new JSONObject(raw);
        } else {
            probe = new JSONObject();
            probe.put("raw", raw != null ? raw : "");
        }
        if (probe.optBoolean("mapsRedirected", false)) {
            reasons.put(com.android.device.i18n.AppLocale.tr("/proc/self/maps 被重定向到伪造文件: ", "/proc/self/maps redirected to a fake file: ") + probe.optString("mapsFdTarget", ""));
        }
        if (probe.optBoolean("mapsFdOffProcFs", false)) {
            reasons.put(com.android.device.i18n.AppLocale.tr("/proc/self/maps 句柄不在 procfs 上（st_dev 不一致），疑似临时文件重定向", "/proc/self/maps handle not on procfs (st_dev mismatch), suspected temp-file redirection"));
        }
        if (probe.optBoolean("statSizeAnomaly", false)) {
            reasons.put(com.android.device.i18n.AppLocale.tr("/proc/self/maps stat 大小非 0（真实 procfs 恒为 0），疑似 stat 被篡改: ", "/proc/self/maps stat size non-zero (real procfs is always 0), suspected stat tampering: ")
                    + probe.optLong("statSize", -1));
        }
        if (probe.optBoolean("readlinkHookDetected", false)) {
            reasons.put(com.android.device.i18n.AppLocale.tr("readlink 被 Hook：libc 与原始 syscall 结果不一致（libc=", "readlink hooked: libc vs raw syscall result mismatch (libc=")
                    + probe.optString("mapsFdTarget", "") + " raw="
                    + probe.optString("rawFdTarget", "") + "）");
        }
        if (probe.optBoolean("fstatHookDetected", false)) {
            reasons.put(com.android.device.i18n.AppLocale.tr("fstat 被 Hook：libc 与原始 syscall 的 st_dev/st_size 不一致，疑似伪造 procfs 句柄", "fstat hooked: libc vs raw syscall st_dev/st_size mismatch, suspected fake procfs handle"));
        }
        return probe;
    }

    // --- G. inline svc 直读 /proc/self/maps，检出被 libc Hook 隐藏的注入库 ---
    private static JSONObject probeInlineSvcMaps(JSONArray reasons) throws JSONException {
        JSONObject probe;
        String raw = JniPropertyHelper.getInlineSvcMapsProbe();
        if (raw != null && raw.trim().startsWith("{")) {
            probe = new JSONObject(raw);
        } else {
            probe = new JSONObject();
            probe.put("raw", raw != null ? raw : "");
        }
        JSONArray hidden = probe.optJSONArray("hiddenFromLibc");
        JSONArray rawHits = probe.optJSONArray("rawHits");
        if (probe.optBoolean("concealed", false) && hidden != null && hidden.length() > 0) {
            // inline svc 看得见、libc 看不见 → 模块正在主动隐藏自己的注入
            reasons.put(com.android.device.i18n.AppLocale.tr(
                    "Hook 注入被隐藏（inline-svc 绕过 libc 过滤后暴露）: ",
                    "Hook injection concealed (exposed via inline-svc bypassing libc filter): ")
                    + join(hidden));
        } else if (probe.optBoolean("hooked", false) && rawHits != null && rawHits.length() > 0) {
            // 注入库存在（未被隐藏或未过滤）
            reasons.put(com.android.device.i18n.AppLocale.tr(
                    "检测到 Hook 注入库（/proc/self/maps）: ",
                    "Hook injection library detected (/proc/self/maps): ")
                    + join(rawHits));
        }
        // 结构信号：rwx 可写可执行页（inline-hook 补丁 / 注入蹦床），改名藏不住
        if (probe.optBoolean("patched", false)) {
            int rwxFile = probe.optInt("rwxFile", 0);
            int rwxAnon = probe.optInt("rwxAnon", 0);
            reasons.put(com.android.device.i18n.AppLocale.tr(
                    "检测到 rwx 可写可执行内存（违反 W^X，疑似 inline-hook 补丁/注入蹦床）：文件页 ",
                    "rwx writable-executable memory detected (W^X violation, suspected inline-hook patch/injection trampoline): file-backed ")
                    + rwxFile
                    + com.android.device.i18n.AppLocale.tr(" 处，匿名 ", ", anonymous ")
                    + rwxAnon
                    + com.android.device.i18n.AppLocale.tr(" 处", ""));
        }
        return probe;
    }

    private static String join(JSONArray arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr.optString(i));
        }
        return sb.toString();
    }

    // --- F. 过滤临时文件残留扫描 ---
    private static JSONArray scanFilterTempArtifacts(Context context) {
        Set<File> dirs = new LinkedHashSet<>();
        if (context != null) {
            addIfNotNull(dirs, context.getCacheDir());
            addIfNotNull(dirs, context.getFilesDir());
            addIfNotNull(dirs, context.getCodeCacheDir());
            File dataDir = null;
            try {
                dataDir = new File(context.getApplicationInfo().dataDir);
            } catch (RuntimeException ignored) {
            }
            addIfNotNull(dirs, dataDir);
        }
        // 系统临时目录（部分实现 createTempFile 落到此处）
        String javaTmp = System.getProperty("java.io.tmpdir");
        if (javaTmp != null) {
            addIfNotNull(dirs, new File(javaTmp));
        }

        JSONArray hits = new JSONArray();
        for (File dir : dirs) {
            collectFilterTempFiles(dir, hits);
        }
        return hits;
    }

    private static void collectFilterTempFiles(File dir, JSONArray hits) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = f.getName();
            for (String prefix : FILTER_TEMP_PREFIXES) {
                if (name.startsWith(prefix)) {
                    hits.put(f.getAbsolutePath());
                    break;
                }
            }
        }
    }

    private static void addIfNotNull(Set<File> dirs, File dir) {
        if (dir != null) {
            dirs.add(dir);
        }
    }

    // --- D. PackageManager 隐藏交叉核对（API vs shell）---
    private static JSONObject probePackageManagerHiding(Context context, JSONArray reasons)
            throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray hiddenHits = new JSONArray();
        if (context == null) {
            result.put("hidingDetected", false);
            result.put("hiddenPackages", hiddenHits);
            return result;
        }
        PackageManager pm = context.getPackageManager();
        Set<String> shellPackages = readShellPackages();
        result.put("shellPackageCount", shellPackages.size());

        for (String pkg : SENSITIVE_PACKAGES) {
            boolean apiVisible = isApiVisible(pm, pkg);
            boolean shellVisible = shellPackages.contains(pkg);
            if (shellVisible && !apiVisible) {
                hiddenHits.put(pkg);
                reasons.put(com.android.device.i18n.AppLocale.tr("PackageManager 隐藏（shell 可见 / API 不可见）: ", "PackageManager hidden (visible to shell / invisible to API): ") + pkg);
            }
        }

        // 通用差集：shell 可见但 API 完全枚举不到（仅在 shell 列表非空时可信）
        if (!shellPackages.isEmpty()) {
            Set<String> apiPackages = readApiPackages(pm);
            result.put("apiPackageCount", apiPackages.size());
            if (!apiPackages.isEmpty()) {
                for (String pkg : shellPackages) {
                    if (!apiPackages.contains(pkg) && isSensitiveName(pkg) && !contains(hiddenHits, pkg)) {
                        hiddenHits.put(pkg);
                        reasons.put(com.android.device.i18n.AppLocale.tr("PackageManager 隐藏（shell 可见 / API 枚举缺失）: ", "PackageManager hidden (visible to shell / missing from API enumeration): ") + pkg);
                    }
                }
            }
        }

        result.put("hiddenPackages", hiddenHits);
        result.put("hidingDetected", hiddenHits.length() > 0);
        return result;
    }

    private static boolean isApiVisible(PackageManager pm, String pkg) {
        try {
            pm.getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Set<String> readApiPackages(PackageManager pm) {
        Set<String> out = new LinkedHashSet<>();
        try {
            for (android.content.pm.PackageInfo info : pm.getInstalledPackages(0)) {
                if (info != null && info.packageName != null) {
                    out.add(info.packageName);
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "API package enumeration failed", e);
        }
        return out;
    }

    private static Set<String> readShellPackages() {
        Set<String> out = new LinkedHashSet<>();
        String output = Cmd.exe("pm list packages 2>/dev/null");
        if (output == null || output.trim().isEmpty()) {
            return out;
        }
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package:")) {
                trimmed = trimmed.substring("package:".length()).trim();
            }
            if (!trimmed.isEmpty() && !trimmed.contains(" ")) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static boolean isSensitiveName(String pkg) {
        String lower = pkg.toLowerCase(Locale.US);
        return lower.contains("magisk") || lower.contains("kernelsu") || lower.contains("ksu")
                || lower.contains("apatch") || lower.contains("lsposed") || lower.contains("xposed")
                || lower.contains("hidemyapplist") || lower.contains("v2ray") || lower.contains("clash")
                || lower.contains("shadowsocks") || lower.contains("appcloner")
                || lower.contains("gameguardian") || lower.contains("httpcanary");
    }

    private static boolean contains(JSONArray array, String value) {
        for (int i = 0; i < array.length(); i++) {
            if (value.equals(array.optString(i))) {
                return true;
            }
        }
        return false;
    }
}
