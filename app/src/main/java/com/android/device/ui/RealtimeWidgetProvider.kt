package com.android.device.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.android.device.R
import com.android.device.i18n.AppLocale

/**
 * 桌面小组件：展示实时电量 / CPU / GPU / 内存概要。
 * 声明后可从桌面「长按 → 小组件」列表添加。系统按 updatePeriodMillis 定时刷新，
 * 点击 ↻ 手动刷新，点击组件主体打开应用的实时监测页。
 */
class RealtimeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateOne(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, RealtimeWidgetProvider::class.java))
            for (id in ids) updateOne(context, mgr, id)
        }
    }

    private fun updateOne(context: Context, mgr: AppWidgetManager, id: Int) {
        AppLocale.applyFromCache(context)
        val rt = RealtimeCollector(context).read()
        val views = RemoteViews(context.packageName, R.layout.widget_realtime)

        views.setTextViewText(R.id.w_battery, "🔋 ${rt.level}%  ·  ${rt.status}")
        val cpu = rt.curFreqKhz?.maxOrNull()?.let { "%.2f GHz".format(it / 1_000_000.0) }
            ?: context.getString(R.string.rt_unavailable)
        val cpuTemp = rt.cpuTemp?.let { "  %.0f℃".format(it) } ?: ""
        views.setTextViewText(R.id.w_cpu, "⚙ CPU  $cpu$cpuTemp")
        val gpu = rt.gpuLoad?.let { "${(it * 100).toInt()}%" } ?: context.getString(R.string.rt_unavailable)
        val gpuTemp = rt.gpuTemp?.let { "  %.0f℃".format(it) } ?: ""
        views.setTextViewText(R.id.w_gpu, "🎮 GPU  $gpu$gpuTemp")
        val usedGb = (rt.memTotal - rt.memAvail) / 1.073741824e9
        val totalGb = rt.memTotal / 1.073741824e9
        views.setTextViewText(R.id.w_mem, "▦ %s  %.1f/%.1f GB".format(context.getString(R.string.rt_memory), usedGb, totalGb))
        views.setTextViewText(
            R.id.w_time, context.getString(
                R.string.rt_updated_at,
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            )
        )

        // ↻ 手动刷新
        val refresh = Intent(context, RealtimeWidgetProvider::class.java).setAction(ACTION_REFRESH)
        views.setOnClickPendingIntent(
            R.id.w_refresh,
            PendingIntent.getBroadcast(context, 0, refresh, piFlags())
        )
        // 点击主体打开实时页
        val open = Intent(context, RealtimeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        views.setOnClickPendingIntent(
            R.id.w_title,
            PendingIntent.getActivity(context, 1, open, piFlags())
        )

        mgr.updateAppWidget(id, views)
    }

    private fun piFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private companion object {
        const val ACTION_REFRESH = "com.android.device.widget.REFRESH"
    }
}
