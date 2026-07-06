package com.android.device;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Hook / Root / 篡改检测。通过多通道读取系统属性、su 路径、Hook 框架特征等判断环境是否被修改。
 */
public final class CheckEmu {

    public static final String TAG = "CheckEmu";

    private static final String[] HOOK_PROBE_KEYS = {
            "ro.product.model",
            "ro.build.fingerprint",
            "ro.hardware",
            "ro.product.manufacturer",
            "ro.secure",
            "ro.debuggable",
            "ro.build.tags",
            "ro.boot.verifiedbootstate"
    };

    private static final String[] SU_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sbin/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/data/adb/magisk",
            "/sbin/.magisk"
    };

    private static final String[] HOOK_FRAMEWORK_FILES = {
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/data/local/tmp/frida-gadget.so"
    };

    private static final String[] HOOK_FRAMEWORK_CLASSES = {
            "de.robv.android.xposed.XposedBridge",
            "org.lsposed.lspd.core.Main"
    };

    private static final String[] PROC_MAPS_HOOK_KEYWORDS = {
            "frida",
            "xposed",
            "lsposed",
            "substrate",
            "yumyhook",
            "lspd"
    };

    private CheckEmu() {
    }

    public static JSONObject getSecurityCheckInfo(Context context) {
        JSONObject legacy = new JSONObject();
        try {
            JSONObject hook = buildHookSection();
            JSONObject root = buildRootSection();
            legacy.put("hookFrameworkDetected", hook.optBoolean("frameworkDetected"));
            legacy.put("isPropertyTampered", hook.optBoolean("propertyTampered"));
            legacy.put("propertyProbes", hook.optJSONObject("propertyProbes"));
            legacy.put("hookFrameworkIndicators", hook.optJSONObject("frameworkIndicators"));
            legacy.put("isRooted", root.optBoolean("isRooted"));
            legacy.put("rootAccessGranted", root.optBoolean("accessGranted"));
            legacy.put("rootAccessDetail", root.optString("accessDetail"));
            legacy.put("rootIndicators", root.optJSONObject("indicators"));
            legacy.put("isAdbEnabled", isAdbEnabled(context));
            legacy.put("reasons", new JSONObject()
                    .put("hook", buildHookReasons(hook))
                    .put("root", buildRootReasons(root))
                    .put("propertyTamper", buildPropertyTamperReasons(hook)));
        } catch (Throwable t) {
            Log.e(TAG, "getSecurityCheckInfo failed", t);
            try {
                legacy.put("error", t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
            } catch (JSONException ignored) {
            }
        }
        return legacy;
    }

    public static JSONObject buildHookSection() {
        JSONObject hook = new JSONObject();
        try {
            JSONObject propertyProbes = buildPropertyProbes();
            JSONObject frameworkIndicators = buildHookFrameworkIndicators();
            hook.put("frameworkDetected", isHookFrameworkDetected(frameworkIndicators));
            hook.put("propertyTampered", hasTamperedProperty(propertyProbes));
            hook.put("frameworkIndicators", frameworkIndicators);
            hook.put("propertyProbes", propertyProbes);
            hook.put("nativeDiagnostics", buildNativeDiagnostics());
        } catch (JSONException e) {
            Log.e(TAG, "buildHookSection failed", e);
        }
        return hook;
    }

    private static JSONObject buildNativeDiagnostics() {
        JSONObject diagnostics = new JSONObject();
        try {
            diagnostics.put("channelImpl", JniPropertyHelper.getNativePropertyDiagnostics());
            diagnostics.put("selfTestJniGetModel", normalizePropertyValue(
                    JniPropertyHelper.getSystemPropertyByGet("ro.product.model")));
            diagnostics.put("selfTestJniFindModel", normalizePropertyValue(
                    JniPropertyHelper.getSystemPropertyByFind("ro.product.model")));
            diagnostics.put("selfTestLibcutilsModel", normalizePropertyValue(
                    JniPropertyHelper.getLibcutilsPropertyGet("ro.product.model")));
            JSONArray keys = new JSONArray();
            for (String key : HOOK_PROBE_KEYS) {
                keys.put(key);
            }
            diagnostics.put("probeKeys", keys);
        } catch (JSONException e) {
            Log.e(TAG, "buildNativeDiagnostics failed", e);
        }
        return diagnostics;
    }

    public static JSONObject buildRootSection() {
        JSONObject root = new JSONObject();
        try {
            JSONObject indicators = buildRootIndicators();
            root.put("isRooted", hasPositiveRootIndicator(indicators));
            root.put("accessGranted", RootAccessHelper.isRootGranted());
            root.put("accessDetail", RootAccessHelper.getAttemptDetail());
            root.put("indicators", indicators);
        } catch (JSONException e) {
            Log.e(TAG, "buildRootSection failed", e);
        }
        return root;
    }

    public static JSONArray buildHookReasons(JSONObject hookSection) {
        JSONArray reasons = new JSONArray();
        JSONObject indicators = hookSection.optJSONObject("frameworkIndicators");
        if (indicators == null) {
            return reasons;
        }
        if (indicators.optBoolean("xposedClassPresent", false)) {
            reasons.put("检测到 Xposed 类: " + HOOK_FRAMEWORK_CLASSES[0]);
        }
        if (indicators.optBoolean("lsposedClassPresent", false)) {
            reasons.put("检测到 LSPosed 类: " + HOOK_FRAMEWORK_CLASSES[1]);
        }
        JSONArray hookFiles = indicators.optJSONArray("hookFrameworkFilesPresent");
        if (hookFiles != null) {
            for (int i = 0; i < hookFiles.length(); i++) {
                reasons.put("Hook 特征文件存在: " + hookFiles.optString(i));
            }
        }
        JSONArray mapsMatches = indicators.optJSONArray("procMapsMatches");
        if (mapsMatches != null) {
            for (int i = 0; i < mapsMatches.length(); i++) {
                reasons.put("/proc/self/maps 命中关键词: " + mapsMatches.optString(i));
            }
        }
        if (!indicators.optBoolean("procMapsScanned", false)) {
            reasons.put("未能读取 /proc/self/maps");
        }
        return reasons;
    }

    public static JSONArray buildPropertyTamperReasons(JSONObject hookSection) {
        JSONArray reasons = new JSONArray();
        JSONObject probes = hookSection.optJSONObject("propertyProbes");
        if (probes == null) {
            return reasons;
        }
        Iterator<String> keys = probes.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject probe = probes.optJSONObject(key);
            if (probe == null) {
                continue;
            }
            String tamperReason = probe.optString("tamperReason", "");
            if (!tamperReason.isEmpty()) {
                reasons.put(tamperReason);
            }
        }
        return reasons;
    }

    public static JSONArray buildRootReasons(JSONObject rootSection) {
        JSONArray reasons = new JSONArray();
        if (rootSection.optBoolean("accessGranted", false)) {
            reasons.put("Root 授权探测成功: " + rootSection.optString("accessDetail", ""));
        }
        JSONObject indicators = rootSection.optJSONObject("indicators");
        if (indicators == null) {
            return reasons;
        }
        JSONArray matchedSuPaths = indicators.optJSONArray("matchedSuPaths");
        if (matchedSuPaths != null) {
            for (int i = 0; i < matchedSuPaths.length(); i++) {
                reasons.put("存在 su 路径: " + matchedSuPaths.optString(i));
            }
        }
        String suWhichPath = indicators.optString("suWhichPath", "");
        if (!suWhichPath.isEmpty()) {
            reasons.put("which su 可用: " + suWhichPath);
        }
        JSONArray matchedMagiskPaths = indicators.optJSONArray("matchedMagiskPaths");
        if (matchedMagiskPaths != null) {
            for (int i = 0; i < matchedMagiskPaths.length(); i++) {
                reasons.put("Magisk 路径存在: " + matchedMagiskPaths.optString(i));
            }
        }
        if (indicators.optBoolean("testKeysBuild", false)) {
            reasons.put("构建标签含 test-keys: " + Build.TAGS);
        }
        if (indicators.optBoolean("roSecureOff", false)) {
            reasons.put("ro.secure=0");
        }
        if (indicators.optBoolean("roDebuggableOn", false)) {
            reasons.put("ro.debuggable=1");
        }
        if (indicators.optBoolean("rootedSystemProperty", false)) {
            reasons.put("vzw.os.rooted 指示已 Root");
        }
        return reasons;
    }

    private static boolean hasPositiveRootIndicator(JSONObject indicators) {
        Iterator<String> keys = indicators.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.endsWith("Paths") || key.endsWith("Path") || "suWhichPath".equals(key)) {
                continue;
            }
            if (indicators.optBoolean(key, false)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRooted() {
        return hasPositiveRootIndicator(buildRootIndicators());
    }

    public static boolean isAdbEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ADB_ENABLED, 0) > 0;
    }

    public static boolean isAnyPropertyTampered() {
        try {
            return hasTamperedProperty(buildPropertyProbes());
        } catch (JSONException e) {
            Log.e(TAG, "isAnyPropertyTampered failed", e);
            return false;
        }
    }

    public static boolean isHookFrameworkDetected() {
        return isHookFrameworkDetected(buildHookFrameworkIndicators());
    }

    private static boolean hasTamperedProperty(JSONObject propertyProbes) {
        Iterator<String> keys = propertyProbes.keys();
        while (keys.hasNext()) {
            JSONObject probe = propertyProbes.optJSONObject(keys.next());
            if (probe != null && probe.optBoolean("tampered", false)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHookFrameworkDetected(JSONObject indicators) {
        if (indicators.optBoolean("xposedClassPresent", false)
                || indicators.optBoolean("lsposedClassPresent", false)) {
            return true;
        }
        JSONArray hookFiles = indicators.optJSONArray("hookFrameworkFilesPresent");
        if (hookFiles != null && hookFiles.length() > 0) {
            return true;
        }
        JSONArray mapsMatches = indicators.optJSONArray("procMapsMatches");
        return mapsMatches != null && mapsMatches.length() > 0;
    }

    private static JSONObject buildHookFrameworkIndicators() {
        JSONObject indicators = new JSONObject();
        try {
            indicators.put("xposedClassPresent", isClassPresent(HOOK_FRAMEWORK_CLASSES[0]));
            indicators.put("lsposedClassPresent", isClassPresent(HOOK_FRAMEWORK_CLASSES[1]));

            JSONArray presentFiles = new JSONArray();
            for (String path : HOOK_FRAMEWORK_FILES) {
                if (new File(path).exists()) {
                    presentFiles.put(path);
                }
            }
            indicators.put("hookFrameworkFilesPresent", presentFiles);

            String maps = readProcSelfMaps();
            indicators.put("procMapsScanned", maps != null);
            indicators.put("procMapsMatches", collectProcMapsKeywordMatches(maps));
        } catch (JSONException e) {
            Log.e(TAG, "buildHookFrameworkIndicators failed", e);
        }
        return indicators;
    }

    private static JSONArray collectProcMapsKeywordMatches(String maps) throws JSONException {
        JSONArray matches = new JSONArray();
        if (maps == null) {
            return matches;
        }
        String lower = maps.toLowerCase(Locale.US);
        for (String keyword : PROC_MAPS_HOOK_KEYWORDS) {
            if (lower.contains(keyword)) {
                matches.put(keyword);
            }
        }
        return matches;
    }

    public static JSONObject probeProperty(String key) {
        JSONObject probe = new JSONObject();
        try {
            String getprop = readViaGetprop(key);
            String systemProperty = normalizePropertyValue(Cmd.getPropertyViaJavaApi(key));
            String jniFind = normalizePropertyValue(JniPropertyHelper.getSystemPropertyByFind(key));
            String libcutils = normalizePropertyValue(JniPropertyHelper.getLibcutilsPropertyGet(key));

            boolean tampered = isPropertyTampered(getprop, systemProperty, jniFind, libcutils);
            probe.put("getprop", getprop);
            probe.put("SystemProperties", systemProperty);
            probe.put("jniFind", jniFind);
            probe.put("libcutils", libcutils);
            probe.put("tampered", tampered);
            if (tampered) {
                probe.put("tamperReason", buildTamperReason(key, getprop, systemProperty, jniFind, libcutils));
            }
        } catch (JSONException e) {
            Log.e(TAG, "probeProperty failed for " + key, e);
        }
        return probe;
    }

    private static JSONObject buildPropertyProbes() throws JSONException {
        JSONObject probes = new JSONObject();
        for (String key : HOOK_PROBE_KEYS) {
            probes.put(key, probeProperty(key));
        }
        return probes;
    }

    private static JSONObject buildRootIndicators() {
        JSONObject indicators = new JSONObject();
        try {
            JSONArray matchedSuPaths = listExistingPaths(SU_PATHS);
            JSONArray matchedMagiskPaths = listExistingPaths(
                    "/data/adb/magisk",
                    "/sbin/.magisk"
            );
            String suWhichPath = normalizePropertyValue(Cmd.exe("which su"));

            indicators.put("suBinaryExists", matchedSuPaths.length() > 0);
            indicators.put("matchedSuPaths", matchedSuPaths);
            indicators.put("suCommandAvailable", !suWhichPath.isEmpty() && !suWhichPath.contains("not found"));
            indicators.put("suWhichPath", suWhichPath);
            indicators.put("suShellGranted", RootAccessHelper.isRootGranted());
            indicators.put("testKeysBuild", isTestKeysBuild());
            indicators.put("roSecureOff", isSecurePropertyOff());
            indicators.put("roDebuggableOn", isDebuggablePropertyOn());
            indicators.put("rootedSystemProperty", isRootedSystemProperty());
            indicators.put("magiskPathExists", matchedMagiskPaths.length() > 0);
            indicators.put("matchedMagiskPaths", matchedMagiskPaths);
        } catch (JSONException e) {
            Log.e(TAG, "buildRootIndicators failed", e);
        }
        return indicators;
    }

    private static JSONArray listExistingPaths(String... paths) throws JSONException {
        JSONArray existing = new JSONArray();
        for (String path : paths) {
            if (new File(path).exists()) {
                existing.put(path);
            }
        }
        return existing;
    }

    private static String buildTamperReason(
            String key,
            String getprop,
            String systemProperty,
            String jniFind,
            String libcutils
    ) {
        return key + " 多通道不一致: getprop=" + getprop
                + ", SystemProperties=" + systemProperty
                + ", jniFind=" + jniFind
                + ", libcutils=" + libcutils;
    }

    private static boolean isTestKeysBuild() {
        return Build.TAGS != null && Build.TAGS.contains("test-keys");
    }

    private static boolean isSecurePropertyOff() {
        return "0".equals(normalizePropertyValue(Cmd.getProperty("ro.secure")));
    }

    private static boolean isDebuggablePropertyOn() {
        return "1".equals(normalizePropertyValue(Cmd.getProperty("ro.debuggable")));
    }

    private static boolean isRootedSystemProperty() {
        String rooted = normalizePropertyValue(Cmd.getProperty("vzw.os.rooted"));
        return "true".equalsIgnoreCase(rooted) || "1".equals(rooted);
    }

    private static String readViaGetprop(String key) {
        return normalizePropertyValue(Cmd.exe("getprop " + key));
    }

    private static boolean isPropertyTampered(String... values) {
        Set<String> distinct = new LinkedHashSet<>();
        for (String value : values) {
            distinct.add(normalizePropertyValue(value));
        }
        return distinct.size() > 1;
    }

    private static String normalizePropertyValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static String readProcSelfMaps() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/maps"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
