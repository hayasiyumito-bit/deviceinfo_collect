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
            "/system/usr/we-need-root/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/system/xbin/daemonsu",
            "/data/adb/magisk",
            "/data/adb/ksu",
            "/data/adb/ap",
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
            "lspd",
            "magisk",
            "zygisk",
            "magiskpolicy",
            "kernelsu",
            "ksu",
            "apatch",
            "apd"
    };

    private CheckEmu() {
    }

    public static JSONObject getSecurityCheckInfo(Context context) {
        JSONObject legacy = new JSONObject();
        try {
            JSONObject hook = buildHookSection();
            JSONObject root = buildRootSection(context);
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

    public static JSONObject buildRootSection(Context context) {
        JSONObject root = new JSONObject();
        try {
            JSONObject indicators = buildRootIndicators();
            JSONObject rootProbe = RootFrameworkDetector.probe(context);
            boolean magiskDetected = rootProbe.optBoolean("magiskDetected", false);
            boolean kernelsuDetected = rootProbe.optBoolean("kernelsuDetected", false);
            boolean apatchDetected = rootProbe.optBoolean("apatchDetected", false);
            boolean systemSuDetected = rootProbe.optBoolean("systemSuDetected", false);
            boolean frameworkDetected = rootProbe.optBoolean("detected", false);
            boolean rooted = hasPositiveRootIndicator(indicators) || frameworkDetected;

            root.put("isRooted", rooted);
            root.put("magiskDetected", magiskDetected);
            root.put("kernelsuDetected", kernelsuDetected);
            root.put("apatchDetected", apatchDetected);
            root.put("systemSuDetected", systemSuDetected);
            root.put("magiskHideSuspected", rootProbe.optBoolean("hideSuspected", false));
            root.put("accessGranted", RootAccessHelper.isRootGranted());
            root.put("accessDetail", RootAccessHelper.getAttemptDetail());
            root.put("indicators", indicators);
            root.put("frameworks", rootProbe.optJSONObject("frameworks"));
            root.put("rootProbe", rootProbe);
            root.put("magisk", rootProbe);
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
        if (indicators != null) {
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
        }
        JSONObject rootProbe = rootSection.optJSONObject("rootProbe");
        if (rootProbe == null) {
            rootProbe = rootSection.optJSONObject("magisk");
        }
        if (rootProbe != null) {
            JSONArray probeReasons = rootProbe.optJSONArray("reasons");
            if (probeReasons != null) {
                for (int i = 0; i < probeReasons.length(); i++) {
                    reasons.put(probeReasons.optString(i));
                }
            }
            if (rootSection.optBoolean("magiskHideSuspected", false)) {
                reasons.put("疑似 Root 隐藏：检测信号存在但 su/路径被隐藏");
            }
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
            String jniGet = normalizePropertyValue(JniPropertyHelper.getSystemPropertyByGet(key));
            String jniFind = normalizePropertyValue(JniPropertyHelper.getSystemPropertyByFind(key));
            String libcutils = normalizePropertyValue(JniPropertyHelper.getLibcutilsPropertyGet(key));

            boolean tampered = isPropertyTampered(getprop, systemProperty, jniGet, jniFind, libcutils);
            probe.put("getprop", getprop);
            probe.put("SystemProperties", systemProperty);
            probe.put("jniGet", jniGet);
            probe.put("jniFind", jniFind);
            probe.put("libcutils", libcutils);
            probe.put("tampered", tampered);
            if (tampered) {
                probe.put("tamperReason", buildTamperReason(
                        key, getprop, systemProperty, jniGet, jniFind, libcutils));
            }
            JSONArray channelErrors = collectChannelErrors(jniGet, jniFind, libcutils);
            if (channelErrors.length() > 0) {
                probe.put("channelErrors", channelErrors);
            }
        } catch (JSONException e) {
            Log.e(TAG, "probeProperty failed for " + key, e);
            try {
                probe.put("error", "Error: " + e.getMessage());
            } catch (JSONException ignored) {
            }
        }
        return probe;
    }

    private static JSONArray collectChannelErrors(String... channelValues) throws JSONException {
        JSONArray errors = new JSONArray();
        for (String value : channelValues) {
            if (JniPropertyHelper.isErrorResult(value)) {
                errors.put(value);
            }
        }
        return errors;
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
            String jniGet,
            String jniFind,
            String libcutils
    ) {
        return key + " 多通道不一致: getprop=" + getprop
                + ", SystemProperties=" + systemProperty
                + ", jniGet=" + jniGet
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
            String normalized = normalizePropertyValue(value);
            if (normalized.isEmpty() || JniPropertyHelper.isErrorResult(normalized)) {
                continue;
            }
            distinct.add(normalized);
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
