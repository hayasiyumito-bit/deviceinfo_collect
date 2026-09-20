package com.android.device.ui

import android.content.Context
import com.android.device.DeviceInfoParser
import com.android.device.R
import com.android.device.i18n.AppLocale
import org.json.JSONArray
import org.json.JSONObject

/**
 * 把原始快照 JSON 按 Tab 关心的顶层字段展开成卡片：
 *  - 标量字段直接翻译展示；
 *  - 嵌套对象若不大则逐字段展开（如 storage → blockCount / 可用空间）；
 *  - 过大或晦涩的对象/数组（如 powerProfile 功耗系数、camera 列表）收进「详情」，点击弹完整 JSON；
 *  - 过滤空值，减少重复噪声。
 *
 * 翻译优先复用 [DeviceInfoParser.translateKey] 的词典，未命中再用本类补充词典，最后美化原键名。
 */
object SnapshotExpander {

    /** 一个嵌套对象最多展开这么多字段，超过则并入「详情」。 */
    private const val EXPAND_MAX = 16

    /** 隐藏的裸码/冗余键：已有更友好的同义字段(networkTypeName 等)，或对用户无意义。 */
    private val HIDDEN_KEYS = setOf(
        "networkType", "networkSubType", "networkSpecifier", "networkSubTypeName",
        "provenanceFingerprint", "buildPackage", "buildType", "artifactVersion"
    )

    fun expand(context: Context, raw: JSONObject, topKeys: List<String>): List<CardItem> {
        val cards = mutableListOf<CardItem>()
        val misc = mutableListOf<CardItem.Row>()

        for (key in topKeys) {
            if (!raw.has(key)) continue
            // 深度解析：CPU/摄像头、传感器走专门解析器，输出结构化卡片
            if (key == "hardware") {
                raw.optJSONObject(key)?.let { cards.addAll(HardwareParser.parseHardware(context, it)) }
                continue
            }
            if (key == "sensor") {
                raw.optJSONObject(key)?.let { cards.add(HardwareParser.parseSensors(context, it)) }
                continue
            }
            when (val v = raw.opt(key)) {
                is JSONObject -> {
                    // 顶层对象总是展开成一张卡；卡内的大/嵌套子字段再各自收进详情。
                    val rows = expandObject(v, 0)
                    if (rows.isNotEmpty()) cards.add(CardItem.Body(label(key), rows))
                }
                is JSONArray -> {
                    if (v.length() > 0) {
                        misc.add(detailRow("${label(key)} (${v.length()})", v.toString()))
                    }
                }
                else -> {
                    val row = scalarRow(key, v)
                    if (row != null) misc.add(row)
                }
            }
        }

        if (misc.isNotEmpty()) {
            cards.add(0, CardItem.Body(context.getString(R.string.card_overview), misc))
        }
        return cards
    }

    /** 展开一个对象的字段为行；depth<1 时对可展开的子对象再递归一层（带分节前缀）。 */
    private fun expandObject(obj: JSONObject, depth: Int): List<CardItem.Row> {
        val rows = mutableListOf<CardItem.Row>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k in HIDDEN_KEYS) continue
            when (val v = obj.opt(k)) {
                is JSONObject -> {
                    if (depth < 1 && v.length() in 1..EXPAND_MAX) {
                        for (child in expandObject(v, depth + 1)) {
                            rows.add(child.copy(label = "${label(k)} · ${child.label}"))
                        }
                    } else if (v.length() > 0) {
                        rows.add(detailRow(label(k), v.toString()))
                    }
                }
                is JSONArray -> {
                    if (v.length() > 0) rows.add(detailRow("${label(k)} (${v.length()})", v.toString()))
                }
                else -> scalarRow(k, v)?.let { rows.add(it) }
            }
        }
        return rows
    }

    /** 标量行：过滤空/无意义值；超长文本转「详情」查看。 */
    private fun scalarRow(key: String, v: Any?): CardItem.Row? {
        val text = scalarText(key, v) ?: return null
        // 过长的文本（如 cpuinfo、指纹串）不挤在行内，改为点击查看详情
        return if (text.length > 60) detailRow(label(key), text)
        else CardItem.Row(label(key), text, text, key)
    }

    private fun scalarText(key: String, v: Any?): String? {
        if (v == null || v == JSONObject.NULL) return null
        if (v is Boolean) return if (v) yes() else no()
        // 过滤无效哨兵值（Long.MIN_VALUE 表示驱动未上报）
        if (v is Number && v.toLong() == Long.MIN_VALUE) return null
        var s = v.toString().trim()
        if (s.isEmpty() || s == "null") return null
        // 字节字段附带可读单位
        if ((key.endsWith("Bytes") || key.endsWith("bytes")) && v is Number) {
            val gb = v.toLong() / 1073741824.0
            if (gb >= 0.01) s = "%s (%.2f GB)".format(s, gb)
        }
        return s
    }

    private fun detailRow(label: String, json: String): CardItem.Row {
        val hint = AppLocale.tr("查看详情 ›", "View detail ›")
        return CardItem.Row(label, hint, json, "__detail__")
    }

    // ---- 翻译 ----

    private fun label(key: String): String {
        val t = DeviceInfoParser.translateKey(key)
        if (t.isNotEmpty() && t != key) return t
        SUPP[key]?.let { return AppLocale.tr(it[0], it[1]) }
        return beautify(key)
    }

    private fun beautify(key: String): String =
        key.replace('_', ' ').replace('-', ' ')
            .split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    private fun yes() = AppLocale.tr("是", "Yes")
    private fun no() = AppLocale.tr("否", "No")

    /** 补充词典：translateKey 未覆盖的常见子键。数组为 [中文, English]。 */
    private val SUPP: Map<String, Array<String>> = mapOf(
        // 电池
        "charge_counter" to arrayOf("电量计数器 (µAh)", "Charge counter (µAh)"),
        "current_now" to arrayOf("瞬时电流 (µA)", "Current now (µA)"),
        "current_average" to arrayOf("平均电流 (µA)", "Average current (µA)"),
        "battery_capacity" to arrayOf("电量百分比 (%)", "Battery level (%)"),
        "energy_counter" to arrayOf("能量计数器 (nWh)", "Energy counter (nWh)"),
        "battery_status" to arrayOf("电池状态码", "Battery status"),
        "isCharging" to arrayOf("正在充电", "Charging"),
        "powerProfile" to arrayOf("功耗配置", "Power profile"),
        // 存储
        "blockCount" to arrayOf("总块数", "Block count"),
        "blockSize" to arrayOf("块大小 (字节)", "Block size (bytes)"),
        "freeBytes" to arrayOf("空闲空间", "Free space"),
        "availableBytes" to arrayOf("可用空间", "Available space"),
        "totalBytes" to arrayOf("总容量", "Total capacity"),
        "usedBytes" to arrayOf("已用空间", "Used space"),
        "storageInfo" to arrayOf("内部存储", "Internal storage"),
        "rootInfo" to arrayOf("系统分区", "System partition"),
        "dataInfo" to arrayOf("数据分区", "Data partition"),
        // 位置 / 时间
        "gps" to arrayOf("GPS 已开启", "GPS enabled"),
        "country" to arrayOf("国家代码", "Country code"),
        "displayCountry" to arrayOf("国家/地区", "Country"),
        "language" to arrayOf("语言代码", "Language code"),
        "displayLanguage" to arrayOf("语言", "Language"),
        "ISO3Language" to arrayOf("ISO3 语言", "ISO3 language"),
        "timezone-ID" to arrayOf("时区 ID", "Timezone ID"),
        "timezone-DisplayName" to arrayOf("时区名称", "Timezone name"),
        "currentTimeMillis" to arrayOf("当前时间戳", "Current time (ms)"),
        "uptimeMillis" to arrayOf("运行时间(不含休眠)", "Uptime (excl. sleep)"),
        "elapsedRealtime" to arrayOf("运行时间(含休眠)", "Elapsed realtime"),
        "bootTime" to arrayOf("开机时刻", "Boot time"),
        "boot_count" to arrayOf("开机次数", "Boot count"),
        // 网络
        "networkTypeName" to arrayOf("网络类型", "Network type"),
        "networkSubTypeName" to arrayOf("网络子类型", "Network subtype"),
        "networkOperator" to arrayOf("运营商代码", "Operator code"),
        "networkCountryIso" to arrayOf("网络国家", "Network country"),
        "ip4" to arrayOf("IPv4 地址", "IPv4 address"),
        "ip6" to arrayOf("IPv6 地址", "IPv6 address"),
        "mac1" to arrayOf("MAC 地址 1", "MAC address 1"),
        "mac2" to arrayOf("MAC 地址 2", "MAC address 2"),
        "isWifi" to arrayOf("使用 WiFi", "On WiFi"),
        "linkedWifi" to arrayOf("已连接 WiFi", "Connected WiFi"),
        "bluetoothAddress" to arrayOf("蓝牙地址", "Bluetooth address"),
        "bluetoothMAC" to arrayOf("蓝牙 MAC", "Bluetooth MAC"),
        "ssid" to arrayOf("WiFi 名称", "SSID"),
        "bssid" to arrayOf("接入点 BSSID", "BSSID"),
        // GPU
        "GL_VENDOR" to arrayOf("GL 厂商", "GL vendor"),
        "GL_VERSION" to arrayOf("GL 版本", "GL version"),
        "GL_EXTENSIONS" to arrayOf("GL 扩展", "GL extensions"),
        // 标识
        "androidId" to arrayOf("Android ID", "Android ID"),
        "googleADID" to arrayOf("Google 广告 ID", "Google Ad ID"),
        "drmId" to arrayOf("DRM ID", "DRM ID"),
        "serialNo" to arrayOf("序列号", "Serial number"),
        "userAgent" to arrayOf("User-Agent", "User-Agent"),
        // USB / 输入
        "adbEnable" to arrayOf("ADB 已开启", "ADB enabled"),
        "defaultInputMethod" to arrayOf("默认输入法", "Default IME"),
        // 媒体
        "volume" to arrayOf("当前音量", "Volume"),
        "imageCount" to arrayOf("图片数量", "Image count")
    )
}
