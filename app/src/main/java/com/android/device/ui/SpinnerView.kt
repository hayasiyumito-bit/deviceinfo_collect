package com.android.device.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.min

/**
 * 自绘旋转加载环。用 Handler 手动推进角度，**不依赖系统动画缩放**——
 * 因此在开发者选项关闭动画（animator_duration_scale=0）的设备上仍能转动，
 * 避免静态圈让人误以为卡死。
 */
class SpinnerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(5f)
        color = resolveColor()
    }
    private val oval = RectF()
    private var angle = 0f
    private val step = object : Runnable {
        override fun run() {
            angle = (angle + 13f) % 360f
            invalidate()
            postDelayed(this, 16L)
        }
    }

    private fun start() { removeCallbacks(step); post(step) }
    private fun stop() { removeCallbacks(step) }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && isAttachedToWindow) start() else stop()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE) start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = paint.strokeWidth
        val r = min(width, height) / 2f - pad
        val cx = width / 2f
        val cy = height / 2f
        oval.set(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, angle, 270f, false, paint)
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    private fun resolveColor(): Int {
        val tv = TypedValue()
        val attr = com.google.android.material.R.attr.colorPrimary
        return if (context.theme.resolveAttribute(attr, tv, true)) tv.data else 0xFF1565C0.toInt()
    }
}
