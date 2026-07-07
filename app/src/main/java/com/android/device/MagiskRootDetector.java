package com.android.device;

import android.content.Context;
import android.content.pm.ApplicationInfo;
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
 * Magisk / Root 深度检测：针对隐藏 su、DenyList、属性伪造等绕过手段。
 */
public final class MagiskRootDetector {

    private static final String TAG = "MagiskRootDetector";

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

    private static final String[] MAGISK_PACKAGE_KEYWORDS = {
            "com.topjohnwu.magisk",
            "io.github.huskydg.magisk",
            "magisk"
    };

    private static final String[] MAGISK_MAPS_KEYWORDS = {
            "magisk",
            "zygisk",
            "magiskpolicy",
            "magisk32",
            "magisk64",
            "kernelsu",
            "ksu",
            "apd"
    };

    private static final String[] BOOT_UNLOCK_PROPS = {
            "ro.boot.verifiedbootstate",
            "ro.boot.flash.locked",
            "ro.boot.vbmeta.device_state",
            "ro.boot.warranty_bit",
            "ro.boot.veritymode"
    };

    private MagiskRootDetector() {
    }

    public static JSONObject probe(Context context) {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();
        try {
            JSONArray matchedPaths = scanExistingPaths();
            JSONArray mapsHits = scanProcMaps();
            JSONArray mountHits = scanProcMounts();
            JSONArray mountInfoHits = scanProcFile("/proc/self/mountinfo", MAGISK_MAPS_KEYWORDS);
            JSONArray shellHits = runShellProbes();
            JSONArray envHits = scanEnvironmentVariables();
            JSONObject nativeProbe = parseNativeProbe();
            JSONArray magiskProps = collectMagiskProperties();
            JSONArray bootUnlockSignals = collectBootUnlockSignals();
            JSONArray buildMismatches = collectBuildMismatches();
            JSONArray suspiciousPackages = scanInstalledPackages(context);
            JSONArray javaNativeMismatches = detectJavaNativePathMismatch(nativeProbe, matchedPaths);
            String selinuxMode = normalize(Cmd.exe("getenforce"));
            String suReadlink = normalize(Cmd.exe("readlink /system/bin/su 2>/dev/null"));
            String idOutput = normalize(Cmd.exe("id"));

            boolean pathHit = matchedPaths.length() > 0;
            boolean mapsHit = mapsHits.length() > 0;
            boolean mountHit = mountHits.length() > 0 || mountInfoHits.length() > 0;
            boolean shellHit = shellHits.length() > 0;
            boolean nativeHit = nativeProbe.optBoolean("anyHit", false);
            boolean propHit = magiskProps.length() > 0;
            boolean bootUnlockHit = bootUnlockSignals.length() > 0;
            boolean buildMismatchHit = buildMismatches.length() > 0;
            boolean packageHit = suspiciousPackages.length() > 0;
            boolean javaNativeMismatchHit = javaNativeMismatches.length() > 0;
            boolean envHit = envHits.length() > 0;
            boolean selinuxPermissive = selinuxMode.toLowerCase(Locale.US).contains("permissive");
            boolean suLinkedToMagisk = suReadlink.toLowerCase(Locale.US).contains("magisk");
            boolean idShowsRoot = idOutput.contains("uid=0") || idOutput.contains("(root)");
            boolean hideSuspected = (mapsHit || mountHit || nativeHit || propHit || bootUnlockHit
                    || javaNativeMismatchHit || envHit)
                    && !pathHit && !RootAccessHelper.isRootGranted();

            indicators.put("matchedPaths", matchedPaths);
            indicators.put("mapsHits", mapsHits);
            indicators.put("mountHits", mountHits);
            indicators.put("mountInfoHits", mountInfoHits);
            indicators.put("shellHits", shellHits);
            indicators.put("envHits", envHits);
            indicators.put("javaNativeMismatches", javaNativeMismatches);
            indicators.put("nativeProbe", nativeProbe);
            indicators.put("magiskProperties", magiskProps);
            indicators.put("bootUnlockSignals", bootUnlockSignals);
            indicators.put("buildMismatches", buildMismatches);
            indicators.put("suspiciousPackages", suspiciousPackages);
            indicators.put("selinuxMode", selinuxMode);
            indicators.put("suReadlink", suReadlink);
            indicators.put("idOutput", idOutput);
            indicators.put("pathHit", pathHit);
            indicators.put("mapsHit", mapsHit);
            indicators.put("mountHit", mountHit);
            indicators.put("shellHit", shellHit);
            indicators.put("nativeHit", nativeHit);
            indicators.put("propHit", propHit);
            indicators.put("bootUnlockHit", bootUnlockHit);
            indicators.put("buildMismatchHit", buildMismatchHit);
            indicators.put("packageHit", packageHit);
            indicators.put("javaNativeMismatchHit", javaNativeMismatchHit);
            indicators.put("envHit", envHit);
            indicators.put("selinuxPermissive", selinuxPermissive);
            indicators.put("suLinkedToMagisk", suLinkedToMagisk);
            indicators.put("idShowsRoot", idShowsRoot);
            indicators.put("hideSuspected", hideSuspected);

            appendPathReasons(reasons, matchedPaths);
            appendArrayReasons(reasons, mapsHits, "/proc/self/maps 命中");
            appendArrayReasons(reasons, mountHits, "/proc/mounts 命中");
            appendArrayReasons(reasons, mountInfoHits, "mountinfo 命中");
            appendArrayReasons(reasons, shellHits, "Shell 探测");
            appendArrayReasons(reasons, envHits, "环境变量");
            appendArrayReasons(reasons, javaNativeMismatches, "Java/Native 路径不一致(疑似 Hide)");
            appendNativeReasons(reasons, nativeProbe);
            appendArrayReasons(reasons, magiskProps, "Magisk 属性");
            appendArrayReasons(reasons, bootUnlockSignals, "Boot 解锁信号");
            appendArrayReasons(reasons, buildMismatches, "Build 与属性不一致");
            appendArrayReasons(reasons, suspiciousPackages, "可疑安装包");
            if (selinuxPermissive) {
                reasons.put("SELinux 处于 Permissive: " + selinuxMode);
            }
            if (suLinkedToMagisk) {
                reasons.put("/system/bin/su 链接到 Magisk: " + suReadlink);
            }
            if (idShowsRoot) {
                reasons.put("id 显示 root: " + idOutput);
            }
            if (hideSuspected) {
                reasons.put("疑似 Magisk Hide/DenyList：挂载/maps/属性异常但常规路径不可见");
            }

            boolean detected = pathHit || mapsHit || mountHit || shellHit || nativeHit
                    || propHit || bootUnlockHit || buildMismatchHit || packageHit
                    || selinuxPermissive || suLinkedToMagisk || idShowsRoot
                    || javaNativeMismatchHit || envHit;

            result.put("detected", detected);
            result.put("hideSuspected", hideSuspected);
            result.put("indicators", indicators);
            result.put("reasons", reasons);
        } catch (JSONException e) {
            Log.e(TAG, "Magisk probe failed", e);
            try {
                result.put("error", e.getMessage());
            } catch (JSONException ignored) {
            }
        }
        return result;
    }

    private static JSONArray scanExistingPaths() throws JSONException {
        JSONArray hits = new JSONArray();
        for (String path : MAGISK_PATHS) {
            if (new File(path).exists()) {
                hits.put(path);
            }
        }
        return hits;
    }

    private static JSONArray scanProcMaps() throws JSONException {
        return scanProcFile("/proc/self/maps", MAGISK_MAPS_KEYWORDS);
    }

    private static JSONArray scanProcMounts() throws JSONException {
        JSONArray hits = scanProcFile("/proc/mounts", MAGISK_MAPS_KEYWORDS);
        JSONArray overlayHits = scanProcFile("/proc/mounts", new String[]{"overlay", "/system"});
        for (int i = 0; i < overlayHits.length(); i++) {
            String line = overlayHits.optString(i);
            if (line.toLowerCase(Locale.US).contains("overlay")
                    && line.contains("/system")) {
                hits.put("system_overlay:" + truncate(line, 120));
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

    private static boolean containsString(JSONArray array, String value) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            if (value.equals(array.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private static JSONArray scanEnvironmentVariables() throws JSONException {
        JSONArray hits = new JSONArray();
        String env = normalize(Cmd.exe("printenv"));
        if (env.isEmpty()) {
            return hits;
        }
        for (String line : env.split("\n")) {
            String lower = line.toLowerCase(Locale.US);
            if (lower.contains("magisk") || lower.contains("zygisk") || lower.contains("kernelsu")) {
                hits.put(line.trim());
            }
        }
        return hits;
    }

    private static JSONArray runShellProbes() throws JSONException {
        JSONArray hits = new JSONArray();
        addShellHit(hits, "getprop_magisk", Cmd.exe("getprop | grep -i magisk"));
        addShellHit(hits, "ls_data_adb", Cmd.exe("ls -la /data/adb 2>&1"));
        addShellHit(hits, "test_sbin_magisk", Cmd.exe("test -f /sbin/magisk && echo exists"));
        addShellHit(hits, "which_magisk", Cmd.exe("which magisk 2>/dev/null"));
        addShellHit(hits, "magisk_version", Cmd.exe("magisk -v 2>/dev/null"));
        addShellHit(hits, "mounts_magisk", Cmd.exe("cat /proc/mounts | grep -i magisk"));
        addShellHit(hits, "mountinfo_magisk", Cmd.exe("cat /proc/self/mountinfo | grep -i magisk"));
        addShellHit(hits, "resetprop_check", Cmd.exe("resetprop 2>&1 | head -1"));
        return hits;
    }

    private static void addShellHit(JSONArray hits, String tag, String output) throws JSONException {
        String normalized = normalize(output);
        if (normalized.isEmpty()) {
            return;
        }
        String lower = normalized.toLowerCase(Locale.US);
        if (lower.contains("not found") || lower.contains("no such file")
                || lower.contains("permission denied") && !lower.contains("magisk")) {
            if (!lower.contains("magisk") && !lower.contains("zygisk") && !lower.contains("modules")) {
                return;
            }
        }
        if (tag.equals("ls_data_adb") && normalized.startsWith("total")) {
            hits.put(tag + ": /data/adb accessible");
            return;
        }
        if (normalized.contains("magisk") || normalized.contains("zygisk")
                || normalized.contains("modules") || tag.equals("test_sbin_magisk")
                || tag.equals("which_magisk") || tag.equals("magisk_version")
                || tag.equals("resetprop_check")) {
            hits.put(tag + ": " + truncate(normalized, 160));
        }
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

    private static JSONArray collectMagiskProperties() throws JSONException {
        JSONArray hits = new JSONArray();
        String allProps = Cmd.exe("getprop");
        if (allProps != null) {
            String lower = allProps.toLowerCase(Locale.US);
            if (lower.contains("magisk") || lower.contains("zygisk")) {
                for (String line : allProps.split("\n")) {
                    String trimmed = line.trim();
                    if (trimmed.toLowerCase(Locale.US).contains("magisk")
                            || trimmed.toLowerCase(Locale.US).contains("zygisk")) {
                        hits.put(trimmed);
                    }
                }
            }
        }
        for (String key : new String[]{
                "init.svc.magisk",
                "init.svc.magisk_daemon",
                "init.svc.magisk_service",
                "ro.magisk.version",
                "persist.magisk.version"
        }) {
            String value = normalize(Cmd.getPropertyViaShell(key));
            if (!value.isEmpty()) {
                hits.put(key + "=" + value);
            }
        }
        return hits;
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
        if (!buildValue.isEmpty() && !propValue.isEmpty()
                && !buildValue.equals(propValue)) {
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

    private static JSONArray scanInstalledPackages(Context context) throws JSONException {
        JSONArray hits = new JSONArray();
        if (context == null) {
            return hits;
        }
        try {
            PackageManager pm = context.getPackageManager();
            for (PackageInfo info : pm.getInstalledPackages(0)) {
                if (info == null || info.packageName == null) {
                    continue;
                }
                String pkg = info.packageName.toLowerCase(Locale.US);
                for (String keyword : MAGISK_PACKAGE_KEYWORDS) {
                    if (pkg.contains(keyword)) {
                        hits.put(info.packageName);
                        break;
                    }
                }
                ApplicationInfo appInfo = info.applicationInfo;
                if (appInfo != null && appInfo.nativeLibraryDir != null
                        && appInfo.nativeLibraryDir.toLowerCase(Locale.US).contains("magisk")) {
                    hits.put(info.packageName + " (nativeLib)");
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Package scan failed", e);
        }
        return hits;
    }

    private static void appendPathReasons(JSONArray reasons, JSONArray paths) throws JSONException {
        for (int i = 0; i < paths.length(); i++) {
            reasons.put("Magisk/Root 路径存在: " + paths.optString(i));
        }
    }

    private static void appendArrayReasons(JSONArray reasons, JSONArray items, String prefix)
            throws JSONException {
        for (int i = 0; i < items.length(); i++) {
            reasons.put(prefix + ": " + items.optString(i));
        }
    }

    private static void appendNativeReasons(JSONArray reasons, JSONObject nativeProbe)
            throws JSONException {
        appendArrayReasons(reasons, nativeProbe.optJSONArray("accessiblePaths"), "Native 路径可访问");
        appendArrayReasons(reasons, nativeProbe.optJSONArray("mapsHits"), "Native maps 命中");
        appendArrayReasons(reasons, nativeProbe.optJSONArray("mountHits"), "Native mount 命中");
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
