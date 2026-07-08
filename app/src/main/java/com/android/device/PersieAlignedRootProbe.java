package com.android.device;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.android.utils.Cmd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 对齐 at.persie0.root_detection_app（Root Detector）检测向量，供 deviceinfo_collect 回归 YumyHook。
 * 参考：frida/at.persie0.root_detection_app/jadx-src/sources/p317j8/C18480d.java
 */
public final class PersieAlignedRootProbe {

    private static final String TAG = "PersieAlignedRootProbe";

    /** 源自 persie C18480d.f53240b — SuspiciousMounts 正则（简化可维护子集）。 */
    private static final Pattern SUSPICIOUS_MOUNT = Pattern.compile(
            "(\\.magisk(?:/|$)|/(?:sbin|debug_ramdisk)/\\.magisk(?:/|$)"
                    + "|(?:^|[ \\t/._-])(?:magisk|magisksu|kernel[-_]?su|apatch|kernelpatch)(?:[ \\t/._:-]|$)"
                    + "|/(?:data/)?adb/modules/[^ \\t]+/(?:system|system_ext|vendor|product|odm)(?:/|[ \\t]|$)"
                    + "|/data/adb/(?:magisk|ksu|ap|kpmodules)(?:/|$)"
                    + "|\\bmodules(?:_update)?\\.img\\b"
                    + "|(?:lowerdir|upperdir|workdir)=[^ \\t]*(?:/data/adb|/adb/modules|\\.magisk|/ksu/|/ap/)[^ \\t]*)",
            Pattern.CASE_INSENSITIVE);

    private static final String[] UNIX_SOCKET_MARKERS = {
            "@magisk", "@zygisk", "@ksud", "@apatch", "@riru"
    };

    private static final String[] ZYGISK_MODULE_PATHS = {
            "/data/adb/modules/zygisk",
            "/data/adb/modules/zygisk_lsposed"
    };

    private static final String[] EXTRA_MAGISK_MOUNT_PATHS = {
            "/sbin/.magisk",
            "/dev/.magisk_unblock",
            "/apex/com.android.art/.magisk",
            "/debug_ramdisk/.magisk"
    };

    public static final String[] ROOT_CLOAKING_PACKAGES = {
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.amphoras.hidemyrootadfree",
            "com.formyhm.hiderootPremium",
            "com.formyhm.hideroot",
            "com.mattmags.roothide",
            "com.draco.ladb",
            "io.github.libxposed.service",
            "io.github.suika.hidemyapplist",
            "cn.geektang.privacyspace",
            "com.tsng.hidemyapplist",
            "io.github.vvb2060.magisk",
            "com.topjohnwu.magisk.debug",
            "com.topjohnwu.magisk.alpha",
            "com.rifsxd.ksunext",
            "com.rifsxd.ksunext.ui",
            "org.apatch.manager",
            "me.weishu.kernelsu.ui"
    };

    private PersieAlignedRootProbe() {
    }

    public static JSONObject probe(Context context) {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();
        try {
            JSONArray dfHits = scanDfMagisk();
            JSONArray psHits = scanPsRootProcesses();
            JSONArray unixHits = scanUnixSockets();
            JSONArray mountRegexHits = scanSuspiciousMountRegex();
            JSONArray zygiskPaths = scanExistingPaths(ZYGISK_MODULE_PATHS);
            JSONArray extraMountPaths = scanExistingPaths(EXTRA_MAGISK_MOUNT_PATHS);
            JSONArray cloakingPackages = scanInstalledPackages(context, ROOT_CLOAKING_PACKAGES);

            putArray(indicators, "dfHits", dfHits);
            putArray(indicators, "psHits", psHits);
            putArray(indicators, "unixSocketHits", unixHits);
            putArray(indicators, "suspiciousMountHits", mountRegexHits);
            putArray(indicators, "zygiskModulePaths", zygiskPaths);
            putArray(indicators, "extraMagiskMountPaths", extraMountPaths);
            putArray(indicators, "rootCloakingPackages", cloakingPackages);

            appendAll(reasons, dfHits, "df 命中: ");
            appendAll(reasons, psHits, "ps 命中: ");
            appendAll(reasons, unixHits, "unix socket: ");
            appendAll(reasons, mountRegexHits, "挂载正则: ");
            appendAll(reasons, zygiskPaths, "Zygisk 模块路径: ");
            appendAll(reasons, extraMountPaths, "Magisk 挂载路径: ");
            appendAll(reasons, cloakingPackages, "Root 隐藏类 App: ");

            boolean detected = reasons.length() > 0;
            result.put("detected", detected);
            result.put("indicators", indicators);
            result.put("reasons", reasons);
            result.put("referenceApp", "at.persie0.root_detection_app");
        } catch (JSONException e) {
            Log.e(TAG, "probe failed", e);
            try {
                result.put("error", e.getMessage());
            } catch (JSONException ignored) {
            }
        }
        return result;
    }

    /** 供 hideSuspected 聚合：df/ps/unix/挂载正则等 persie 向量。 */
    public static boolean hasHideRelevantSignal(JSONObject persie) {
        if (persie == null || !persie.optBoolean("detected", false)) {
            return false;
        }
        JSONObject indicators = persie.optJSONObject("indicators");
        if (indicators == null) {
            return true;
        }
        return arrayNonEmpty(indicators, "dfHits")
                || arrayNonEmpty(indicators, "psHits")
                || arrayNonEmpty(indicators, "unixSocketHits")
                || arrayNonEmpty(indicators, "suspiciousMountHits");
    }

    private static JSONArray scanDfMagisk() throws JSONException {
        JSONArray hits = new JSONArray();
        String df = normalize(Cmd.exe("df"));
        if (df.isEmpty()) {
            return hits;
        }
        for (String line : df.split("\n")) {
            String lower = line.toLowerCase(Locale.US);
            if (lower.contains("magisk") || lower.contains("zygisk")
                    || lower.contains("/data/adb/ksu") || lower.contains("kernelsu")) {
                hits.put(truncate(line.trim(), 200));
            }
        }
        return hits;
    }

    private static JSONArray scanPsRootProcesses() throws JSONException {
        JSONArray hits = new JSONArray();
        String ps = normalize(Cmd.exe("ps -A 2>/dev/null"));
        if (ps.isEmpty()) {
            ps = normalize(Cmd.exe("ps 2>/dev/null"));
        }
        if (ps.isEmpty()) {
            return hits;
        }
        String lower = ps.toLowerCase(Locale.US);
        if (lower.contains("magiskd") || lower.contains("zygisk") || lower.contains("ksud")) {
            hits.put("进程列表含 magiskd/zygisk/ksud");
        }
        return hits;
    }

    private static JSONArray scanUnixSockets() throws JSONException {
        JSONArray hits = new JSONArray();
        String content = readFile("/proc/net/unix");
        if (content == null) {
            return hits;
        }
        String lower = content.toLowerCase(Locale.US);
        for (String marker : UNIX_SOCKET_MARKERS) {
            if (lower.contains(marker.toLowerCase(Locale.US))) {
                hits.put("/proc/net/unix 含 " + marker);
            }
        }
        return hits;
    }

    private static JSONArray scanSuspiciousMountRegex() throws JSONException {
        JSONArray hits = new JSONArray();
        for (String path : new String[]{"/proc/self/mountinfo", "/proc/mounts"}) {
            String content = readFile(path);
            if (content == null) {
                continue;
            }
            for (String line : content.split("\n")) {
                if (SUSPICIOUS_MOUNT.matcher(line).find()) {
                    hits.put("regex@" + path + ": " + truncate(line.trim(), 120));
                }
            }
        }
        return hits;
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

    private static JSONArray scanInstalledPackages(Context context, String[] packages) throws JSONException {
        JSONArray hits = new JSONArray();
        PackageManager pm = context.getPackageManager();
        for (String pkg : packages) {
            try {
                pm.getPackageInfo(pkg, 0);
                hits.put(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return hits;
    }

    private static void appendAll(JSONArray reasons, JSONArray hits, String prefix) throws JSONException {
        for (int i = 0; i < hits.length(); i++) {
            reasons.put(prefix + hits.optString(i));
        }
    }

    private static boolean arrayNonEmpty(JSONObject obj, String key) {
        JSONArray arr = obj.optJSONArray(key);
        return arr != null && arr.length() > 0;
    }

    private static void putArray(JSONObject parent, String key, JSONArray value) throws JSONException {
        parent.put(key, value != null ? value : new JSONArray());
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
