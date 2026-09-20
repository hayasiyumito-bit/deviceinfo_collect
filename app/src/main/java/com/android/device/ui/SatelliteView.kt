package com.android.device.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * GNSS 卫星星图：极坐标绘制。圆心为天顶(仰角90°)，外圈为地平线(仰角0°)，
 * 正上方为正北。卫星点半径按仰角、角度按方位角，颜色按信噪比(C/N0)。
 */
class SatelliteView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Sat(
        val azimuth: Float,
        val elevation: Float,
        val snr: Float,
        val used: Boolean,
        val constellation: Int = 0
    )

    private var sats: List<Sat> = emptyList()

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#80888888")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA888888")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    fun setSatellites(list: List<Sat>) {
        sats = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) - 40f

        // 同心圈：仰角 0 / 30 / 60
        canvas.drawCircle(cx, cy, radius, gridPaint)
        canvas.drawCircle(cx, cy, radius * 2 / 3, gridPaint)
        canvas.drawCircle(cx, cy, radius / 3, gridPaint)
        // 十字方位线
        canvas.drawLine(cx, cy - radius, cx, cy + radius, gridPaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, gridPaint)
        // 方位标注
        canvas.drawText("N", cx, cy - radius - 8f, textPaint)
        canvas.drawText("S", cx, cy + radius + 30f, textPaint)
        canvas.drawText("E", cx + radius + 22f, cy + 10f, textPaint)
        canvas.drawText("W", cx - radius - 22f, cy + 10f, textPaint)

        for (s in sats) {
            val r = radius * (1f - (s.elevation.coerceIn(0f, 90f) / 90f))
            val rad = Math.toRadians((s.azimuth - 90f).toDouble())
            val x = cx + r * cos(rad).toFloat()
            val y = cy + r * sin(rad).toFloat()
            dotPaint.color = constellationColor(s.constellation)
            // 已用于定位的卫星更大更实；仅可见的略小
            val dotR = if (s.used) 17f else 12f
            dotPaint.alpha = if (s.used) 255 else 170
            canvas.drawCircle(x, y, dotR, dotPaint)
            if (s.used) canvas.drawCircle(x, y, dotR, dotStroke)
        }
    }

    companion object {
        /** 按 GNSS 星座着色（与图例一致）。GnssStatus.CONSTELLATION_* 取值。 */
        fun constellationColor(c: Int): Int = when (c) {
            1 -> Color.parseColor("#1565C0") // GPS 蓝
            3 -> Color.parseColor("#E53935") // GLONASS 红
            5 -> Color.parseColor("#FB8C00") // BeiDou 北斗 橙
            6 -> Color.parseColor("#2E7D32") // Galileo 绿
            4 -> Color.parseColor("#8E24AA") // QZSS 紫
            2 -> Color.parseColor("#00838F") // SBAS 青
            7 -> Color.parseColor("#6D4C41") // IRNSS 棕
            else -> Color.parseColor("#9E9E9E")
        }
    }
}
