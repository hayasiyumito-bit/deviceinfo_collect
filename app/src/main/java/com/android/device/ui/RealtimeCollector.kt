package com.android.device.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.SystemClock
import com.android.device.i18n.AppLocale
import java.io.RandomAccessFile

/**
 * 实时指标采集：电量 / 电流 / 温度、CPU 占用（/proc/stat 差值）、内存、网络上下行速率。
 * 有状态（保存上次 CPU / 流量读数以算差值），由 [RealtimeFragment] 定时调用 [read]。
 */
class RealtimeCollector(context: Context) {

    private val appContext = context.applicationContext
    private val batteryManager =
        appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val activityManager =
        appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L
    private var lastRx = TrafficStats.getTotalRxBytes()
    private var lastTx = TrafficStats.getTotalTxBytes()
    private var lastNetTime = SystemClock.elapsedRealtime()

    data class Realtime(
        val level: Int,
        val currentNow: Int,     // µA
        val temperature: Float,  // ℃
        val voltage: Int,        // mV
        val status: String,
        val charging: Boolean,
        val health: String,
        val technology: String,
        val cycleCount: Int?,    // 电池循环次数，null 表示不可读
        val cpuTemp: Float?,     // CPU 最高温度 ℃
        val gpuTemp: Float?,     // GPU 温度 ℃
        val gpuLoad: Float?,     // GPU 负载 0..1，null 表示不可读
        val gpuFreqKhz: Int?,    // GPU 当前频率(kHz)，null 表示不可读
        val cpuUsage: Float?,    // 0..1，null 表示不可读（高版本 SELinux 常挡）
        val coreCount: Int,
        val curFreqKhz: IntArray?,  // 各核当前频率(kHz)，null 表示不可读
        val maxFreqKhz: Int,        // 整机最大频率(kHz)
        val memAvail: Long,
        val memTotal: Long,
        val rxSpeed: Long,       // B/s
        val txSpeed: Long        // B/s
    )

    fun read(): Realtime {
        val battery = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val temp = (battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = battery?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val statusCode = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val healthCode = battery?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val tech = battery?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "-"
        // isCharging() 比用 status 推断更可靠（API23+）
        val charging = batteryManager.isCharging
        val cycleCount = readCycleCount(battery)
        val (cpuTemp, gpuTemp) = readThermal()

        val mem = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val (rxSpeed, txSpeed) = netSpeed()

        return Realtime(
            level = level,
            currentNow = currentNow,
            temperature = temp,
            voltage = voltage,
            status = if (charging) AppLocale.tr("充电中", "Charging") else statusText(statusCode),
            charging = charging,
            health = healthText(healthCode),
            technology = tech,
            cycleCount = cycleCount,
            cpuTemp = cpuTemp,
            gpuTemp = gpuTemp,
            gpuLoad = readGpuLoad(),
            gpuFreqKhz = readGpuFreq(),
            cpuUsage = readCpuUsage(),
            coreCount = Runtime.getRuntime().availableProcessors(),
            curFreqKhz = readCurFreqs(),
            maxFreqKhz = readMaxFreq(),
            memAvail = mem.availMem,
            memTotal = mem.totalMem,
            rxSpeed = rxSpeed,
            txSpeed = txSpeed
        )
    }

    /** 读 /proc/stat 首行，与上次快照做差值得到总 CPU 占用率。 */
    private fun readCpuUsage(): Float? {
        return try {
            RandomAccessFile("/proc/stat", "r").use { reader ->
                val line = reader.readLine() ?: return null
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 5 || parts[0] != "cpu") return null
                val nums = parts.drop(1).map { it.toLongOrNull() ?: 0L }
                val idle = nums.getOrElse(3) { 0L } + nums.getOrElse(4) { 0L }
                val total = nums.sum()
                val dTotal = total - lastCpuTotal
                val dIdle = idle - lastCpuIdle
                lastCpuTotal = total
                lastCpuIdle = idle
                if (dTotal <= 0) null else ((dTotal - dIdle).toFloat() / dTotal).coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 电池循环次数：优先 sticky intent 的 EXTRA_CYCLE_COUNT(API34+)，回落到 sysfs。 */
    private fun readCycleCount(battery: Intent?): Int? {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            val c = battery?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
            if (c > 0) return c
        }
        for (p in listOf(
            "/sys/class/power_supply/battery/cycle_count",
            "/sys/class/power_supply/bms/cycle_count"
        )) {
            readTextFile(p)?.toIntOrNull()?.let { if (it > 0) return it }
        }
        return null
    }

    /** GPU 负载：读 kgsl gpubusy("busy total"，读后重置，即上次到本次的占用)。 */
    private fun readGpuLoad(): Float? {
        val line = readTextFile("/sys/class/kgsl/kgsl-3d0/gpubusy") ?: return null
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 2) return null
        val busy = parts[0].toLongOrNull() ?: return null
        val total = parts[1].toLongOrNull() ?: return null
        return if (total > 0) (busy.toFloat() / total).coerceIn(0f, 1f) else 0f
    }

    /** GPU 当前频率(kHz)：遍历多种常见节点(Adreno/Mali)，单位归一到 kHz；全读不到返回 null。 */
    private fun readGpuFreq(): Int? {
        // (path, 原始单位→kHz 的换算)
        val candidatesHz = listOf(
            "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq", // Adreno
            "/sys/class/kgsl/kgsl-3d0/gpuclk",
            "/sys/class/devfreq/gpufreq/cur_freq",
            "/sys/class/devfreq/gpu/cur_freq",
            "/sys/class/devfreq/*.mali/cur_freq",        // Mali (通配，见下 glob 处理)
            "/sys/kernel/gpu/gpu_clock"                  // 部分为 MHz
        )
        for (p in candidatesHz) {
            val actual = if (p.contains("*")) expandGlobFirst(p) ?: continue else p
            val v = readTextFile(actual)?.toLongOrNull() ?: continue
            if (v <= 0) continue
            // >100000 视为 Hz(→/1000)，否则视为 MHz(→*1000)
            return if (v > 100_000) (v / 1000).toInt() else (v * 1000).toInt()
        }
        return null
    }

    /** 展开形如 /a/b/&#42;.mali/cur_freq 的通配路径，返回第一个存在的实际路径。 */
    private fun expandGlobFirst(pattern: String): String? {
        val star = pattern.indexOf('*')
        val dirEnd = pattern.lastIndexOf('/', star)
        val nextSlash = pattern.indexOf('/', star)
        if (dirEnd < 0 || nextSlash < 0) return null
        val dir = java.io.File(pattern.substring(0, dirEnd))
        val namePat = pattern.substring(dirEnd + 1, nextSlash) // 如 "*.mali"
        val suffix = pattern.substring(nextSlash)              // 如 "/cur_freq"
        val regex = Regex("^" + Regex.escape(namePat).replace("\\*", ".*") + "$")
        val match = try { dir.listFiles()?.firstOrNull { regex.matches(it.name) } } catch (e: Exception) { null }
        return match?.let { it.absolutePath + suffix }
    }

    /** 遍历 thermal_zone，按 type 归类取 CPU / GPU 最高温(℃)。 */
    private fun readThermal(): Pair<Float?, Float?> {
        var cpu: Float? = null
        var gpu: Float? = null
        var chip: Float? = null // 无独立 cpu 分区时的兜底（SoC/AP 芯片温度）
        for (i in 0 until 60) {
            val typePath = "/sys/class/thermal/thermal_zone$i/type"
            val type = readTextFile(typePath) ?: continue
            val t = type.lowercase()
            val isCpu = t.contains("cpu")
            val isGpu = t.contains("gpu")
            val isChip = t.contains("soc") || t.contains("tsens") || t.startsWith("ap_") || t.contains("apc")
            if (!isCpu && !isGpu && !isChip) continue
            val raw = readTextFile("/sys/class/thermal/thermal_zone$i/temp")?.toLongOrNull() ?: continue
            val celsius = if (raw > 1000) raw / 1000f else raw.toFloat()
            when {
                isGpu -> if (gpu == null || celsius > gpu!!) gpu = celsius
                isCpu -> if (cpu == null || celsius > cpu!!) cpu = celsius
                else -> if (chip == null || celsius > chip!!) chip = celsius
            }
        }
        return (cpu ?: chip) to gpu
    }

    private fun readTextFile(path: String): String? = try {
        RandomAccessFile(path, "r").use { it.readLine()?.trim() }
    } catch (e: Exception) {
        null
    }

    /** 读各核当前频率(kHz)；全部读不到返回 null。 */
    private fun readCurFreqs(): IntArray? {
        val cores = Runtime.getRuntime().availableProcessors()
        val arr = IntArray(cores) { readFreqFile("/sys/devices/system/cpu/cpu$it/cpufreq/scaling_cur_freq") }
        return if (arr.any { it > 0 }) arr else null
    }

    private fun readMaxFreq(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        var max = 0
        for (i in 0 until cores) {
            max = maxOf(max, readFreqFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq"))
        }
        return max
    }

    private fun readFreqFile(path: String): Int = try {
        RandomAccessFile(path, "r").use { it.readLine()?.trim()?.toIntOrNull() ?: 0 }
    } catch (e: Exception) {
        0
    }

    private fun netSpeed(): Pair<Long, Long> {
        val now = SystemClock.elapsedRealtime()
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val dt = (now - lastNetTime).coerceAtLeast(1)
        val rxSpeed = ((rx - lastRx) * 1000 / dt).coerceAtLeast(0)
        val txSpeed = ((tx - lastTx) * 1000 / dt).coerceAtLeast(0)
        lastRx = rx
        lastTx = tx
        lastNetTime = now
        return rxSpeed to txSpeed
    }

    private fun statusText(code: Int): String = when (code) {
        BatteryManager.BATTERY_STATUS_CHARGING -> AppLocale.tr("充电中", "Charging")
        BatteryManager.BATTERY_STATUS_DISCHARGING -> AppLocale.tr("放电中", "Discharging")
        BatteryManager.BATTERY_STATUS_FULL -> AppLocale.tr("已充满", "Full")
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> AppLocale.tr("未充电", "Not charging")
        else -> AppLocale.tr("未知", "Unknown")
    }

    private fun healthText(code: Int): String = when (code) {
        BatteryManager.BATTERY_HEALTH_GOOD -> AppLocale.tr("良好", "Good")
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> AppLocale.tr("过热", "Overheat")
        BatteryManager.BATTERY_HEALTH_DEAD -> AppLocale.tr("损坏", "Dead")
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> AppLocale.tr("过压", "Over voltage")
        BatteryManager.BATTERY_HEALTH_COLD -> AppLocale.tr("过冷", "Cold")
        else -> AppLocale.tr("未知", "Unknown")
    }
}
