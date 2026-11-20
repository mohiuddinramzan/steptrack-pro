package com.steptrack.pro.utils

import com.steptrack.pro.data.DailyActivity
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DayBar(val dateKey: String, val label: String, val steps: Int)

object DashboardUtils {

    /** Goal progress in percent; may exceed 100 but is capped at 999. */
    fun percent(steps: Int, goal: Int): Int =
        if (goal <= 0) 0 else ((steps.toLong() * 100) / goal).coerceIn(0L, 999L).toInt()

    /** Goal progress for the ring, always between 0 and 1. */
    fun fraction(steps: Int, goal: Int): Float =
        if (goal <= 0) 0f else (steps.toFloat() / goal).coerceIn(0f, 1f)

    fun formatDuration(seconds: Long): String {
        val minutes = seconds.coerceAtLeast(0L) / 60
        return if (minutes < 60) {
            "$minutes min"
        } else {
            val mm = (minutes % 60).toString().padStart(2, '0')
            "${minutes / 60}h ${mm}m"
        }
    }

    /** Seven bars ending today (oldest first); days without data get zero steps. */
    fun buildWeek(
        rows: List<DailyActivity>,
        todayKey: String,
        locale: Locale = Locale.getDefault()
    ): List<DayBar> {
        val today = LocalDate.parse(todayKey)
        val byDate = rows.associateBy { it.date }
        return (6 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            val key = day.toString()
            DayBar(
                dateKey = key,
                label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                steps = byDate[key]?.totalSteps ?: 0
            )
        }
    }
}
