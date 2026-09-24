package com.android.device.report

import android.content.Context
import com.android.device.BuildConfig
import com.android.device.sdk.DeviceCollectSdk
import com.android.device.sdk.ReportConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 采集 App 侧的设备参数后台静默上报入口。
 *
 * 与 YumyHook 管理器复用同一 [DeviceCollectSdk] 与同一服务端，但以 `source = "collect_app"`
 * 标记来源：采集 App 处于 YumyHook 的 Hook 作用域内，采集到的是**伪装后**的设备值，
 * 服务端据此与管理器上报的真实值区分对照。
 *
 * 纯后台、静态上报，不关心返回值（服务端返回 204 空响应）。每个进程仅触发一次。
 */
object DeviceReportManager {

    /** 采集 App 的来源标识。 */
    private const val SOURCE = "collect_app"

    /** gzip 压缩请求体（快照约 300KB）。 */
    private const val GZIP = true

    /**
     * 是否在自动上报里包含风控检测。**必须为 false**：风控检测会密集 fork 子进程
     * (app_process/su/shell)，在后台线程静默执行会触发 Android 12+ phantom process killer
     * 把本进程 SIGKILL（表现为闪退）。仅采集设备快照。
     */
    private const val INCLUDE_RISK = false

    private val started = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "device-report").apply { isDaemon = true }
    }

    private val enabled: Boolean
        get() = BuildConfig.REPORT_BASE_URL.isNotEmpty() && BuildConfig.REPORT_API_KEY.isNotEmpty()

    /** 进程内仅执行一次的异步采集上报；主线程调用安全（立即返回）。 */
    @JvmStatic
    fun reportOnceAsync(context: Context) {
        if (!enabled) return
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        executor.execute {
            try {
                // 延后到冷启动窗口之后再采集，避免与启动争资源
                Thread.sleep(4_000L)
                val cfg = ReportConfig(BuildConfig.REPORT_BASE_URL, BuildConfig.REPORT_API_KEY)
                    .appPackage(BuildConfig.APPLICATION_ID)
                    .appVersion(BuildConfig.VERSION_NAME)
                    .source(SOURCE)
                    .gzip(GZIP)
                    .includeRisk(INCLUDE_RISK)
                // 后台静默上报：成功/失败/超时都不打日志（与 YumyHook 上报逻辑一致，仅 source 参数区分来源）。
                DeviceCollectSdk.collectAndReport(appContext, cfg)
            } catch (_: Throwable) {
                // 静默：采集/上报异常不打日志、不影响 App。
            }
        }
    }
}
