package com.android.device.ui

import android.content.Context
import com.android.device.i18n.AppLocale
import org.json.JSONArray
import org.json.JSONObject

/**
 * 把 hardware.cpuInfo / hardware.camera / sensor 这类原始块解析成结构化、可读的卡片。
 * 全部中英双语（[AppLocale.tr]）。解析不到的字段跳过，不强求。
 */
object HardwareParser {

    /** 解析 hardware 块 → CPU 卡 + 摄像头卡 + 其余硬件字段卡。 */
    fun parseHardware(context: Context, hw: JSONObject): List<CardItem> {
        val cards = mutableListOf<CardItem>()

        hw.optString("cpuInfo").takeIf { it.isNotBlank() }?.let { cpuCard(it)?.let(cards::add) }
        hw.optJSONArray("camera")?.let { cameraCard(it)?.let(cards::add) }

        // 其余硬件字段（排除已单独解析的），走通用展开
        val rows = mutableListOf<CardItem.Row>()
        val keys = hw.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k == "cpuInfo" || k == "camera") continue
            val v = hw.opt(k)
            val text = when (v) {
                is JSONObject, is JSONArray -> null // 复杂对象放详情
                else -> v?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
            }
            if (text != null) {
                if (text.length > 60) rows.add(detail(label(k), text)) else rows.add(CardItem.Row(label(k), text, text, k))
            } else if (v != null) {
                rows.add(detail(label(k), v.toString()))
            }
        }
        if (rows.isNotEmpty()) cards.add(CardItem.Body(AppLocale.tr("其他硬件", "Other hardware"), rows))
        return cards
    }

    private fun cpuCard(cpuInfo: String): CardItem? {
        val lines = cpuInfo.split("\n")
        fun field(prefix: String): String? =
            lines.firstOrNull { it.trimStart().startsWith(prefix) }?.substringAfter(":")?.trim()?.takeIf { it.isNotEmpty() }

        val cores = lines.count { it.trimStart().startsWith("processor") }
        val rows = mutableListOf<CardItem.Row>()
        if (cores > 0) rows.add(row(AppLocale.tr("CPU 核心数", "CPU cores"), cores.toString()))
        field("Hardware")?.let { rows.add(row(AppLocale.tr("芯片", "Chipset"), it)) }
        field("CPU implementer")?.let {
            rows.add(row(AppLocale.tr("CPU 厂商", "CPU vendor"), implementer(it)))
        }
        field("CPU architecture")?.let { rows.add(row(AppLocale.tr("CPU 架构", "CPU architecture"), "ARMv$it")) }
        field("Features")?.let {
            val label = AppLocale.tr("指令集特性", "Instruction set")
            rows.add(if (it.length > 60) detail(label, it) else row(label, it))
        }
        if (rows.isEmpty()) return null
        return CardItem.Body(AppLocale.tr("CPU 信息", "CPU"), rows)
    }

    private fun cameraCard(cams: JSONArray): CardItem? {
        val rows = mutableListOf<CardItem.Row>()
        rows.add(row(AppLocale.tr("摄像头数量", "Camera count"), cams.length().toString()))
        for (i in 0 until cams.length()) {
            val cam = cams.optJSONObject(i) ?: continue
            val id = cam.optString("cameraId", i.toString())
            val prefix = AppLocale.tr("摄像头 $id", "Camera $id")
            // 最大分辨率 → 像素(MP)
            val sizes = cam.optJSONArray("sizeInfo")
            var maxW = 0
            var maxH = 0
            if (sizes != null) {
                for (j in 0 until sizes.length()) {
                    val s = sizes.optJSONObject(j) ?: continue
                    val w = s.optInt("width")
                    val h = s.optInt("height")
                    if (w.toLong() * h > maxW.toLong() * maxH) {
                        maxW = w; maxH = h
                    }
                }
            }
            if (maxW > 0) {
                val mp = maxW.toLong() * maxH / 1_000_000.0
                rows.add(row("$prefix · ${AppLocale.tr("最高分辨率", "Max resolution")}", "%d×%d (%.1f MP)".format(maxW, maxH, mp)))
            }
            val info = cam.optJSONArray("info")?.optJSONObject(0)
            info?.optString("focalLength")?.takeIf { it.isNotBlank() }
                ?.let { rows.add(row("$prefix · ${AppLocale.tr("焦距", "Focal length")}", "$it mm")) }
            info?.optString("horizonalAngle")?.takeIf { it.isNotBlank() }
                ?.let { rows.add(row("$prefix · ${AppLocale.tr("水平视场", "Horizontal FOV")}", "%.1f°".format(it.toFloatOrNull() ?: 0f))) }
        }
        if (rows.size <= 1) return null
        return CardItem.Body(AppLocale.tr("摄像头", "Camera"), rows)
    }

    /** 解析 sensor 块 → 每个传感器一行(名称 → 功耗)，点击看完整参数。 */
    fun parseSensors(context: Context, sensorObj: JSONObject): CardItem {
        val rows = mutableListOf<CardItem.Row>()
        val keys = sensorObj.keys()
        while (keys.hasNext()) {
            val name = keys.next()
            val raw = sensorObj.optString(name)
            val power = field(raw, "power")
            val resolution = field(raw, "resolution")
            val maxRange = field(raw, "maxRange")
            val vendor = quoted(raw, "vendor")
            // 主展示：功耗；详情：完整参数
            val value = if (power != null) "%s mA".format(power) else AppLocale.tr("查看详情 ›", "View detail ›")
            val detailText = buildString {
                if (vendor != null) append(AppLocale.tr("厂商", "Vendor")).append(": ").append(vendor).append("\n")
                if (power != null) append(AppLocale.tr("功耗", "Power")).append(": ").append(power).append(" mA\n")
                if (resolution != null) append(AppLocale.tr("分辨率", "Resolution")).append(": ").append(resolution).append("\n")
                if (maxRange != null) append(AppLocale.tr("量程", "Max range")).append(": ").append(maxRange)
            }.ifBlank { raw }
            rows.add(CardItem.Row(name, value, detailText, "__detail__"))
        }
        return CardItem.Body(AppLocale.tr("传感器详情", "Sensor details"), rows)
    }

    // ---- helpers ----

    private fun row(label: String, value: String) = CardItem.Row(label, value, value)
    private fun detail(label: String, full: String) =
        CardItem.Row(label, AppLocale.tr("查看详情 ›", "View detail ›"), full, "__detail__")

    private fun field(s: String, key: String): String? =
        Regex("$key=([0-9.]+)").find(s)?.groupValues?.getOrNull(1)

    private fun quoted(s: String, key: String): String? =
        Regex("$key=\"([^\"]*)\"").find(s)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }

    private fun implementer(hex: String): String = when (hex.lowercase()) {
        "0x41" -> "ARM"
        "0x51" -> "Qualcomm"
        "0x61" -> "Apple"
        "0x48" -> "HiSilicon"
        "0x53" -> "Samsung"
        "0x69" -> "Intel"
        "0x4e" -> "NVIDIA"
        "0x4d" -> "Motorola"
        else -> hex
    }

    private fun label(key: String): String =
        com.android.device.DeviceInfoParser.translateKey(key).let { if (it.isNotEmpty() && it != key) it else key }
}
