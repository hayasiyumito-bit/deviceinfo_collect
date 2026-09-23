package com.android.device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 根据安全检测结果生成面向 YumyHook / 风控对接方的修复提示，写入 debug_output.json。
 */
public final class SecurityRemediationBuilder {

    private SecurityRemediationBuilder() {
    }

    public static JSONArray buildFixHints(JSONObject remediation) {
        JSONArray hints = new JSONArray();
        if (remediation == null) {
            return hints;
        }
        JSONArray items = remediation.optJSONArray("items");
        if (items == null) {
            return hints;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String detection = item.optString("detection", "");
            String fixAction = item.optString("fixAction", "");
            String fixTarget = item.optString("fixTarget", "");
            if (!detection.isEmpty() && !fixAction.isEmpty()) {
                hints.put(detection + com.android.device.i18n.AppLocale.tr(" | 修复: ", " | Fix: ") + fixAction + " @ " + fixTarget);
            }
        }
        return hints;
    }

    public static JSONObject build(JSONObject summary, JSONObject hookSection, JSONObject rootSection) {
        JSONObject remediation = new JSONObject();
        try {
            JSONArray items = new JSONArray();
            if (summary == null) {
                summary = new JSONObject();
            }
            boolean anyRisk = summary.optBoolean("anyRisk", false);
            remediation.put("anyRisk", anyRisk);
            remediation.put("verdict", anyRisk ? "RISK_DETECTED" : "PASS");

            if (!anyRisk) {
                remediation.put("summary", com.android.device.i18n.AppLocale.tr("未检测到安全风险，无需修复。", "No security risk detected; no fix needed."));
                remediation.put("items", items);
                remediation.put("regressionChecklist", buildPassChecklist());
                return remediation;
            }

            appendHookRemediation(items, summary, hookSection);
            appendRootRemediation(items, summary, rootSection);
            appendEnvironmentRemediation(items, summary);

            remediation.put("summary", buildSummaryText(summary, items));
            remediation.put("items", items);
            remediation.put("regressionChecklist", buildRiskChecklist(summary));
            remediation.put("yumyhookReferences", buildYumyhookReferences());
        } catch (JSONException e) {
            try {
                remediation.put("error", e.getMessage());
            } catch (JSONException ignored) {
            }
        }
        return remediation;
    }

    private static void appendHookRemediation(JSONArray items, JSONObject summary, JSONObject hookSection)
            throws JSONException {
        if (summary.optBoolean("hookFrameworkDetected", false)) {
            JSONObject indicators = hookSection != null
                    ? hookSection.optJSONObject("frameworkIndicators") : null;
            if (indicators != null && indicators.optBoolean("xposedClassPresent", false)) {
                addItem(items, "hook_xposed_class", "high", "hook_java",
                        com.android.device.i18n.AppLocale.tr("检测到 Xposed 类加载", "Xposed class loading detected"),
                        "YumyHook/xposed/stealth",
                        com.android.device.i18n.AppLocale.tr("隐藏 Class.forName / 类加载对 de.robv.android.xposed.XposedBridge 的可见性", "Hide visibility of de.robv.android.xposed.XposedBridge to Class.forName / classloader"),
                        "frameworkIndicators.xposedClassPresent=false");
            }
            if (indicators != null && indicators.optBoolean("lsposedClassPresent", false)) {
                addItem(items, "hook_lsposed_class", "high", "hook_java",
                        com.android.device.i18n.AppLocale.tr("检测到 LSPosed 类加载", "LSPosed class loading detected"),
                        "YumyHook/xposed/stealth",
                        com.android.device.i18n.AppLocale.tr("隐藏 org.lsposed.lspd 相关类", "Hide org.lsposed.lspd related classes"),
                        "frameworkIndicators.lsposedClassPresent=false");
            }
            JSONArray maps = indicators != null ? indicators.optJSONArray("procMapsMatches") : null;
            if (maps != null && maps.length() > 0) {
                addItem(items, "hook_proc_maps", "high", "hook_proc",
                        com.android.device.i18n.AppLocale.tr("Java 层 /proc/self/maps 命中 Hook 关键词: ", "Java-layer /proc/self/maps hit hook keyword: ") + joinJsonArray(maps),
                        com.android.device.i18n.AppLocale.tr("YumyHook/ProcMapsStealthHook.kt + native_bridge.cpp fgets 过滤", "YumyHook/ProcMapsStealthHook.kt + native_bridge.cpp fgets filtering"),
                        com.android.device.i18n.AppLocale.tr("Hook BufferedReader 读 maps，并同步 Native fopen/fgets 行过滤 frida/xposed/lsposed", "Hook BufferedReader reading maps, and mirror Native fopen/fgets line filtering of frida/xposed/lsposed"),
                        "frameworkIndicators.procMapsMatches=[]");
            }
        }

        if (summary.optBoolean("propertyTampered", false)) {
            JSONObject probes = hookSection != null
                    ? hookSection.optJSONObject("propertyProbes") : null;
            JSONArray tamperedKeys = collectTamperedKeys(probes);
            addItem(items, "hook_property_tamper", "high", "hook_property",
                    com.android.device.i18n.AppLocale.tr("系统属性多通道不一致", "System property multi-channel mismatch") + (tamperedKeys.length() == 0 ? "" : ": " + joinJsonArray(tamperedKeys)),
                    com.android.device.i18n.AppLocale.tr("YumyHook 四通道属性 Hook (getprop/SystemProperties/JNI)", "YumyHook four-channel property hook (getprop/SystemProperties/JNI)"),
                    com.android.device.i18n.AppLocale.tr("对齐 getprop、SystemProperties、jniGet、jniFind 返回值；勿只 Hook 单通道", "Align getprop, SystemProperties, jniGet, jniFind return values; do not hook a single channel only"),
                    com.android.device.i18n.AppLocale.tr("hook.propertyTampered=false，各 propertyProbes.*.tampered=false", "hook.propertyTampered=false, each propertyProbes.*.tampered=false"));
        }
    }

    private static void appendRootRemediation(JSONArray items, JSONObject summary, JSONObject rootSection)
            throws JSONException {
        JSONObject rootProbe = rootSection != null ? rootSection.optJSONObject("rootProbe") : null;
        if (rootProbe == null && rootSection != null) {
            rootProbe = rootSection.optJSONObject("magisk");
        }
        JSONObject shared = rootProbe != null ? rootProbe.optJSONObject("sharedIndicators") : null;
        JSONObject nativeProbe = shared != null ? shared.optJSONObject("nativeProbe") : null;

        if (summary.optBoolean("magiskHideSuspected", false)) {
            addItem(items, "root_hide_suspected", "medium", "root_hide",
                    com.android.device.i18n.AppLocale.tr("疑似 Root 已隐藏：存在 maps/mount/Native/df/ps 信号但 Java 路径不可见", "Suspected root hidden: maps/mount/Native/df/ps signals present but Java paths invisible"),
                    com.android.device.i18n.AppLocale.tr("YumyHook Java+Native 双层对齐", "YumyHook Java+Native dual-layer alignment"),
                    com.android.device.i18n.AppLocale.tr("Java File.exists 与 Native access 结果须一致；补齐 ProcMaps + native_bridge + Shell df/ps 过滤", "Java File.exists must match Native access; add ProcMaps + native_bridge + Shell df/ps filtering"),
                    "magiskHideSuspected=false");
        }

        appendPersieRemediation(items, rootProbe);

        if (nativeProbe != null) {
            appendNativeProbeRemediation(items, nativeProbe);
        }

        if (summary.optBoolean("magiskDetected", false)) {
            JSONObject magisk = getFramework(rootSection, "magisk");
            if (!hasItemId(items, "native_maps_magisk")) {
                addItem(items, "root_magisk_confirmed", "high", "root_framework",
                        com.android.device.i18n.AppLocale.tr("Magisk/Zygisk 已确认", "Magisk/Zygisk confirmed") + frameworkReasonSuffix(magisk),
                        "YumyHook native_bridge.cpp + SensitivePathStealthHook.kt",
                        com.android.device.i18n.AppLocale.tr("见 nativeProbe 相关项；过滤 magisk/zygisk 路径与 proc 行", "See nativeProbe items; filter magisk/zygisk paths and proc lines"),
                        "magiskDetected=false, frameworkConfirmed=false");
            }
        }
        if (summary.optBoolean("kernelsuDetected", false)) {
            addItem(items, "root_kernelsu_confirmed", "high", "root_framework",
                    com.android.device.i18n.AppLocale.tr("KernelSU 已确认", "KernelSU confirmed"),
                    "YumyHook native_bridge.cpp",
                    com.android.device.i18n.AppLocale.tr("隐藏 /data/adb/ksu、过滤 maps 中 kernelsu/ksud", "Hide /data/adb/ksu; filter kernelsu/ksud in maps"),
                    "kernelsuDetected=false");
        }
        if (summary.optBoolean("apatchDetected", false)) {
            addItem(items, "root_apatch_confirmed", "high", "root_framework",
                    com.android.device.i18n.AppLocale.tr("APatch 已确认", "APatch confirmed"),
                    "YumyHook native_bridge.cpp",
                    com.android.device.i18n.AppLocale.tr("隐藏 /data/adb/ap、/data/adb/apd，过滤 apatch 关键词", "Hide /data/adb/ap, /data/adb/apd; filter apatch keywords"),
                    "apatchDetected=false");
        }
        if (summary.optBoolean("systemSuDetected", false)) {
            addItem(items, "root_system_su", "high", "root_su",
                    com.android.device.i18n.AppLocale.tr("系统 su 已确认", "System su confirmed"),
                    "YumyHook SensitivePathStealthHook + ShellProbeStealthHook",
                    com.android.device.i18n.AppLocale.tr("隐藏 /system/xbin/su、which su、su -v 输出", "Hide /system/xbin/su, which su, su -v output"),
                    "systemSuDetected=false");
        }
        if (summary.optBoolean("rootAccessGranted", false)) {
            addItem(items, "root_su_granted", "critical", "root_su",
                    com.android.device.i18n.AppLocale.tr("su 授权探测成功", "su grant probe succeeded"),
                    com.android.device.i18n.AppLocale.tr("Magisk DenyList / 拒绝本包 su", "Magisk DenyList / deny su to this package"),
                    com.android.device.i18n.AppLocale.tr("将 com.android.device 加入 DenyList；Hook 无法替代 su 授权拒绝", "Add com.android.device to DenyList; hooking cannot replace su grant denial"),
                    "rootAccessGranted=false");
        }

        JSONArray mismatches = shared != null ? shared.optJSONArray("javaNativeMismatches") : null;
        if (mismatches != null && mismatches.length() > 0) {
            addItem(items, "root_java_native_mismatch", "high", "root_native",
                    com.android.device.i18n.AppLocale.tr("Java/Native 路径不一致: ", "Java/Native path mismatch: ") + joinJsonArray(mismatches),
                    "YumyHook native_bridge.cpp access/faccessat",
                    com.android.device.i18n.AppLocale.tr("Java 不可见的路径，Native access() 也必须失败（与 File.exists 对齐）", "Paths invisible to Java must also fail Native access() (aligned with File.exists)"),
                    "sharedIndicators.javaNativeMismatches=[]");
        }
    }

    private static void appendNativeProbeRemediation(JSONArray items, JSONObject nativeProbe)
            throws JSONException {
        JSONArray accessible = nativeProbe.optJSONArray("accessiblePaths");
        if (accessible != null) {
            for (int i = 0; i < accessible.length(); i++) {
                String path = accessible.optString(i);
                if (path.isEmpty()) {
                    continue;
                }
                String framework = guessFrameworkByPath(path);
                addItem(items, "native_access_" + sanitizeId(path), "critical", "root_native",
                        com.android.device.i18n.AppLocale.tr("Native access() 可访问: ", "Native access() accessible: ") + path,
                        "YumyHook/native_bridge.cpp",
                        com.android.device.i18n.AppLocale.tr("Hook access/faccessat/__syscall(faccessat)，对路径 ", "Hook access/faccessat/__syscall(faccessat) for path ") + path + com.android.device.i18n.AppLocale.tr(" 返回 -1 (ENOENT)", " returns -1 (ENOENT)"),
                        com.android.device.i18n.AppLocale.tr("nativeProbe.accessiblePaths 不含 ", "nativeProbe.accessiblePaths excludes ") + path,
                        framework);
            }
        }

        JSONArray mapsHits = nativeProbe.optJSONArray("mapsHits");
        if (mapsHits != null && mapsHits.length() > 0) {
            Set<String> unique = new LinkedHashSet<>();
            for (int i = 0; i < mapsHits.length(); i++) {
                unique.add(mapsHits.optString(i));
            }
            for (String keyword : unique) {
                if (keyword.isEmpty()) {
                    continue;
                }
                addItem(items, "native_maps_" + sanitizeId(keyword), "critical", "root_native",
                        com.android.device.i18n.AppLocale.tr("Native 读 /proc/self/maps 命中: ", "Native read /proc/self/maps hit: ") + keyword,
                        "YumyHook/native_bridge.cpp",
                        com.android.device.i18n.AppLocale.tr(
                                "Hook fopen/open + fgets/read，过滤 maps 行中含 \"" + keyword + "\" 的内容",
                                "Hook fopen/open + fgets/read, filter maps lines containing \"" + keyword + "\""),
                        com.android.device.i18n.AppLocale.tr("nativeProbe.mapsHits 不含 ", "nativeProbe.mapsHits excludes ") + keyword,
                        guessFrameworkByKeyword(keyword));
            }
        }

        JSONArray mountHits = nativeProbe.optJSONArray("mountHits");
        if (mountHits != null && mountHits.length() > 0) {
            Set<String> unique = new LinkedHashSet<>();
            for (int i = 0; i < mountHits.length(); i++) {
                unique.add(mountHits.optString(i));
            }
            for (String keyword : unique) {
                if (keyword.isEmpty()) {
                    continue;
                }
                addItem(items, "native_mount_" + sanitizeId(keyword), "critical", "root_native",
                        com.android.device.i18n.AppLocale.tr("Native 读 /proc/self/mountinfo 命中: ", "Native read /proc/self/mountinfo hit: ") + keyword,
                        "YumyHook/native_bridge.cpp",
                        com.android.device.i18n.AppLocale.tr(
                                "Hook fopen(\"/proc/self/mountinfo\") + fgets，过滤含 \"" + keyword + "\" 的行",
                                "Hook fopen(\"/proc/self/mountinfo\") + fgets, filter lines containing \"" + keyword + "\""),
                        com.android.device.i18n.AppLocale.tr("nativeProbe.mountHits 不含 ", "nativeProbe.mountHits excludes ") + keyword,
                        guessFrameworkByKeyword(keyword));
            }
        }
    }

    private static void appendEnvironmentRemediation(JSONArray items, JSONObject summary)
            throws JSONException {
        if (summary.optBoolean("isEmulator", false)) {
            addItem(items, "env_emulator", "medium", "environment",
                    com.android.device.i18n.AppLocale.tr("疑似模拟器", "Suspected emulator"),
                    com.android.device.i18n.AppLocale.tr("YumyHook 环境伪装模块", "YumyHook environment-spoofing module"),
                    com.android.device.i18n.AppLocale.tr("对齐 Build、传感器、QEMU 特征", "Align Build, sensor and QEMU characteristics"),
                    "environment.isEmulator=false");
        }
        if (summary.optBoolean("simulatorDetected", false)) {
            addItem(items, "env_simulator", "medium", "environment",
                    com.android.device.i18n.AppLocale.tr("模拟器综合判定", "Emulator overall verdict"),
                    "YumyHook SimulatorStealth",
                    com.android.device.i18n.AppLocale.tr("检查光线传感器、CPU 型号、模拟器文件", "Check light sensor, CPU model, emulator files"),
                    "simulator.detected=false");
        }
        if (summary.optBoolean("isVpn", false)) {
            addItem(items, "env_vpn", "low", "environment",
                    com.android.device.i18n.AppLocale.tr("VPN 已连接", "VPN connected"),
                    com.android.device.i18n.AppLocale.tr("系统层 / 非 Root Hide 范畴", "System-level / not root-hide category"),
                    com.android.device.i18n.AppLocale.tr("关闭 VPN 或按需 Hook NetworkCapabilities", "Disable VPN or hook NetworkCapabilities as needed"),
                    "environment.isVPN=false");
        }
        if (summary.optBoolean("isAdbEnabled", false)) {
            addItem(items, "env_adb", "low", "environment",
                    com.android.device.i18n.AppLocale.tr("ADB 调试已开启", "ADB debugging enabled"),
                    com.android.device.i18n.AppLocale.tr("设置 / 非 Hook 范畴", "Settings / not hook category"),
                    com.android.device.i18n.AppLocale.tr("关闭开发者选项 ADB，或 Hook Settings.Secure.ADB_ENABLED", "Turn off developer-options ADB, or hook Settings.Secure.ADB_ENABLED"),
                    "summary.isAdbEnabled=false");
        }
        if (summary.optBoolean("bootloaderUnlocked", false)
                && !summary.optBoolean("anyRootSignal", false)) {
            addItem(items, "boot_unlocked_info", "info", "bootloader",
                    com.android.device.i18n.AppLocale.tr("Bootloader 已解锁（不计入 anyRisk）", "Bootloader unlocked (not counted in anyRisk)"),
                    com.android.device.i18n.AppLocale.tr("无需修复", "No fix needed"),
                    com.android.device.i18n.AppLocale.tr("仅信息字段 bootloaderUnlocked=true，当前逻辑不单独判 Root", "info-only field bootloaderUnlocked=true; current logic does not treat it as root by itself"),
                    com.android.device.i18n.AppLocale.tr("bootloaderUnlocked 可保持 true，anyRisk 应为 false", "bootloaderUnlocked may stay true, anyRisk should be false"));
        }
    }

    private static JSONObject getFramework(JSONObject rootSection, String id) throws JSONException {
        if (rootSection == null) {
            return null;
        }
        JSONObject frameworks = rootSection.optJSONObject("frameworks");
        return frameworks != null ? frameworks.optJSONObject(id) : null;
    }

    private static String frameworkReasonSuffix(JSONObject framework) throws JSONException {
        if (framework == null) {
            return "";
        }
        JSONArray reasons = framework.optJSONArray("reasons");
        if (reasons == null || reasons.length() == 0) {
            return "";
        }
        return " (" + reasons.optString(0) + ")";
    }

    private static JSONArray collectTamperedKeys(JSONObject probes) throws JSONException {
        JSONArray keys = new JSONArray();
        if (probes == null) {
            return keys;
        }
        JSONArray names = probes.names();
        if (names == null) {
            return keys;
        }
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            JSONObject probe = probes.optJSONObject(key);
            if (probe != null && probe.optBoolean("tampered", false)) {
                keys.put(key);
            }
        }
        return keys;
    }

    private static void addItem(
            JSONArray items,
            String id,
            String severity,
            String category,
            String detection,
            String fixTarget,
            String fixAction,
            String verify
    ) throws JSONException {
        addItem(items, id, severity, category, detection, fixTarget, fixAction, verify, "");
    }

    private static void addItem(
            JSONArray items,
            String id,
            String severity,
            String category,
            String detection,
            String fixTarget,
            String fixAction,
            String verify,
            String framework
    ) throws JSONException {
        if (hasItemId(items, id)) {
            return;
        }
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("severity", severity);
        item.put("category", category);
        item.put("detection", detection);
        item.put("fixTarget", fixTarget);
        item.put("fixAction", fixAction);
        item.put("verify", verify);
        if (framework != null && !framework.isEmpty()) {
            item.put("framework", framework);
        }
        item.put("detectorSource", mapDetectorSource(category));
        items.put(item);
    }

    private static String mapDetectorSource(String category) {
        switch (category) {
            case "hook_java":
            case "hook_proc":
            case "hook_property":
                return "CheckEmu.buildHookSection";
            case "root_native":
                return "device.cpp/getMagiskNativeProbe + RootFrameworkDetector";
            case "root_framework":
            case "root_hide":
            case "root_su":
                return "RootFrameworkDetector.probe";
            case "environment":
                return "HackChecker/SimulatorChecker";
            case "bootloader":
                return "RootFrameworkDetector.bootUnlockSignals";
            default:
                return "SecurityReportComposer";
        }
    }

    private static void appendPersieRemediation(JSONArray items, JSONObject rootProbe) throws JSONException {
        if (rootProbe == null) {
            return;
        }
        JSONObject persie = rootProbe.optJSONObject("persieAligned");
        if (persie == null) {
            persie = rootProbe.optJSONObject("sharedIndicators") != null
                    ? rootProbe.optJSONObject("sharedIndicators").optJSONObject("persieAligned")
                    : null;
        }
        if (persie == null || !persie.optBoolean("detected", false)) {
            return;
        }
        JSONObject indicators = persie.optJSONObject("indicators");
        if (indicators == null) {
            return;
        }
        if (indicators.optJSONArray("dfHits") != null && indicators.optJSONArray("dfHits").length() > 0) {
            addItem(items, "persie_df_magisk", "high", "root_shell",
                    com.android.device.i18n.AppLocale.tr("df 输出含 Magisk/Zygisk 挂载", "df output contains Magisk/Zygisk mounts"),
                    "YumyHook/ShellProbeStealthHook.kt + ShellOutputFilter.kt",
                    com.android.device.i18n.AppLocale.tr("拦截 df 命令并过滤含 magisk/zygisk/ksu 的行", "Intercept df and filter lines containing magisk/zygisk/ksu"),
                    "persieAligned.indicators.dfHits=[]");
        }
        if (indicators.optJSONArray("psHits") != null && indicators.optJSONArray("psHits").length() > 0) {
            addItem(items, "persie_ps_zygisk", "high", "root_shell",
                    com.android.device.i18n.AppLocale.tr("ps 输出含 magiskd/zygisk/ksud", "ps output contains magiskd/zygisk/ksud"),
                    "YumyHook/ShellProbeStealthHook.kt",
                    com.android.device.i18n.AppLocale.tr("拦截 ps/ps -A 并过滤 root 框架进程名", "Intercept ps/ps -A and filter root-framework process names"),
                    "persieAligned.indicators.psHits=[]");
        }
        if (indicators.optJSONArray("unixSocketHits") != null
                && indicators.optJSONArray("unixSocketHits").length() > 0) {
            addItem(items, "persie_unix_socket", "high", "root_proc",
                    com.android.device.i18n.AppLocale.tr("/proc/net/unix 含 @magisk/@zygisk 等", "/proc/net/unix contains @magisk/@zygisk etc."),
                    "YumyHook/native_bridge.cpp fopen/fgets",
                    com.android.device.i18n.AppLocale.tr("过滤 /proc/net/unix 行中含 @magisk/@zygisk/@ksud/@apatch/@riru", "Filter /proc/net/unix lines containing @magisk/@zygisk/@ksud/@apatch/@riru"),
                    "persieAligned.indicators.unixSocketHits=[]");
        }
        if (indicators.optJSONArray("suspiciousMountHits") != null
                && indicators.optJSONArray("suspiciousMountHits").length() > 0) {
            addItem(items, "persie_mount_regex", "high", "root_proc",
                    com.android.device.i18n.AppLocale.tr("挂载表命中 persie 可疑正则", "Mount table matched persie suspicious regex"),
                    "YumyHook/native_bridge.cpp + ProcMapsStealthHook.kt",
                    com.android.device.i18n.AppLocale.tr("双层过滤 mountinfo/mounts 中 magisk/zygisk/ksu/apatch overlay 特征", "Dual-layer filter magisk/zygisk/ksu/apatch overlay markers in mountinfo/mounts"),
                    "persieAligned.indicators.suspiciousMountHits=[]");
        }
    }

    private static boolean hasItemId(JSONArray items, String id) throws JSONException {
        for (int i = 0; i < items.length(); i++) {
            if (id.equals(items.optJSONObject(i).optString("id"))) {
                return true;
            }
        }
        return false;
    }

    private static String guessFrameworkByPath(String path) {
        String lower = path.toLowerCase(Locale.US);
        if (lower.contains("magisk")) {
            return "magisk";
        }
        if (lower.contains("ksu") || lower.contains("kernelsu")) {
            return "kernelsu";
        }
        if (lower.contains("/ap") || lower.contains("apd")) {
            return "apatch";
        }
        if (lower.contains("su")) {
            return "systemSu";
        }
        return "";
    }

    private static String guessFrameworkByKeyword(String keyword) {
        String lower = keyword.toLowerCase(Locale.US);
        if (lower.contains("magisk") || lower.contains("zygisk") || lower.contains("resetprop")) {
            return "magisk";
        }
        if (lower.contains("kernel") || lower.contains("ksu")) {
            return "kernelsu";
        }
        if (lower.contains("apatch") || lower.contains("apd") || lower.contains("bmax")) {
            return "apatch";
        }
        if (lower.contains("su") || lower.contains("superuser")) {
            return "systemSu";
        }
        return "";
    }

    private static String sanitizeId(String value) {
        return value.replaceAll("[^a-zA-Z0-9]+", "_").toLowerCase(Locale.US);
    }

    private static String joinJsonArray(JSONArray array) throws JSONException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(array.optString(i));
        }
        return sb.toString();
    }

    private static String buildSummaryText(JSONObject summary, JSONArray items) throws JSONException {
        if (items.length() == 0) {
            return com.android.device.i18n.AppLocale.tr("存在 anyRisk 但未生成修复项，请检查 security 明细。", "anyRisk is set but no fix items were generated; check the security detail.");
        }
        boolean nativeLeak = false;
        for (int i = 0; i < items.length(); i++) {
            if ("root_native".equals(items.optJSONObject(i).optString("category"))) {
                nativeLeak = true;
                break;
            }
        }
        if (nativeLeak) {
            return com.android.device.i18n.AppLocale.tr("Native JNI 层未对齐（access/proc 读仍泄漏 Root 信号），优先修复 YumyHook/native_bridge.cpp", "Native JNI layer not aligned (access/proc reads still leak root signals); fix YumyHook/native_bridge.cpp first");
        }
        if (summary.optBoolean("propertyTampered", false)) {
            return com.android.device.i18n.AppLocale.tr("属性多通道不一致，需对齐 YumyHook 四通道属性 Hook", "Property multi-channel mismatch; align YumyHook four-channel property hook");
        }
        if (summary.optBoolean("hookFrameworkDetected", false)) {
            return com.android.device.i18n.AppLocale.tr("Hook 框架信号未隐藏，检查 ProcMaps / 类加载 Hook", "Hook framework signals not hidden; check ProcMaps / classloader hook");
        }
        return com.android.device.i18n.AppLocale.tr("存在安全风险，请按 items 逐项修复并回归 debug_output.json", "Security risks present; fix each item and re-verify debug_output.json");
    }

    private static JSONArray buildRiskChecklist(JSONObject summary) throws JSONException {
        JSONArray checklist = new JSONArray();
        checklist.put("security.summary.anyRisk → false");
        checklist.put(com.android.device.i18n.AppLocale.tr("security.summary.frameworkConfirmed → false（无经典 Root 指标时）", "security.summary.frameworkConfirmed -> false (when no classic root indicators)"));
        checklist.put("security.summary.magiskDetected / kernelsuDetected / apatchDetected / systemSuDetected → false");
        checklist.put("security.root.rootProbe.sharedIndicators.nativeProbe.anyHit → false");
        checklist.put("security.root.rootProbe.sharedIndicators.javaNativeMismatches → []");
        checklist.put("security.remediation.verdict → PASS");
        if (summary.optBoolean("propertyTampered", false)) {
            checklist.put("security.hook.propertyProbes.*.tampered → false");
        }
        return checklist;
    }

    private static JSONArray buildPassChecklist() throws JSONException {
        JSONArray checklist = new JSONArray();
        checklist.put("security.summary.anyRisk → false");
        checklist.put("security.remediation.verdict → PASS");
        return checklist;
    }

    private static JSONObject buildYumyhookReferences() throws JSONException {
        JSONObject refs = new JSONObject();
        refs.put("nativeBridge", "YumyHook/app/src/main/cpp/native_bridge.cpp");
        refs.put("procMapsHook", "YumyHook/.../ProcMapsStealthHook.kt");
        refs.put("pathHook", "YumyHook/.../SensitivePathStealthHook.kt");
        refs.put("shellHook", "YumyHook/.../ShellProbeStealthHook.kt");
        refs.put("propertyHook", com.android.device.i18n.AppLocale.tr("YumyHook 四通道 SystemProperties/getprop/JNI", "YumyHook four-channel SystemProperties/getprop/JNI"));
        refs.put("logcatTag", "YH-NATIVE-STEALTH");
        refs.put("regressionDoc", "deviceinfo_collect/docs/SECURITY_RISK_ANALYSIS.md");
        refs.put("nativeProbeJni", "com.android.device.Jni.JniInterface.getMagiskNativeProbe");
        refs.put("nativeProbeCpp", "deviceinfo_collect/app/src/main/cpp/device.cpp");
        return refs;
    }
}
