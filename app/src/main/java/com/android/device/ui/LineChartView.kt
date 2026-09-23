package com.android.device.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.abs

/**
 * 轻量实时折线图（自绘 View，无第三方依赖）。
 *
 * 每个系列一条彩色折线，共享 Y 轴（同单位），随时间从右向左滚动。
 * 顶部图例显示各系列名 + 当前值。因测试机系统动画被关，走自绘 invalidate 刷新（同 [SpinnerView]）。
 */
class LineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private class Series(val key: String, val label: String, val color: Int) {
        val points = ArrayDeque<Float>() // NaN 表示该时刻不可读（断点）
        var latest: Float = Float.NaN
    }

    private val series = LinkedHashMap<String, Series>()
    private var capacity = 120
    private var unit: String = ""
    private var formatter: (Float) -> String = { "%.1f".format(it) }

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(0.75f)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2f)
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = dp(10f) }
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = dp(12f) }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = dp(12f); textAlign = Paint.Align.CENTER }

    private val path = Path()
    private val fillPath = Path()

    private val onSurface: Int by lazy { themeColor(android.R.attr.textColorPrimary, Color.DKGRAY) }
    private val onSurfaceVar: Int by lazy { themeColor(android.R.attr.textColorSecondary, Color.GRAY) }

    private fun themeColor(attr: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) resources.getColor(tv.resourceId, context.theme) else tv.data
        } else fallback
    }

    /** specs: (key, 显示名, 颜色)。unit 为 Y 轴单位串（如 "℃" / "MHz"）。 */
    fun configure(
        specs: List<Triple<String, String, Int>>,
        unit: String,
        capacity: Int = 120,
        formatter: ((Float) -> String)? = null,
    ) {
        series.clear()
        specs.forEach { series[it.first] = Series(it.first, it.second, it.third) }
        this.unit = unit
        this.capacity = capacity
        formatter?.let { this.formatter = it }
        invalidate()
    }

    /** 追加一帧：每个系列取 values[key]，null=断点(NaN)。 */
    fun push(values: Map<String, Float?>) {
        for ((k, s) in series) {
            val v = values[k]
            s.points.addLast(v ?: Float.NaN)
            while (s.points.size > capacity) s.points.removeFirst()
            if (v != null) s.latest = v
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        gridPaint.color = (onSurfaceVar and 0x00FFFFFF) or 0x30000000
        axisTextPaint.color = onSurfaceVar
        legendPaint.color = onSurface
        hintPaint.color = onSurfaceVar

        val legendH = dp(20f)
        val padTop = legendH + dp(6f)
        val padBottom = dp(6f)
        val padRight = dp(6f)
        val padLeft = dp(34f) // 给 Y 轴数值留位

        // 图例（顶部一行）
        var lx = padLeft
        val ly = dp(13f)
        for (s in series.values) {
            dotPaint.color = s.color
            canvas.drawCircle(lx + dp(4f), ly - dp(3f), dp(4f), dotPaint)
            val txt = s.label + (if (!s.latest.isNaN()) "  " + formatter(s.latest) + unit else "  --")
            canvas.drawText(txt, lx + dp(12f), ly, legendPaint)
            lx += dp(12f) + legendPaint.measureText(txt) + dp(14f)
        }

        val plotL = padLeft
        val plotT = padTop
        val plotR = w - padRight
        val plotB = h - padBottom
        val plotW = plotR - plotL
        val plotH = plotB - plotT

        // Y 值域：所有系列所有有限点的 min/max
        var minV = Float.MAX_VALUE
        var maxV = -Float.MAX_VALUE
        for (s in series.values) for (v in s.points) if (!v.isNaN()) {
            if (v < minV) minV = v; if (v > maxV) maxV = v
        }
        if (minV > maxV) { // 还没有数据
            canvas.drawText("采集中…", w / 2f, h / 2f, hintPaint)
            return
        }
        if (abs(maxV - minV) < 1e-3f) { minV -= 1f; maxV += 1f }
        val range = (maxV - minV)
        val padV = range * 0.12f
        val lo = minV - padV
        val hi = maxV + padV
        val span = (hi - lo).coerceAtLeast(1e-3f)

        fun yOf(v: Float) = plotB - (v - lo) / span * plotH

        // 网格 + Y 轴刻度（4 段 5 线）
        val lines = 4
        for (i in 0..lines) {
            val y = plotT + plotH * i / lines
            canvas.drawLine(plotL, y, plotR, y, gridPaint)
            val value = hi - span * i / lines
            canvas.drawText(formatter(value), dp(2f), y + dp(3.5f), axisTextPaint)
        }

        // 每个系列：面积(低透明) + 折线 + 末端点
        for (s in series.values) {
            val n = s.points.size
            if (n == 0) continue
            path.reset(); fillPath.reset()
            var started = false
            var lastX = 0f
            var idx = 0
            for (v in s.points) {
                val x = if (capacity <= 1) plotR else plotL + plotW * idx / (capacity - 1)
                idx++
                if (v.isNaN()) { started = false; continue }
                val y = yOf(v)
                if (!started) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, plotB); fillPath.lineTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y); fillPath.lineTo(x, y)
                }
                lastX = x
            }
            if (started) {
                fillPath.lineTo(lastX, plotB); fillPath.close()
                fillPaint.color = (s.color and 0x00FFFFFF) or 0x1F000000
                canvas.drawPath(fillPath, fillPaint)
            }
            linePaint.color = s.color
            canvas.drawPath(path, linePaint)
            if (!s.latest.isNaN() && n > 0) {
                dotPaint.color = s.color
                canvas.drawCircle(lastX, yOf(s.latest), dp(3f), dotPaint)
            }
        }
    }
}
