package com.steptrack.pro.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.steptrack.pro.R

/** Ring that fills clockwise from the top. Progress is a value from 0 to 1. */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokePx = resources.displayMetrics.density * 14f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        color = ContextCompat.getColor(context, R.color.st_card)
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.st_primary)
    }

    private val oval = RectF()
    private var shown = 0f
    private var animator: ValueAnimator? = null

    fun setProgress(target: Float) {
        val to = target.coerceIn(0f, 1f)
        animator?.cancel()
        animator = ValueAnimator.ofFloat(shown, to).apply {
            duration = 600L
            addUpdateListener {
                shown = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val half = strokePx / 2f
        oval.set(half, half, w - half, h - half)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawOval(oval, trackPaint)
        if (shown > 0f) {
            canvas.drawArc(oval, -90f, 360f * shown, false, arcPaint)
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
