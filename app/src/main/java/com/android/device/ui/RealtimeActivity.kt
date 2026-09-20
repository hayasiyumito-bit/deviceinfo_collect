package com.android.device.ui

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.device.R
import com.android.device.databinding.ActivityRealtimeBinding
import com.android.device.i18n.AppLocale
import java.util.concurrent.Executors

/**
 * 独立的实时监测页：电量 / CPU / GPU / 内存 / 网速定时轮询，GNSS 卫星星图、实时定位、
 * 传感器走系统回调。**所有监听都在 onResume 开启、onPause 关闭**（onDestroy 兜底），
 * 采集的文件 IO 放后台线程，避免跨页面泄漏与主线程 ANR。
 */
class RealtimeActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityRealtimeBinding
    private lateinit var collector: RealtimeCollector
    private val handler = Handler(Looper.getMainLooper())
    private val bgExecutor = Executors.newSingleThreadExecutor()
    private var polling = false

    private var locationManager: LocationManager? = null
    private lateinit var sensorManager: SensorManager
    private val sensorValueViews = LinkedHashMap<Int, TextView>()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startLocation() else showPermHint() }

    // 轮询：读取放后台线程(含 sysfs/proc 文件 IO)，回主线程更新，避免主线程 ANR。
    private val poll = Runnable { tick() }

    private fun tick() {
        bgExecutor.execute {
            val rt = try {
                collector.read()
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                if (rt != null && !isDestroyed) updateRealtime(rt)
                if (polling) handler.postDelayed(poll, INTERVAL)
            }
        }
    }

    private val locationListener = LocationListener { loc -> updateLocation(loc) }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val list = ArrayList<SatelliteView.Sat>(status.satelliteCount)
            var used = 0
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) used++
                list.add(
                    SatelliteView.Sat(
                        azimuth = status.getAzimuthDegrees(i),
                        elevation = status.getElevationDegrees(i),
                        snr = status.getCn0DbHz(i),
                        used = status.usedInFix(i),
                        constellation = status.getConstellationType(i)
                    )
                )
            }
            binding.satelliteView.setSatellites(list)
            binding.tvSatSummary.text = getString(R.string.rt_sat_summary, used, status.satelliteCount)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        AppLocale.applyFromCache(this)
        super.onCreate(savedInstanceState)
        binding = ActivityRealtimeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applySystemBars(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        collector = RealtimeCollector(this)
        locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        binding.btnGrant.setOnClickListener {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // 进入页面：开启全部监听
    override fun onResume() {
        super.onResume()
        polling = true
        handler.post(poll)
        startSensors()
        if (hasLocationPermission()) startLocation() else showPermHint()
    }

    // 离开页面：关闭全部监听，避免跨页面泄漏 / 后台耗电
    override fun onPause() {
        super.onPause()
        polling = false
        handler.removeCallbacks(poll)
        stopLocation()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 兜底：彻底清空 Handler 任务、监听与后台线程
        polling = false
        handler.removeCallbacksAndMessages(null)
        stopLocation()
        sensorManager.unregisterListener(this)
        bgExecutor.shutdownNow()
    }

    // ---- 传感器实时 ----

    private val monitoredSensors = listOf(
        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR,
        Sensor.TYPE_LIGHT, Sensor.TYPE_PROXIMITY, Sensor.TYPE_PRESSURE,
        Sensor.TYPE_AMBIENT_TEMPERATURE, Sensor.TYPE_RELATIVE_HUMIDITY, Sensor.TYPE_STEP_COUNTER
    )

    private fun startSensors() {
        binding.sensorDetail.removeAllViews()
        sensorValueViews.clear()
        val inflater = LayoutInflater.from(this)
        for (type in monitoredSensors) {
            val sensor = sensorManager.getDefaultSensor(type) ?: continue
            val row = inflater.inflate(R.layout.item_info_row, binding.sensorDetail, false)
            row.findViewById<TextView>(R.id.row_label).text = sensorLabel(type)
            val valueView = row.findViewById<TextView>(R.id.row_value).apply { text = "—" }
            binding.sensorDetail.addView(row)
            sensorValueViews[type] = valueView
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        sensorValueViews[event.sensor.type]?.text = formatSensor(event.sensor.type, event.values)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun sensorLabel(type: Int): String = when (type) {
        Sensor.TYPE_ACCELEROMETER -> AppLocale.tr("加速度计", "Accelerometer")
        Sensor.TYPE_GYROSCOPE -> AppLocale.tr("陀螺仪", "Gyroscope")
        Sensor.TYPE_MAGNETIC_FIELD -> AppLocale.tr("磁力计", "Magnetometer")
        Sensor.TYPE_GRAVITY -> AppLocale.tr("重力", "Gravity")
        Sensor.TYPE_LINEAR_ACCELERATION -> AppLocale.tr("线性加速度", "Linear accel")
        Sensor.TYPE_ROTATION_VECTOR -> AppLocale.tr("旋转矢量", "Rotation vec")
        Sensor.TYPE_LIGHT -> AppLocale.tr("光线", "Light")
        Sensor.TYPE_PROXIMITY -> AppLocale.tr("距离", "Proximity")
        Sensor.TYPE_PRESSURE -> AppLocale.tr("气压", "Pressure")
        Sensor.TYPE_AMBIENT_TEMPERATURE -> AppLocale.tr("环境温度", "Ambient temp")
        Sensor.TYPE_RELATIVE_HUMIDITY -> AppLocale.tr("相对湿度", "Humidity")
        Sensor.TYPE_STEP_COUNTER -> AppLocale.tr("计步", "Steps")
        else -> "#$type"
    }

    private fun formatSensor(type: Int, v: FloatArray): String = when (type) {
        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION ->
            "%.2f, %.2f, %.2f m/s²".format(v[0], v[1], v.getOrElse(2) { 0f })
        Sensor.TYPE_GYROSCOPE ->
            "%.2f, %.2f, %.2f rad/s".format(v[0], v[1], v.getOrElse(2) { 0f })
        Sensor.TYPE_MAGNETIC_FIELD ->
            "%.1f, %.1f, %.1f µT".format(v[0], v[1], v.getOrElse(2) { 0f })
        Sensor.TYPE_ROTATION_VECTOR ->
            "%.2f, %.2f, %.2f".format(v[0], v[1], v.getOrElse(2) { 0f })
        Sensor.TYPE_LIGHT -> "%.0f lx".format(v[0])
        Sensor.TYPE_PROXIMITY -> "%.0f cm".format(v[0])
        Sensor.TYPE_PRESSURE -> "%.1f hPa".format(v[0])
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "%.1f ℃".format(v[0])
        Sensor.TYPE_RELATIVE_HUMIDITY -> "%.0f %%".format(v[0])
        Sensor.TYPE_STEP_COUNTER -> "%.0f".format(v[0])
        else -> v.joinToString(", ") { "%.2f".format(it) }
    }

    private fun updateRealtime(rt: RealtimeCollector.Realtime) {
        binding.tvBatteryLevel.text = "${rt.level}%"
        binding.pbBattery.setProgressCompat(rt.level.coerceIn(0, 100), true)
        val batteryRows = mutableListOf(
            getString(R.string.rt_status) to rt.status,
            getString(R.string.rt_current) to "${rt.currentNow} µA",
            getString(R.string.rt_temp) to "%.1f ℃".format(rt.temperature),
            getString(R.string.rt_voltage) to "${rt.voltage} mV",
            getString(R.string.rt_health) to rt.health,
            getString(R.string.rt_tech) to rt.technology
        )
        rt.cycleCount?.let { batteryRows.add(getString(R.string.rt_cycle) to it.toString()) }
        fillRows(binding.batteryDetail, batteryRows)

        // GPU 卡：负载 + 温度
        if (rt.gpuLoad != null) {
            val gpct = (rt.gpuLoad * 100).toInt()
            binding.tvGpu.text = "$gpct%"
            binding.pbGpu.setProgressCompat(gpct, true)
        } else {
            binding.tvGpu.text = getString(R.string.rt_unavailable)
            binding.pbGpu.setProgressCompat(0, true)
        }
        val gpuRows = mutableListOf<Pair<String, String>>()
        rt.gpuTemp?.let { gpuRows.add(getString(R.string.rt_gpu_temp) to "%.1f ℃".format(it)) }
        fillRows(binding.thermalDetail, gpuRows)

        // 更新时间戳，体现实时性
        binding.toolbar.subtitle = getString(
            R.string.rt_updated_at,
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        )

        val freqs = rt.curFreqKhz
        when {
            rt.cpuUsage != null -> {
                val pct = (rt.cpuUsage * 100).toInt()
                binding.tvCpu.text = "$pct%"
                binding.pbCpu.setProgressCompat(pct, true)
                binding.cpuDetail.removeAllViews()
            }
            freqs != null -> {
                val maxCur = freqs.max()
                binding.tvCpu.text = "%.2f GHz".format(maxCur / 1_000_000.0)
                binding.pbCpu.setProgressCompat(
                    if (rt.maxFreqKhz > 0) (maxCur * 100 / rt.maxFreqKhz) else 0, true
                )
                val cpuRows = mutableListOf<Pair<String, String>>()
                rt.cpuTemp?.let { cpuRows.add(getString(R.string.rt_cpu_temp) to "%.1f ℃".format(it)) }
                cpuRows.addAll(freqs.mapIndexed { i, f ->
                    AppLocale.tr("核 $i", "Core $i") to
                        if (f > 0) "%,d MHz".format(f / 1000) else "-"
                })
                fillRows(binding.cpuDetail, cpuRows)
            }
            else -> {
                binding.tvCpu.text = getString(R.string.rt_cores, rt.coreCount)
                binding.pbCpu.setProgressCompat(0, true)
                binding.cpuDetail.removeAllViews()
            }
        }

        val usedGb = (rt.memTotal - rt.memAvail) / 1.073741824e9
        val totalGb = rt.memTotal / 1.073741824e9
        binding.tvMem.text = "%.1f / %.1f GB".format(usedGb, totalGb)
        binding.pbMem.setProgressCompat(
            if (rt.memTotal > 0) ((rt.memTotal - rt.memAvail) * 100 / rt.memTotal).toInt() else 0, true
        )

        binding.tvDownload.text = "↓ " + fmtSpeed(rt.rxSpeed)
        binding.tvUpload.text = "↑ " + fmtSpeed(rt.txSpeed)
    }

    private fun updateLocation(loc: Location) {
        binding.tvPermHint.visibility = View.GONE
        binding.btnGrant.visibility = View.GONE
        fillRows(
            binding.locationDetail, listOf(
                getString(R.string.rt_latitude) to "%.6f".format(loc.latitude),
                getString(R.string.rt_longitude) to "%.6f".format(loc.longitude),
                getString(R.string.rt_accuracy) to "%.1f m".format(loc.accuracy),
                getString(R.string.rt_altitude) to "%.1f m".format(loc.altitude),
                getString(R.string.rt_speed) to "%.1f m/s".format(loc.speed),
                getString(R.string.rt_bearing) to "%.0f°".format(loc.bearing),
                getString(R.string.rt_provider) to (loc.provider ?: "-")
            )
        )
    }

    private fun startLocation() {
        if (!hasLocationPermission()) return
        binding.tvPermHint.visibility = View.GONE
        binding.btnGrant.visibility = View.GONE
        val lm = locationManager ?: return
        try {
            for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (lm.isProviderEnabled(provider)) {
                    lm.requestLocationUpdates(provider, 1000L, 0f, locationListener)
                }
            }
            lm.registerGnssStatusCallback(gnssCallback, handler)
            if (binding.locationDetail.childCount == 0) {
                fillRows(binding.locationDetail, listOf(getString(R.string.rt_waiting) to ""))
            }
        } catch (_: SecurityException) {
        }
    }

    private fun stopLocation() {
        val lm = locationManager ?: return
        try {
            lm.removeUpdates(locationListener)
            lm.unregisterGnssStatusCallback(gnssCallback)
        } catch (_: Exception) {
        }
    }

    private fun showPermHint() {
        binding.tvPermHint.visibility = View.VISIBLE
        binding.btnGrant.visibility = View.VISIBLE
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun fillRows(container: LinearLayout, pairs: List<Pair<String, String>>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        for ((label, value) in pairs) {
            val row = inflater.inflate(R.layout.item_info_row, container, false)
            row.findViewById<TextView>(R.id.row_label).text = label
            row.findViewById<TextView>(R.id.row_value).text = value
            container.addView(row)
        }
    }

    private fun fmtSpeed(bps: Long): String = when {
        bps >= 1048576 -> "%.2f MB/s".format(bps / 1048576.0)
        bps >= 1024 -> "%.1f KB/s".format(bps / 1024.0)
        else -> "$bps B/s"
    }

    private companion object {
        const val INTERVAL = 1500L
    }
}
