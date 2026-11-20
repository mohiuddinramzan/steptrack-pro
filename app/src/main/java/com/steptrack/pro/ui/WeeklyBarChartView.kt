package com.steptrack.pro.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.steptrack.pro.R
import com.steptrack.pro.utils.DayBar

/** Simple bar chart: one bar per day, today highlighted, thin line marking the daily goal. */
class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bars: List<DayBar> = emptyList()
    private var goal: Int = 0

    private fun dp(value: Float) = value * resources.displayMetrics.density

    private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.st_primary)
    }
    private val pastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.st_secondary)
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.st_background)
    }
    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.st_accent)
        strokeWidth = dp(1f)
        alpha = 160
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.st_muted)
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics
        )
    }

    fun setData(newBars: List<DayBar>, dailyGoal: Int) {
        bars = newBars
        goal = dailyGoal
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return

        val labelSpace = textPaint.textSize * 1.8f
        val top = paddingTop + dp(8f)
        val chartBottom = height - paddingBottom - labelSpace
        val chartHeight = (chartBottom - top).coerceAtLeast(1f)
        val maxValue = maxOf(goal, bars.maxOf { it.steps }, 1)

        val slot = (width - paddingLeft - paddingRight).toFloat() / bars.size
        val barWidth = slot * 0.5f
        val radius = dp(6f)

        if (goal > 0) {
            val goalY = chartBottom - chartHeight * (goal.toFloat() / maxValue)
            canvas.drawLine(paddingLeft.toFloat(), goalY, (width - paddingRight).toFloat(), goalY, goalPaint)
        }

        bars.forEachIndexed { index, bar ->
            val left = paddingLeft + index * slot + (slot - barWidth) / 2f
            val barHeight = maxOf(chartHeight * (bar.steps.toFloat() / maxValue), dp(4f))
            val paint = when {
                bar.steps == 0 -> emptyPaint
                index == bars.lastIndex -> todayPaint
                else -> pastPaint
            }
            canvas.drawRoundRect(
                left, chartBottom - barHeight, left + barWidth, chartBottom,
                radius, radius, paint
            )
            canvas.drawText(
                bar.label,
                paddingLeft + index * slot + slot / 2f,
                height - paddingBottom - labelSpace * 0.3f,
                textPaint
            )
        }
    }
}
