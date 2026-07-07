package com.android.device;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.android.device.Jni.JniPropertyHelper;
import com.android.utils.Cmd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * Root 框架分类检测：Magisk、KernelSU、APatch、系统 su。
 */
public final class RootFrameworkDetector {

    private static final String TAG = "RootFrameworkDetector";

    private static final String ID_MAGISK = "magisk";
    private static final String ID_KERNELSU = "kernelsu";
    private static final String ID_APATCH = "apatch";
    private static final String ID_SYSTEM_SU = "systemSu";

    private static final String[] MAGISK_PATHS = {
            "/sbin/magisk",
            "/sbin/magiskpolicy",
            "/sbin/.magisk",
            "/debug_ramdisk/magisk",
            "/debug_ramdisk/.magisk",
            "/data/adb/magisk",
            "/data/adb/magisk.db",
            "/data/adb/magisk.img",
            "/data/adb/magisk/busybox",
            "/data/adb/modules",
            "/data/adb/post-fs-data.d",
            "/data/adb/service.d",
            "/cache/magisk.log",
            "/data/magisk.apk",
            "/system/xbin/magisk",
            "/system/bin/magisk",
            "/vendor/bin/magisk",
            "/product/bin/magisk",
            "/persist/magisk",
            "/metadata/magisk",
            "/system/app/Magisk",
            "/system/etc/init/magisk.rc"
    };

    private static final String[] KERNELSU_PATHS = {
            "/data/adb/ksu",
            "/data/adb/kernelsu",
            "/data/adb/ksud",
            "/data/adb/ksu/bin",
            "/data/adb/ksu/modules",
            "/sys/fs/ksu",
            "/dev/ksu",
            "/proc/ksu",
            "/data/adb/.ksu",
            "/data/adb/ksu/.allowlist"
    };

    private static final String[] APATCH_PATHS = {
            "/data/adb/ap",
            "/data/adb/apd",
            "/data/adb/ap/bin",
            "/data/adb/apd.apk",
            "/data/adb/ap/modules",
            "/data/adb/ap/log",
            "/data/adb/ap/superkey",
            "/data/adb/apd/superkey",
            "/data/adb/ap/package_config",
            "/data/adb/apd/package_config"
    };

    private static final String[] SYSTEM_SU_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sbin/su",
            "/system/usr/we-need-root/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU",
            "/system/etc/init.d/99SuperSUDaemon",
            "/system/xbin/daemonsu",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/cache/su",
            "/dev/com.koushikdutta.superuser.daemon/"
    };

    private static final String[] MAGISK_PACKAGES = {
            "com.topjohnwu.magisk",
            "io.github.huskydg.magisk"
    };

    private static final String[] KERNELSU_PACKAGES = {
            "me.weishu.kernelsu",
            "io.github.huskydg.kernelsu",
            "com.kernel.su",
            "kernelsu"
    };

    private static final String[] APATCH_PACKAGES = {
            "me.bmax.apatch",
            "com.bmax.apatch",
            "bmax.apatch"
    };

    private static final String[] SYSTEM_SU_PACKAGES = {
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
    };

    private static final String[] MAGISK_MAPS_KEYWORDS = {
            "magisk", "zygisk", "magiskpolicy", "magisk32", "magisk64", "resetprop"
    };

    private static final String[] KERNELSU_MAPS_KEYWORDS = {
            "kernelsu", "ksu", "ksud", "kernel_su"
    };

    private static final String[] APATCH_MAPS_KEYWORDS = {
            "apatch", "apd", "bmax", "/data/adb/ap"
    };

    private static final String[] SYSTEM_SU_MAPS_KEYWORDS = {
            "/system/bin/su", "/system/xbin/su", "supersu", "daemonsu", "superuser"
    };

    private static final String[] MAGISK_PROP_KEYS = {
            "init.svc.magisk",
            "init.svc.magisk_daemon",
            "init.svc.magisk_service",
            "ro.magisk.version",
            "persist.magisk.version"
    };

    private static final String[] KERNELSU_PROP_KEYS = {
            "persist.sys.kernelsu",
            "ro.kernel.su",
            "persist.sys.ksu",
            "init.svc.ksud",
            "init.svc.kernelsu"
    };

    private static final String[] APATCH_PROP_KEYS = {
            "init.svc.apd",
            "init.svc.apatch",
            "ro.apatch.version",
            "persist.apatch.version"
    };

    private static final String[] BOOT_UNLOCK_PROPS = {
            "ro.boot.verifiedbootstate",
            "ro.boot.flash.locked",
            "ro.boot.vbmeta.device_state",
            "ro.boot.warranty_bit",
            "ro.boot.veritymode"
    };

    private RootFrameworkDetector() {
    }

    public static JSONObject probe(Context context) {
        JSONObject result = new JSONObject();
        JSONArray combinedReasons = new JSONArray();
        try {
            SharedContext shared = SharedContext.collect(context);

            JSONObject magisk = probeFramework(
                    ID_MAGISK, "Magisk", MAGISK_PATHS, MAGISK_PACKAGES, MAGISK_MAPS_KEYWORDS,
                    MAGISK_PROP_KEYS, shared, true);
            JSONObject kernelsu = probeFramework(
                    ID_KERNELSU, "KernelSU", KERNELSU_PATHS, KERNELSU_PACKAGES, KERNELSU_MAPS_KEYWORDS,
                    KERNELSU_PROP_KEYS, shared, false);
            JSONObject apatch = probeFramework(
                    ID_APATCH, "APatch", APATCH_PATHS, APATCH_PACKAGES, APATCH_MAPS_KEYWORDS,
                    APATCH_PROP_KEYS, shared, false);
            JSONObject systemSu = probeSystemSu(shared);

            JSONObject frameworks = new JSONObject();
            frameworks.put(ID_MAGISK, magisk);
            frameworks.put(ID_KERNELSU, kernelsu);
            frameworks.put(ID_APATCH, apatch);
            frameworks.put(ID_SYSTEM_SU, systemSu);

            boolean magiskDetected = magisk.optBoolean("detected", false);
            boolean kernelsuDetected = kernelsu.optBoolean("detected", false);
            boolean apatchDetected = apatch.optBoolean("detected", false);
            boolean systemSuDetected = systemSu.optBoolean("detected", false);
            boolean detected = magiskDetected || kernelsuDetected || apatchDetected || systemSuDetected;
            boolean hideSuspected = shared.hideSuspected && !detected;

            mergeReasons(combinedReasons, magisk.optJSONArray("reasons"));
            mergeReasons(combinedReasons, kernelsu.optJSONArray("reasons"));
            mergeReasons(combinedReasons, apatch.optJSONArray("reasons"));
            mergeReasons(combinedReasons, systemSu.optJSONArray("reasons"));
            if (hideSuspected) {
                combinedReasons.put("疑似 Root 隐藏：maps/mount/Native 有信号但各框架路径不可见");
            }

            JSONObject sharedIndicators = new JSONObject();
            sharedIndicators.put("mapsHits", shared.mapsHits);
            sharedIndicators.put("mountHits", shared.mountHits);
            sharedIndicators.put("mountInfoHits", shared.mountInfoHits);
            sharedIndicators.put("bootUnlockSignals", shared.bootUnlockSignals);
            sharedIndicators.put("buildMismatches", shared.buildMismatches);
            sharedIndicators.put("selinuxMode", shared.selinuxMode);
            sharedIndicators.put("nativeProbe", shared.nativeProbe);
            sharedIndicators.put("javaNativeMismatches", shared.javaNativeMismatches);
            sharedIndicators.put("envHits", shared.envHits);
            sharedIndicators.put("hideSuspected", hideSuspected);

            result.put("detected", detected || hideSuspected);
            result.put("magiskDetected", magiskDetected);
            result.put("kernelsuDetected", kernelsuDetected);
            result.put("apatchDetected", apatchDetected);
            result.put("systemSuDetected", systemSuDetected);
            result.put("hideSuspected", hideSuspected);
            result.put("frameworks", frameworks);
            result.put("sharedIndicators", sharedIndicators);
            result.put("reasons", combinedReasons);
            // 兼容旧字段：magisk 块保留为完整探测结果
            result.put("indicators", sharedIndicators);
        } catch (JSONException e) {
            Log.e(TAG, "Root framework probe failed", e);
            try {
                result.put("error", e.getMessage());
            } catch (JSONException ignored) {
            }
        }
        return result;
    }

    private static JSONObject probeFramework(
            String id,
            String displayName,
            String[] paths,
            String[] packages,
            String[] mapsKeywords,
            String[] propKeys,
            SharedContext shared,
            boolean includeMagiskShell
    ) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();

        JSONArray matchedPaths = scanExistingPaths(paths);
        JSONArray packageHits = filterPackages(shared.installedPackages, packages);
        JSONArray mapsHits = filterKeywordHits(shared.mapsHits, mapsKeywords);
        JSONArray mountHits = filterKeywordHits(shared.mountHits, mapsKeywords);
        JSONArray mountInfoHits = filterKeywordHits(shared.mountInfoHits, mapsKeywords);
        JSONArray propHits = collectPropertyHits(shared.allProps, propKeys);
        JSONArray shellHits = includeMagiskShell
                ? collectMagiskShellHits() : collectFrameworkShellHits(id);
        JSONArray envHits = filterKeywordHits(shared.envHits, mapsKeywords);

        boolean pathHit = matchedPaths.length() > 0;
        boolean packageHit = packageHits.length() > 0;
        boolean mapsHit = mapsHits.length() > 0;
        boolean mountHit = mountHits.length() > 0 || mountInfoHits.length() > 0;
        boolean propHit = propHits.length() > 0;
        boolean shellHit = shellHits.length() > 0;
        boolean envHit = envHits.length() > 0;
        boolean suLinked = ID_MAGISK.equals(id) && shared.suReadlink.toLowerCase(Locale.US).contains("magisk");
        boolean suLinkedKsu = ID_KERNELSU.equals(id) && shared.suReadlink.toLowerCase(Locale.US).contains("ksu");
        boolean suLinkedApd = ID_APATCH.equals(id) && shared.suReadlink.toLowerCase(Locale.US).contains("apd");

        indicators.put("matchedPaths", matchedPaths);
        indicators.put("packageHits", packageHits);
        indicators.put("mapsHits", mapsHits);
        indicators.put("mountHits", mountHits);
        indicators.put("mountInfoHits", mountInfoHits);
        indicators.put("propertyHits", propHits);
        indicators.put("shellHits", shellHits);
        indicators.put("envHits", envHits);
        indicators.put("suReadlink", shared.suReadlink);

        appendPathReasons(reasons, displayName, matchedPaths);
        appendArrayReasons(reasons, packageHits, displayName + " 安装包");
        appendArrayReasons(reasons, mapsHits, displayName + " maps 命中");
        appendArrayReasons(reasons, mountHits, displayName + " mounts 命中");
        appendArrayReasons(reasons, mountInfoHits, displayName + " mountinfo 命中");
        appendArrayReasons(reasons, propHits, displayName + " 属性");
        appendArrayReasons(reasons, shellHits, displayName + " Shell");
        appendArrayReasons(reasons, envHits, displayName + " 环境变量");
        if (suLinked) {
            reasons.put("su 链接到 Magisk: " + shared.suReadlink);
        }
        if (suLinkedKsu) {
            reasons.put("su 链接到 KernelSU: " + shared.suReadlink);
        }
        if (suLinkedApd) {
            reasons.put("su 链接到 APatch: " + shared.suReadlink);
        }

        boolean detected = pathHit || packageHit || mapsHit || mountHit || propHit || shellHit
                || envHit || suLinked || suLinkedKsu || suLinkedApd;

        result.put("id", id);
        result.put("displayName", displayName);
        result.put("detected", detected);
        result.put("indicators", indicators);
        result.put("reasons", reasons);
        return result;
    }

    private static JSONObject probeSystemSu(SharedContext shared) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();

        JSONArray matchedPaths = scanExistingPaths(SYSTEM_SU_PATHS);
        JSONArray packageHits = filterPackages(shared.installedPackages, SYSTEM_SU_PACKAGES);
        JSONArray mapsHits = filterKeywordHits(shared.mapsHits, SYSTEM_SU_MAPS_KEYWORDS);
        JSONArray shellHits = collectSystemSuShellHits(shared);
        boolean suWhichHit = !shared.suWhichPath.isEmpty()
                && !shared.suWhichPath.contains("not found");
        boolean suGranted = RootAccessHelper.isRootGranted();
        boolean idShowsRoot = shared.idOutput.contains("uid=0") || shared.idOutput.contains("(root)");
        boolean suLinkedFramework = shared.suReadlink.toLowerCase(Locale.US).contains("magisk")
                || shared.suReadlink.toLowerCase(Locale.US).contains("ksu")
                || shared.suReadlink.toLowerCase(Locale.US).contains("apd");

        indicators.put("matchedPaths", matchedPaths);
        indicators.put("packageHits", packageHits);
        indicators.put("mapsHits", mapsHits);
        indicators.put("shellHits", shellHits);
        indicators.put("suWhichPath", shared.suWhichPath);
        indicators.put("suReadlink", shared.suReadlink);
        indicators.put("idOutput", shared.idOutput);
        indicators.put("accessGranted", suGranted);
        indicators.put("accessDetail", RootAccessHelper.getAttemptDetail());

        appendPathReasons(reasons, "系统 su", matchedPaths);
        appendArrayReasons(reasons, packageHits, "系统 Root 管理器");
        appendArrayReasons(reasons, mapsHits, "系统 su maps 命中");
        appendArrayReasons(reasons, shellHits, "系统 su Shell");
        if (suWhichHit) {
            reasons.put("which su 可用: " + shared.suWhichPath);
        }
        if (!shared.suReadlink.isEmpty() && !suLinkedFramework) {
            reasons.put("su 符号链接: " + shared.suReadlink);
        }
        if (suGranted) {
            reasons.put("su 授权探测成功: " + RootAccessHelper.getAttemptDetail());
        }
        if (idShowsRoot) {
            reasons.put("id 显示 root: " + shared.idOutput);
        }

        boolean detected = matchedPaths.length() > 0 || packageHits.length() > 0 || mapsHits.length() > 0
                || shellHits.length() > 0 || suWhichHit || suGranted || idShowsRoot;

        result.put("id", ID_SYSTEM_SU);
        result.put("displayName", "系统 su");
        result.put("detected", detected);
        result.put("indicators", indicators);
        result.put("reasons", reasons);
        return result;
    }

    private static final class SharedContext {
        JSONArray mapsHits = new JSONArray();
        JSONArray mountHits = new JSONArray();
        JSONArray mountInfoHits = new JSONArray();
        JSONArray bootUnlockSignals = new JSONArray();
        JSONArray buildMismatches = new JSONArray();
        JSONArray envHits = new JSONArray();
        JSONArray javaNativeMismatches = new JSONArray();
        JSONArray installedPackages = new JSONArray();
        JSONObject nativeProbe = new JSONObject();
        String allProps = "";
        String selinuxMode = "";
        String suReadlink = "";
        String suWhichPath = "";
        String idOutput = "";
        boolean hideSuspected;

        static SharedContext collect(Context context) throws JSONException {
            SharedContext ctx = new SharedContext();
            ctx.allProps = normalize(Cmd.exe("getprop"));
            ctx.mapsHits = scanProcFile("/proc/self/maps", concatAll(
                    MAGISK_MAPS_KEYWORDS, KERNELSU_MAPS_KEYWORDS, APATCH_MAPS_KEYWORDS, SYSTEM_SU_MAPS_KEYWORDS));
            ctx.mountHits = scanProcMounts();
            ctx.mountInfoHits = scanProcFile("/proc/self/mountinfo", concatAll(
                    MAGISK_MAPS_KEYWORDS, KERNELSU_MAPS_KEYWORDS, APATCH_MAPS_KEYWORDS));
            ctx.bootUnlockSignals = collectBootUnlockSignals();
            ctx.buildMismatches = collectBuildMismatches();
            ctx.envHits = scanEnvironmentVariables();
            ctx.nativeProbe = parseNativeProbe();
            ctx.selinuxMode = normalize(Cmd.exe("getenforce"));
            ctx.suReadlink = normalize(Cmd.exe("readlink /system/bin/su 2>/dev/null"));
            ctx.suWhichPath = normalize(Cmd.exe("which su 2>/dev/null"));
            ctx.idOutput = normalize(Cmd.exe("id"));
            ctx.installedPackages = scanAllInstalledPackages(context);
            JSONArray allPaths = mergeJsonArrays(
                    scanExistingPaths(MAGISK_PATHS),
                    scanExistingPaths(KERNELSU_PATHS),
                    scanExistingPaths(APATCH_PATHS),
                    scanExistingPaths(SYSTEM_SU_PATHS)
            );
            ctx.javaNativeMismatches = detectJavaNativePathMismatch(ctx.nativeProbe, allPaths);
            boolean mapsOrMountHit = ctx.mapsHits.length() > 0 || ctx.mountHits.length() > 0
                    || ctx.mountInfoHits.length() > 0;
            boolean nativeHit = ctx.nativeProbe.optBoolean("anyHit", false);
            boolean pathVisible = allPaths.length() > 0;
            ctx.hideSuspected = (mapsOrMountHit || nativeHit || ctx.bootUnlockSignals.length() > 0
                    || ctx.javaNativeMismatches.length() > 0)
                    && !pathVisible && !RootAccessHelper.isRootGranted();
            return ctx;
        }
    }

    private static JSONArray collectMagiskShellHits() throws JSONException {
        JSONArray hits = new JSONArray();
        addShellHit(hits, "getprop_magisk", Cmd.exe("getprop | grep -i magisk"), "magisk", "zygisk");
        addShellHit(hits, "ls_data_adb", Cmd.exe("ls -la /data/adb 2>&1"), "magisk", "modules", "ksu", "ap");
        addShellHit(hits, "test_sbin_magisk", Cmd.exe("test -f /sbin/magisk && echo exists"), "exists");
        addShellHit(hits, "which_magisk", Cmd.exe("which magisk 2>/dev/null"), "magisk");
        addShellHit(hits, "magisk_version", Cmd.exe("magisk -v 2>/dev/null"), "magisk");
        addShellHit(hits, "mounts_magisk", Cmd.exe("cat /proc/mounts | grep -i magisk"), "magisk");
        addShellHit(hits, "resetprop_check", Cmd.exe("resetprop 2>&1 | head -1"), "resetprop", "magisk");
        return hits;
    }

    private static JSONArray collectFrameworkShellHits(String frameworkId) throws JSONException {
        JSONArray hits = new JSONArray();
        if (ID_KERNELSU.equals(frameworkId)) {
            addShellHit(hits, "ksud_version", Cmd.exe("ksud -V 2>/dev/null"), "ksu", "kernel");
            addShellHit(hits, "which_ksud", Cmd.exe("which ksud 2>/dev/null"), "ksud");
            addShellHit(hits, "ls_ksu_dir", Cmd.exe("ls -la /data/adb/ksu 2>&1"), "ksu", "kernelsu");
            addShellHit(hits, "proc_version_ksu", Cmd.exe("cat /proc/version 2>/dev/null"), "kernelsu");
            addShellHit(hits, "getprop_ksu", Cmd.exe("getprop | grep -i ksu"), "ksu", "kernelsu");
        } else if (ID_APATCH.equals(frameworkId)) {
            addShellHit(hits, "apd_version", Cmd.exe("apd -V 2>/dev/null"), "apd", "apatch");
            addShellHit(hits, "which_apd", Cmd.exe("which apd 2>/dev/null"), "apd");
            addShellHit(hits, "ls_ap_dir", Cmd.exe("ls -la /data/adb/ap 2>&1"), "ap", "apd");
            addShellHit(hits, "ls_apd_dir", Cmd.exe("ls -la /data/adb/apd 2>&1"), "apd", "apatch");
            addShellHit(hits, "getprop_apatch", Cmd.exe("getprop | grep -i apatch"), "apatch", "apd");
        }
        return hits;
    }

    private static JSONArray collectSystemSuShellHits(SharedContext shared) throws JSONException {
        JSONArray hits = new JSONArray();
        addShellHit(hits, "which_su", shared.suWhichPath, "su");
        addShellHit(hits, "ls_system_su", Cmd.exe("ls -la /system/bin/su /system/xbin/su 2>&1"), "su");
        addShellHit(hits, "su_version", Cmd.exe("su -v 2>&1 | head -1"), "su", "superuser");
        addShellHit(hits, "type_su", Cmd.exe("type su 2>&1"), "su");
        addShellHit(hits, "stat_su", Cmd.exe("stat /system/bin/su 2>&1"), "su");
        return hits;
    }

    private static void addShellHit(JSONArray hits, String tag, String output, String... acceptKeywords)
            throws JSONException {
        String normalized = normalize(output);
        if (normalized.isEmpty()) {
            return;
        }
        String lower = normalized.toLowerCase(Locale.US);
        if (tag.equals("ls_data_adb") && normalized.startsWith("total")) {
            hits.put(tag + ": /data/adb accessible");
            return;
        }
        if (tag.equals("which_su") && !normalized.contains("not found")) {
            hits.put(tag + ": " + normalized);
            return;
        }
        for (String keyword : acceptKeywords) {
            if (lower.contains(keyword.toLowerCase(Locale.US))) {
                if (lower.contains("not found") && !lower.contains("su")) {
                    continue;
                }
                hits.put(tag + ": " + truncate(normalized, 160));
                return;
            }
        }
    }

    private static JSONArray scanExistingPaths(String[] paths) throws JSONException {
        JSONArray hits = new JSONArray();
        for (String path : paths) {
            if (new File(path).exists()) {
                hits.put(path);
            }
        }
        return hits;
    }

    private static JSONArray scanProcMounts() throws JSONException {
        JSONArray hits = scanProcFile("/proc/mounts", concatAll(
                MAGISK_MAPS_KEYWORDS, KERNELSU_MAPS_KEYWORDS, APATCH_MAPS_KEYWORDS));
        String content = readFile("/proc/mounts");
        if (content != null) {
            String lower = content.toLowerCase(Locale.US);
            if (lower.contains("overlay") && lower.contains("/system")) {
                hits.put("system_overlay");
            }
        }
        return hits;
    }

    private static JSONArray scanProcFile(String path, String[] keywords) throws JSONException {
        JSONArray hits = new JSONArray();
        String content = readFile(path);
        if (content == null) {
            return hits;
        }
        String lower = content.toLowerCase(Locale.US);
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.US))) {
                hits.put(keyword);
            }
        }
        return hits;
    }

    private static JSONArray collectPropertyHits(String allProps, String[] keys) throws JSONException {
        JSONArray hits = new JSONArray();
        if (allProps != null) {
            String lower = allProps.toLowerCase(Locale.US);
            for (String key : keys) {
                String keyLower = key.toLowerCase(Locale.US);
                if (lower.contains("[" + keyLower + "]") || lower.contains(keyLower)) {
                    for (String line : allProps.split("\n")) {
                        String trimmed = line.trim();
                        if (trimmed.toLowerCase(Locale.US).contains(keyLower)) {
                            hits.put(trimmed);
                        }
                    }
                }
            }
        }
        for (String key : keys) {
            String value = normalize(Cmd.getPropertyViaShell(key));
            if (!value.isEmpty()) {
                hits.put(key + "=" + value);
            }
        }
        return hits;
    }

    private static JSONArray scanEnvironmentVariables() throws JSONException {
        JSONArray hits = new JSONArray();
        String env = normalize(Cmd.exe("printenv"));
        if (env.isEmpty()) {
            return hits;
        }
        for (String line : env.split("\n")) {
            String lower = line.toLowerCase(Locale.US);
            if (lower.contains("magisk") || lower.contains("zygisk")
                    || lower.contains("kernelsu") || lower.contains("ksu")
                    || lower.contains("apatch") || lower.contains("apd")) {
                hits.put(line.trim());
            }
        }
        return hits;
    }

    private static JSONArray scanAllInstalledPackages(Context context) throws JSONException {
        JSONArray hits = new JSONArray();
        if (context == null) {
            return hits;
        }
        try {
            PackageManager pm = context.getPackageManager();
            for (PackageInfo info : pm.getInstalledPackages(0)) {
                if (info != null && info.packageName != null) {
                    hits.put(info.packageName);
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Package scan failed", e);
        }
        return hits;
    }

    private static JSONArray filterPackages(JSONArray installed, String[] keywords) throws JSONException {
        JSONArray hits = new JSONArray();
        for (int i = 0; i < installed.length(); i++) {
            String pkg = installed.optString(i).toLowerCase(Locale.US);
            for (String keyword : keywords) {
                if (pkg.contains(keyword.toLowerCase(Locale.US))) {
                    hits.put(installed.optString(i));
                    break;
                }
            }
        }
        return hits;
    }

    private static JSONArray filterKeywordHits(JSONArray source, String[] keywords) throws JSONException {
        JSONArray hits = new JSONArray();
        for (int i = 0; i < source.length(); i++) {
            String value = source.optString(i).toLowerCase(Locale.US);
            for (String keyword : keywords) {
                if (value.contains(keyword.toLowerCase(Locale.US))) {
                    hits.put(source.optString(i));
                    break;
                }
            }
        }
        return hits;
    }

    private static JSONObject parseNativeProbe() throws JSONException {
        JSONObject probe = new JSONObject();
        try {
            String raw = JniPropertyHelper.getMagiskNativeProbe();
            if (raw != null && raw.trim().startsWith("{")) {
                probe = new JSONObject(raw);
            } else {
                probe.put("raw", raw != null ? raw : "");
            }
        } catch (JSONException e) {
            probe.put("parseError", e.getMessage());
        }
        boolean anyHit = probe.optJSONArray("accessiblePaths") != null
                && probe.optJSONArray("accessiblePaths").length() > 0;
        anyHit = anyHit || (probe.optJSONArray("mapsHits") != null
                && probe.optJSONArray("mapsHits").length() > 0);
        anyHit = anyHit || (probe.optJSONArray("mountHits") != null
                && probe.optJSONArray("mountHits").length() > 0);
        probe.put("anyHit", anyHit);
        return probe;
    }

    private static JSONArray detectJavaNativePathMismatch(JSONObject nativeProbe, JSONArray javaPaths)
            throws JSONException {
        JSONArray mismatches = new JSONArray();
        JSONArray nativePaths = nativeProbe.optJSONArray("accessiblePaths");
        if (nativePaths == null) {
            return mismatches;
        }
        for (int i = 0; i < nativePaths.length(); i++) {
            String path = nativePaths.optString(i);
            if (path.isEmpty()) {
                continue;
            }
            boolean javaVisible = new File(path).exists();
            boolean listedByJavaScan = containsString(javaPaths, path);
            if (!javaVisible || !listedByJavaScan) {
                mismatches.put(path + " (native可访问, Java=" + javaVisible + ")");
            }
        }
        return mismatches;
    }

    private static JSONArray collectBootUnlockSignals() throws JSONException {
        JSONArray hits = new JSONArray();
        addBootSignal(hits, "ro.boot.verifiedbootstate", new String[]{"orange", "yellow"});
        addBootSignal(hits, "ro.boot.flash.locked", new String[]{"0"});
        addBootSignal(hits, "ro.boot.vbmeta.device_state", new String[]{"unlocked"});
        addBootSignal(hits, "ro.boot.warranty_bit", new String[]{"1"});
        addBootSignal(hits, "ro.boot.veritymode", new String[]{"enforcing"});
        for (String key : BOOT_UNLOCK_PROPS) {
            String shell = normalize(Cmd.getPropertyViaShell(key));
            String jni = normalize(JniPropertyHelper.getSystemPropertyByFind(key));
            if (!shell.isEmpty() && !jni.isEmpty() && !shell.equals(jni)) {
                hits.put(key + " 通道不一致: getprop=" + shell + " jni=" + jni);
            }
        }
        return hits;
    }

    private static void addBootSignal(JSONArray hits, String key, String[] suspiciousValues)
            throws JSONException {
        String value = normalize(Cmd.getPropertyViaShell(key));
        if (value.isEmpty()) {
            return;
        }
        String lower = value.toLowerCase(Locale.US);
        for (String suspicious : suspiciousValues) {
            if (key.equals("ro.boot.veritymode")) {
                if (!"enforcing".equalsIgnoreCase(value) && !"eio".equalsIgnoreCase(value)) {
                    hits.put(key + "=" + value);
                }
                return;
            }
            if (lower.equals(suspicious.toLowerCase(Locale.US))) {
                hits.put(key + "=" + value);
                return;
            }
        }
    }

    private static JSONArray collectBuildMismatches() throws JSONException {
        JSONArray hits = new JSONArray();
        compareBuildField(hits, "TAGS", "ro.build.tags");
        compareBuildField(hits, "FINGERPRINT", "ro.build.fingerprint");
        compareBuildField(hits, "TYPE", "ro.build.type");
        compareBuildField(hits, "MODEL", "ro.product.model");
        if (Build.TAGS != null && Build.TAGS.contains("test-keys")) {
            hits.put("Build.TAGS=test-keys");
        }
        return hits;
    }

    private static void compareBuildField(JSONArray hits, String buildField, String propKey)
            throws JSONException {
        String buildValue = readBuildField(buildField);
        String propValue = normalize(Cmd.getPropertyViaShell(propKey));
        if (!buildValue.isEmpty() && !propValue.isEmpty() && !buildValue.equals(propValue)) {
            hits.put(buildField + " vs " + propKey + ": Build=" + buildValue + " prop=" + propValue);
        }
    }

    private static String readBuildField(String field) {
        switch (field) {
            case "TAGS":
                return normalize(Build.TAGS);
            case "FINGERPRINT":
                return normalize(Build.FINGERPRINT);
            case "TYPE":
                return normalize(Build.TYPE);
            case "MODEL":
                return normalize(Build.MODEL);
            default:
                return "";
        }
    }

    private static void appendPathReasons(JSONArray reasons, String label, JSONArray paths)
            throws JSONException {
        for (int i = 0; i < paths.length(); i++) {
            reasons.put(label + " 路径存在: " + paths.optString(i));
        }
    }

    private static void appendArrayReasons(JSONArray reasons, JSONArray items, String prefix)
            throws JSONException {
        for (int i = 0; i < items.length(); i++) {
            reasons.put(prefix + ": " + items.optString(i));
        }
    }

    private static void mergeReasons(JSONArray target, JSONArray source) throws JSONException {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.length(); i++) {
            target.put(source.optString(i));
        }
    }

    private static boolean containsString(JSONArray array, String value) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            if (value.equals(array.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private static JSONArray mergeJsonArrays(JSONArray... arrays) throws JSONException {
        JSONArray merged = new JSONArray();
        for (JSONArray array : arrays) {
            for (int i = 0; i < array.length(); i++) {
                merged.put(array.optString(i));
            }
        }
        return merged;
    }

    private static String[] concatAll(String[]... groups) {
        int total = 0;
        for (String[] group : groups) {
            total += group.length;
        }
        String[] result = new String[total];
        int index = 0;
        for (String[] group : groups) {
            for (String item : group) {
                result[index++] = item;
            }
        }
        return result;
    }

    private static String readFile(String path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
