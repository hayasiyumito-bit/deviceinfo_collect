package com.android.device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 将快照 JSON 解析为带分类标题的列表项。 */
public final class DeviceInfoParser {

    /** 与其它块重复的大聚合字段，不再单独展示。 */
    private static final Set<String> SKIP_TOP_LEVEL_KEYS = new HashSet<>(Arrays.asList(
            "deviceInfo"
    ));

    /** 不参与内容去重的 key（系统 Tab 构建信息拆分）。 */
    private static final Set<String> DEDUP_EXEMPT_PREFIXES = new HashSet<>(Arrays.asList(
            "build",
            "build."
    ));

    private DeviceInfoParser() {
    }

    public static List<Object> parse(JSONObject jsonObject) throws JSONException {
        List<Object> items = new ArrayList<>();
        Map<String, List<DeviceInfoItem>> categoryMap = new HashMap<>();
        Set<String> seenContent = new HashSet<>();

        for (String key : sortedJsonKeys(jsonObject)) {
            if (SKIP_TOP_LEVEL_KEYS.contains(key)) {
                continue;
            }
            Object value = jsonObject.get(key);
            String fullValue = value != null ? value.toString() : "null";
            if (shouldSkipDuplicate(key, fullValue, seenContent)) {
                continue;
            }
            registerContent(key, fullValue, seenContent);

            String category = categorizeKey(key);
            categoryMap.computeIfAbsent(category, k -> new ArrayList<>()).add(
                    new DeviceInfoItem(
                            key,
                            translateKey(key),
                            formatValue(value),
                            category,
                            fullValue
                    )
            );
        }

        List<String> sortedCategories = new ArrayList<>(categoryMap.keySet());
        Collections.sort(sortedCategories, (a, b) -> {
            int pa = categoryOrder(a);
            int pb = categoryOrder(b);
            return pa != pb ? Integer.compare(pa, pb) : a.compareTo(b);
        });

        for (String category : sortedCategories) {
            appendCategorySection(items, category, categoryMap.get(category));
        }
        return items;
    }

    public static int countDataItems(List<Object> items) {
        int count = 0;
        for (Object item : items) {
            if (item instanceof DeviceInfoItem) {
                count++;
            }
        }
        return count;
    }

    private static void appendCategorySection(
            List<Object> items,
            String category,
            List<DeviceInfoItem> categoryItems
    ) {
        if (categoryItems == null || categoryItems.isEmpty()) {
            return;
        }
        items.add(category);
        if ("系统信息".equals(category)) {
            appendSystemSection(items, categoryItems);
        } else if ("安全检测".equals(category)) {
            appendSecuritySection(items, categoryItems);
        } else {
            categoryItems.sort(Comparator.comparing(DeviceInfoItem::getTranslatedKey));
            items.addAll(categoryItems);
        }
    }

    /** 系统 Tab：构建信息 JSON 置顶，随后逐字段拆分，再展示其它系统项。 */
    private static void appendSystemSection(List<Object> items, List<DeviceInfoItem> categoryItems) {
        DeviceInfoItem buildItem = findItem(categoryItems, "build");
        if (buildItem != null) {
            items.add(buildItem);
            items.addAll(createBuildFieldItems(buildItem.getFullValue()));
        }
        List<DeviceInfoItem> rest = new ArrayList<>();
        for (DeviceInfoItem item : categoryItems) {
            if (!"build".equals(item.getOriginalKey())) {
                rest.add(item);
            }
        }
        rest.sort(Comparator.comparing(DeviceInfoItem::getTranslatedKey));
        items.addAll(rest);
    }

    /** 安全 Tab：anyRisk 置顶，再展示 security 块明细。 */
    private static void appendSecuritySection(List<Object> items, List<DeviceInfoItem> categoryItems) {
        addIfPresent(items, categoryItems, "anyRisk");
        DeviceInfoItem anyRiskReasonsItem = findItem(categoryItems, "anyRiskReasons");
        if (anyRiskReasonsItem != null) {
            try {
                JSONArray reasons = new JSONArray(anyRiskReasonsItem.getFullValue());
                if (reasons.length() > 0) {
                    items.add(new DeviceInfoItem(
                            "anyRiskReasons",
                            "安全风险原因",
                            formatReasonArray(reasons),
                            "安全检测",
                            anyRiskReasonsItem.getFullValue()
                    ));
                }
            } catch (JSONException ignored) {
                items.add(anyRiskReasonsItem);
            }
        }

        DeviceInfoItem securityItem = findItem(categoryItems, "security");
        if (securityItem != null) {
            appendUnifiedSecuritySection(items, securityItem.getFullValue());
        } else {
            addIfPresent(items, categoryItems, "rootAccessGranted");
            addIfPresent(items, categoryItems, "rootAccessDetail");
            DeviceInfoItem envItem = findItem(categoryItems, "envCheck");
            if (envItem != null) {
                items.add(envItem);
                items.addAll(createExpandedFieldItems("envCheck", envItem.getFullValue(), "安全检测"));
            }
            DeviceInfoItem securityCheckItem = findItem(categoryItems, "securityCheck");
            if (securityCheckItem != null) {
                items.add(securityCheckItem);
                items.addAll(createExpandedFieldItems(
                        "securityCheck",
                        securityCheckItem.getFullValue(),
                        "安全检测",
                        "rootAccessGranted",
                        "rootAccessDetail"
                ));
            }
        }

        List<DeviceInfoItem> rest = new ArrayList<>();
        for (DeviceInfoItem item : categoryItems) {
            String key = item.getOriginalKey();
            if ("anyRisk".equals(key)
                    || "anyRiskReasons".equals(key)
                    || "security".equals(key)
                    || "rootAccessGranted".equals(key)
                    || "rootAccessDetail".equals(key)
                    || "envCheck".equals(key)
                    || "securityCheck".equals(key)
                    || key.startsWith("envCheck.")
                    || key.startsWith("securityCheck.")
                    || key.startsWith("security.")) {
                continue;
            }
            rest.add(item);
        }
        rest.sort(Comparator.comparing(DeviceInfoItem::getTranslatedKey));
        items.addAll(rest);
    }

    private static void appendUnifiedSecuritySection(List<Object> items, String securityJson) {
        try {
            JSONObject security = new JSONObject(securityJson);

            JSONObject summary = security.optJSONObject("summary");
            if (summary != null) {
                items.add(new DeviceInfoItem(
                        "security.summary",
                        "安全检测摘要",
                        formatValue(summary),
                        "安全检测",
                        summary.toString()
                ));
                items.addAll(createExpandedFieldItems("security.summary", summary.toString(), "安全检测"));
            }

            appendReasonBlock(items, security.optJSONObject("reasons"), "hook", "Hook 检测原因");
            appendReasonBlock(items, security.optJSONObject("reasons"), "root", "Root 检测原因");
            appendReasonBlock(items, security.optJSONObject("reasons"), "propertyTamper", "属性篡改原因");
            appendReasonBlock(items, security.optJSONObject("reasons"), "environment", "环境检测原因");
            appendReasonBlock(items, security.optJSONObject("reasons"), "adb", "ADB 检测原因");
            appendReasonBlock(items, security.optJSONObject("reasons"), "simulator", "模拟器检测原因");

            appendDetailBlock(items, security.optJSONObject("hook"), "security.hook", "Hook 检测明细");
            appendDetailBlock(items, security.optJSONObject("root"), "security.root", "Root 检测明细");
            appendDetailBlock(items, security.optJSONObject("environment"), "security.environment", "环境检测明细");
            appendDetailBlock(items, security.optJSONObject("simulator"), "security.simulator", "模拟器检测明细");
        } catch (JSONException e) {
            items.add(new DeviceInfoItem(
                    "security",
                    "安全检测",
                    securityJson,
                    "安全检测",
                    securityJson
            ));
        }
    }

    private static void appendReasonBlock(
            List<Object> items,
            JSONObject reasons,
            String key,
            String title
    ) throws JSONException {
        if (reasons == null) {
            return;
        }
        JSONArray array = reasons.optJSONArray(key);
        if (array == null || array.length() == 0) {
            return;
        }
        String fullValue = array.toString();
        items.add(new DeviceInfoItem(
                "security.reasons." + key,
                title,
                formatReasonArray(array),
                "安全检测",
                fullValue
        ));
    }

    private static void appendDetailBlock(
            List<Object> items,
            JSONObject detail,
            String prefix,
            String title
    ) throws JSONException {
        if (detail == null) {
            return;
        }
        String fullValue = detail.toString();
        items.add(new DeviceInfoItem(
                prefix,
                title,
                formatValue(detail),
                "安全检测",
                fullValue
        ));
        items.addAll(createExpandedFieldItems(prefix, fullValue, "安全检测"));
    }

    private static String formatReasonArray(JSONArray array) throws JSONException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append("• ").append(array.optString(i));
        }
        return sb.toString();
    }

    private static void addIfPresent(List<Object> items, List<DeviceInfoItem> categoryItems, String key) {
        DeviceInfoItem item = findItem(categoryItems, key);
        if (item != null) {
            items.add(item);
        }
    }

    private static DeviceInfoItem findItem(List<DeviceInfoItem> items, String key) {
        for (DeviceInfoItem item : items) {
            if (key.equals(item.getOriginalKey())) {
                return item;
            }
        }
        return null;
    }

    private static boolean shouldSkipDuplicate(String key, String fullValue, Set<String> seenContent) {
        if (isDedupExempt(key)) {
            return false;
        }
        String fingerprint = contentFingerprint(fullValue);
        return fingerprint != null && seenContent.contains(fingerprint);
    }

    private static void registerContent(String key, String fullValue, Set<String> seenContent) {
        if (isDedupExempt(key)) {
            return;
        }
        String fingerprint = contentFingerprint(fullValue);
        if (fingerprint != null) {
            seenContent.add(fingerprint);
        }
    }

    private static boolean isDedupExempt(String key) {
        if ("build".equals(key)) {
            return true;
        }
        for (String prefix : DEDUP_EXEMPT_PREFIXES) {
            if (prefix.endsWith(".") && key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String contentFingerprint(String fullValue) {
        if (fullValue == null) {
            return null;
        }
        String trimmed = fullValue.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static List<DeviceInfoItem> createExpandedFieldItems(
            String prefix,
            String json,
            String category,
            String... skipFieldKeys
    ) {
        Set<String> skip = new HashSet<>(Arrays.asList(skipFieldKeys));
        List<DeviceInfoItem> items = new ArrayList<>();
        try {
            JSONObject object = new JSONObject(json);
            for (String fieldKey : sortedJsonKeys(object)) {
                if (skip.contains(fieldKey)) {
                    continue;
                }
                Object fieldValue = object.get(fieldKey);
                String originalKey = prefix + "." + fieldKey;
                items.add(new DeviceInfoItem(
                        originalKey,
                        translateExpandedField(prefix, fieldKey),
                        formatValue(fieldValue),
                        category,
                        fieldValue != null ? String.valueOf(fieldValue) : "null"
                ));
            }
        } catch (JSONException ignored) {
        }
        return items;
    }

    private static String translateExpandedField(String prefix, String fieldKey) {
        if ("build".equals(prefix) || fieldKey.startsWith("build.")) {
            return translateBuildField(fieldKey);
        }
        return translateKey(fieldKey);
    }

    /** 构建信息详情：顶部完整 JSON，下方逐字段展示。 */
    public static String formatBuildDetailContent(String buildJson) {
        try {
            JSONObject build = new JSONObject(buildJson);
            StringBuilder sb = new StringBuilder();
            sb.append(build.toString(2));
            sb.append("\n\n──────── 字段明细 ────────\n\n");
            for (String fieldKey : sortedJsonKeys(build)) {
                Object value = build.get(fieldKey);
                sb.append(translateBuildField(fieldKey))
                        .append(" (")
                        .append(fieldKey)
                        .append("): ")
                        .append(value)
                        .append('\n');
            }
            return sb.toString();
        } catch (JSONException e) {
            return buildJson;
        }
    }

    private static List<DeviceInfoItem> createBuildFieldItems(String buildJson) {
        List<DeviceInfoItem> items = new ArrayList<>();
        try {
            JSONObject build = new JSONObject(buildJson);
            for (String fieldKey : sortedJsonKeys(build)) {
                Object fieldValue = build.get(fieldKey);
                String originalKey = "build." + fieldKey;
                items.add(new DeviceInfoItem(
                        originalKey,
                        translateBuildField(fieldKey),
                        formatValue(fieldValue),
                        "系统信息",
                        fieldValue != null ? String.valueOf(fieldValue) : "null"
                ));
            }
        } catch (JSONException ignored) {
        }
        return items;
    }

    private static List<String> sortedJsonKeys(JSONObject jsonObject) {
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        Collections.sort(keys);
        return keys;
    }

    static String translateBuildField(String fieldKey) {
        switch (fieldKey) {
            case "MODEL":
                return "型号";
            case "BRAND":
                return "品牌";
            case "MANUFACTURER":
                return "制造商";
            case "DEVICE":
                return "设备名";
            case "PRODUCT":
                return "产品名";
            case "FINGERPRINT":
                return "指纹";
            case "HARDWARE":
                return "硬件";
            case "BOARD":
                return "主板";
            case "BOOTLOADER":
                return "Bootloader";
            case "DISPLAY":
                return "显示 ID";
            case "HOST":
                return "编译主机";
            case "ID":
                return "构建 ID";
            case "TAGS":
                return "标签";
            case "TYPE":
                return "构建类型";
            case "USER":
                return "构建用户";
            case "TIME":
                return "构建时间戳";
            case "RADIO":
                return "基带版本";
            case "CPU_ABI":
                return "CPU ABI";
            case "CPU_ABI2":
                return "CPU ABI2";
            case "SUPPORTED_ABIS":
                return "支持的 ABI";
            case "SUPPORTED_32_BIT_ABIS":
                return "32 位 ABI";
            case "SUPPORTED_64_BIT_ABIS":
                return "64 位 ABI";
            case "SERIAL":
                return "序列号";
            case "SDK_INT":
                return "SDK 版本";
            case "RELEASE":
                return "系统版本";
            case "INCREMENTAL":
                return "增量版本";
            case "CODENAME":
                return "代号";
            case "SECURITY_PATCH":
                return "安全补丁";
            case "BASE_OS":
                return "基础系统";
            case "PREVIEW_SDK_INT":
                return "预览 SDK";
            case "RESOURCES_SDK_INT":
                return "资源 SDK";
            default:
                return fieldKey;
        }
    }

    private static int categoryOrder(String category) {
        switch (category) {
            case "安全检测":
                return 0;
            case "系统信息":
                return 1;
            case "硬件信息":
                return 2;
            case "网络信息":
                return 3;
            case "存储信息":
                return 4;
            case "传感器信息":
                return 5;
            case "软件信息":
                return 6;
            default:
                return 7;
        }
    }

    static String categorizeKey(String key) {
        if ("security".equals(key) || key.startsWith("security.")
                || "anyRisk".equals(key) || "anyRiskReasons".equals(key)) {
            return "安全检测";
        }
        if (key.startsWith("rootAccess")
                || key.startsWith("envCheck")
                || key.startsWith("securityCheck")
                || key.startsWith("simulator_")
                || key.equals("security")) {
            return "安全检测";
        }
        if ("build".equals(key) || key.startsWith("build.")) {
            return "系统信息";
        }
        if (key.equals("time") || key.equals("uname") || key.equals("fileStat")
                || key.equals("ringTitle") || key.equals("collectedAt") || key.equals("rootProbeSeq")) {
            return "系统信息";
        }
        if (key.contains("hardware") || key.contains("battery") || key.contains("gpu")
                || key.contains("usb") || key.contains("inputDevices")) {
            return "硬件信息";
        }
        if (key.contains("net") || key.contains("location")) {
            return "网络信息";
        }
        if (key.contains("storage") || key.contains("mem")) {
            return "存储信息";
        }
        if ("sensor".equals(key)) {
            return "传感器信息";
        }
        if (key.contains("package") || key.contains("library") || key.contains("media")
                || key.contains("font") || key.contains("input") || key.contains("service")
                || key.contains("installedApps") || key.contains("appsflyer")) {
            return "软件信息";
        }
        return "其他信息";
    }

    static String translateKey(String key) {
        switch (key) {
            case "time":
                return "收集时间";
            case "collectedAt":
                return "本次采集时间戳(ms)";
            case "anyRisk":
                return "存在安全风险";
            case "anyRiskReasons":
                return "安全风险原因";
            case "security":
                return "安全检测(完整JSON)";
            case "security.summary":
                return "安全检测摘要";
            case "security.reasons.hook":
                return "Hook 检测原因";
            case "security.reasons.root":
                return "Root 检测原因";
            case "security.reasons.propertyTamper":
                return "属性篡改原因";
            case "security.reasons.environment":
                return "环境检测原因";
            case "security.reasons.adb":
                return "ADB 检测原因";
            case "security.reasons.simulator":
                return "模拟器检测原因";
            case "security.hook":
                return "Hook 检测明细";
            case "security.root":
                return "Root 检测明细";
            case "security.environment":
                return "环境检测明细";
            case "security.simulator":
                return "模拟器检测明细";
            case "frameworkDetected":
                return "检测到 Hook 框架";
            case "propertyTampered":
                return "系统属性被篡改";
            case "frameworkIndicators":
                return "Hook 框架特征";
            case "accessGranted":
                return "Root 授权结果";
            case "accessDetail":
                return "Root 授权详情";
            case "indicators":
                return "Root 特征项";
            case "matchedSuPaths":
                return "命中的 su 路径";
            case "matchedMagiskPaths":
                return "命中的 Magisk 路径";
            case "suWhichPath":
                return "which su 结果";
            case "tamperReason":
                return "篡改原因";
            case "isVpn":
                return "VPN 已连接";
            case "simulatorDetected":
                return "模拟器综合判定";
            case "anyHookSignal":
                return "存在 Hook 信号";
            case "anyRootSignal":
                return "存在 Root 信号";
            case "detected":
                return "模拟器判定";
            case "isPcCpu":
                return "PC侧CPU(Intel/AMD)";
            case "emulatorFiles":
                return "模拟器特征文件列表";
            case "envCheck":
                return "环境检测(完整JSON)";
            case "securityCheck":
                return "Hook/Root检测(完整JSON)";
            case "rootAccessGranted":
                return "Root 授权结果";
            case "rootAccessDetail":
                return "Root 授权详情";
            case "isRooted":
                return "已 Root";
            case "isAdbEnabled":
                return "ADB 调试已开启";
            case "isPropertyTampered":
                return "系统属性被篡改";
            case "hookFrameworkDetected":
                return "检测到 Hook 框架";
            case "hookFrameworkIndicators":
                return "Hook 框架特征明细";
            case "hookDetectionSummary":
                return "Hook 检测摘要";
            case "hookFrameworkFilesPresent":
                return "Hook 特征文件(存在)";
            case "procMapsMatches":
                return "/proc/self/maps 命中关键词";
            case "procMapsScanned":
                return "已扫描 proc maps";
            case "xposedClassPresent":
                return "Xposed 类存在";
            case "lsposedClassPresent":
                return "LSPosed 类存在";
            case "tamperedPropertyKeys":
                return "不一致的属性键";
            case "probePropertyKeys":
                return "探测属性键列表";
            case "detectedSignals":
                return "检测到的信号";
            case "propertyProbes":
                return "属性多通道探测";
            case "rootIndicators":
                return "Root 特征项";
            case "suBinaryExists":
                return "存在 su 二进制";
            case "suCommandAvailable":
                return "可执行 su 命令";
            case "suShellGranted":
                return "su 命令已获 Root";
            case "testKeysBuild":
                return "test-keys 构建";
            case "roSecureOff":
                return "ro.secure=0";
            case "roDebuggableOn":
                return "ro.debuggable=1";
            case "rootedSystemProperty":
                return "Root 系统属性";
            case "magiskPathExists":
                return "Magisk 路径存在";
            case "getprop":
                return "getprop 通道";
            case "SystemProperties":
                return "SystemProperties 通道";
            case "jniFind":
                return "JNI __system_property_find";
            case "libcutils":
                return "JNI property_get";
            case "tampered":
                return "通道不一致(疑似 Hook)";
            case "simulator_detected":
                return "模拟器综合判定";
            case "simulator_hasLightSensor":
                return "光线传感器存在";
            case "simulator_isPcCpu":
                return "PC侧CPU(Intel/AMD)";
            case "simulator_emulatorFiles":
                return "模拟器特征文件";
            case "isEmulator":
                return "疑似模拟器";
            case "isVPN":
                return "VPN 已连接";
            case "isDebug":
                return "调试/可调试";
            case "appsflyerdebuginfo":
                return "AppsFlyer调试信息";
            case "ids":
                return "设备标识";
            case "build":
                return "构建信息(完整JSON)";
            case "storage":
                return "存储信息";
            case "sensor":
                return "传感器信息";
            case "hardware":
                return "硬件信息";
            case "batteryInfo":
                return "电池信息";
            case "net":
                return "网络信息";
            case "location":
                return "位置信息";
            case "packageInfo":
                return "包信息";
            case "uname":
                return "系统信息(uname)";
            case "fileStat":
                return "文件状态";
            case "ringTitle":
                return "默认铃声";
            case "installedApps":
                return "已安装应用";
            case "service_list":
                return "服务列表";
            default:
                if (key.startsWith("build.")) {
                    return translateBuildField(key.substring("build.".length()));
                }
                if (key.startsWith("envCheck.") || key.startsWith("securityCheck.") || key.startsWith("security.")) {
                    int dot = key.indexOf('.');
                    return translateKey(key.substring(dot + 1));
                }
                return key;
        }
    }

    static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof JSONObject) {
            return "[JSON对象 · 点击查看详情]";
        }
        if (value instanceof JSONArray) {
            return "[JSON数组 · 点击查看详情]";
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? "是" : "否";
        }
        String text = value.toString();
        if ("true".equalsIgnoreCase(text)) {
            return "是";
        }
        if ("false".equalsIgnoreCase(text)) {
            return "否";
        }
        if (text.length() > 120) {
            return text.substring(0, 117) + "...";
        }
        return text;
    }

    static String formatJsonForDisplay(Object json) {
        try {
            if (json instanceof JSONObject) {
                return formatJsonObject((JSONObject) json, 0);
            }
            if (json instanceof JSONArray) {
                return formatJsonArray((JSONArray) json, 0);
            }
        } catch (Exception ignored) {
        }
        return String.valueOf(json);
    }

    private static String formatJsonObject(JSONObject jsonObject, int indent) throws JSONException {
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(Math.max(0, indent));
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            sb.append(indentStr).append(translateKey(key)).append(": ");
            if (value instanceof JSONObject) {
                sb.append('\n').append(formatJsonObject((JSONObject) value, indent + 1));
            } else if (value instanceof JSONArray) {
                sb.append('\n').append(formatJsonArray((JSONArray) value, indent + 1));
            } else {
                sb.append(formatValue(value));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String formatJsonArray(JSONArray jsonArray, int indent) throws JSONException {
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(Math.max(0, indent));
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            sb.append(indentStr).append('[').append(i).append("]: ");
            if (value instanceof JSONObject) {
                sb.append('\n').append(formatJsonObject((JSONObject) value, indent + 1));
            } else if (value instanceof JSONArray) {
                sb.append('\n').append(formatJsonArray((JSONArray) value, indent + 1));
            } else {
                sb.append(formatValue(value));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
