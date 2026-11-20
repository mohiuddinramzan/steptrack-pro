package com.steptrack.pro.utils

import com.steptrack.pro.data.DailyActivity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class StatsMode { DAY, WEEK, MONTH }

data class Period(val start: LocalDate, val end: LocalDate)

data class PeriodSummary(
    val totalSteps: Int,
    val distanceKm: Double,
    val calories: Double,
    val activeSeconds: Long,
    val activeDays: Int,
    val avgStepsPerActiveDay: Int,
    val bestDate: String?,
    val bestSteps: Int,
    val goalDays: Int,
    /** Days of the period that have already started (future days are not counted). */
    val dayCount: Int
)

object StatsUtils {

    fun periodFor(mode: StatsMode, anchor: LocalDate, firstDayOfWeek: DayOfWeek): Period =
        when (mode) {
            StatsMode.DAY -> Period(anchor, anchor)
            StatsMode.WEEK -> {
                val start = anchor.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                Period(start, start.plusDays(6))
            }
            StatsMode.MONTH -> {
                val start = anchor.withDayOfMonth(1)
                Period(start, start.plusMonths(1).minusDays(1))
            }
        }

    fun shift(mode: StatsMode, anchor: LocalDate, delta: Int): LocalDate =
        when (mode) {
            StatsMode.DAY -> anchor.plusDays(delta.toLong())
            StatsMode.WEEK -> anchor.plusWeeks(delta.toLong())
            StatsMode.MONTH -> anchor.plusMonths(delta.toLong())
        }

    /** True if the next period has already started (we never show the future). */
    fun canGoForward(
        mode: StatsMode,
        anchor: LocalDate,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek
    ): Boolean {
        val next = periodFor(mode, shift(mode, anchor, 1), firstDayOfWeek)
        return !next.start.isAfter(today)
    }

    /** One bar per day of the period (days without data have zero steps). */
    fun buildBars(
        rows: List<DailyActivity>,
        period: Period,
        mode: StatsMode,
        locale: Locale = Locale.getDefault()
    ): List<DayBar> {
        val byDate = rows.associateBy { it.date }
        val days = (ChronoUnit.DAYS.between(period.start, period.end) + 1).toInt()
        return (0 until days).map { i ->
            val day = period.start.plusDays(i.toLong())
            val key = day.toString()
            val label = if (mode == StatsMode.MONTH) {
                day.dayOfMonth.toString()
            } else {
                day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            }
            DayBar(key, label, byDate[key]?.totalSteps ?: 0)
        }
    }

    fun summarize(rows: List<DailyActivity>, period: Period, today: LocalDate): PeriodSummary {
        val startKey = period.start.toString()
        val endKey = period.end.toString()
        val inPeriod = rows.filter { it.date >= startKey && it.date <= endKey }
        val active = inPeriod.filter { it.totalSteps > 0 }
        val total = inPeriod.sumOf { it.totalSteps }
        val best = active.maxByOrNull { it.totalSteps }

        val lastDay = if (period.end.isAfter(today)) today else period.end
        val dayCount = (ChronoUnit.DAYS.between(period.start, lastDay) + 1).toInt().coerceAtLeast(0)

        return PeriodSummary(
            totalSteps = total,
            distanceKm = inPeriod.sumOf { it.distanceKm },
            calories = inPeriod.sumOf { it.calories },
            activeSeconds = inPeriod.sumOf { it.activeSeconds },
            activeDays = active.size,
            avgStepsPerActiveDay = if (active.isEmpty()) 0 else total / active.size,
            bestDate = best?.date,
            bestSteps = best?.totalSteps ?: 0,
            goalDays = inPeriod.count { it.goalCompleted },
            dayCount = dayCount
        )
    }

    fun periodLabel(mode: StatsMode, period: Period, locale: Locale = Locale.getDefault()): String =
        when (mode) {
            StatsMode.DAY -> period.start.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            )
            StatsMode.WEEK -> {
                val short = DateTimeFormatter.ofPattern("d MMM", locale)
                period.start.format(short) + " – " + period.end.format(short)
            }
            StatsMode.MONTH -> period.start.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        }
}
