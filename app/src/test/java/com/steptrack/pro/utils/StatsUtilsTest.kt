package com.steptrack.pro.utils

import com.steptrack.pro.data.DailyActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class StatsUtilsTest {

    private fun d(s: String) = LocalDate.parse(s)

    private fun row(date: String, steps: Int, goal: Boolean = false) =
        DailyActivity(date, steps, steps / 1000.0, steps / 20.0, steps.toLong(), goal)

    @Test
    fun week_startsOnConfiguredDay() {
        val monday = StatsUtils.periodFor(StatsMode.WEEK, d("2026-11-20"), DayOfWeek.MONDAY)
        assertEquals(Period(d("2026-11-16"), d("2026-11-22")), monday)

        val saturday = StatsUtils.periodFor(StatsMode.WEEK, d("2026-11-20"), DayOfWeek.SATURDAY)
        assertEquals(Period(d("2026-11-14"), d("2026-11-20")), saturday)
    }

    @Test
    fun month_coversWholeMonthIncludingLeapFebruary() {
        assertEquals(
            Period(d("2026-11-01"), d("2026-11-30")),
            StatsUtils.periodFor(StatsMode.MONTH, d("2026-11-20"), DayOfWeek.MONDAY)
        )
        assertEquals(
            Period(d("2028-02-01"), d("2028-02-29")),
            StatsUtils.periodFor(StatsMode.MONTH, d("2028-02-10"), DayOfWeek.MONDAY)
        )
    }

    @Test
    fun day_isSingleDate() {
        val p = StatsUtils.periodFor(StatsMode.DAY, d("2026-11-20"), DayOfWeek.MONDAY)
        assertEquals(Period(d("2026-11-20"), d("2026-11-20")), p)
    }

    @Test
    fun shift_movesByUnit_andClampsMonthEnd() {
        assertEquals(d("2026-11-19"), StatsUtils.shift(StatsMode.DAY, d("2026-11-20"), -1))
        assertEquals(d("2026-11-13"), StatsUtils.shift(StatsMode.WEEK, d("2026-11-20"), -1))
        assertEquals(d("2026-02-28"), StatsUtils.shift(StatsMode.MONTH, d("2026-01-31"), 1))
    }

    @Test
    fun canGoForward_neverReachesTheFuture() {
        val today = d("2026-11-20")
        assertFalse(StatsUtils.canGoForward(StatsMode.DAY, today, today, DayOfWeek.MONDAY))
        assertTrue(StatsUtils.canGoForward(StatsMode.DAY, d("2026-11-19"), today, DayOfWeek.MONDAY))
        assertFalse(StatsUtils.canGoForward(StatsMode.WEEK, today, today, DayOfWeek.MONDAY))
        assertTrue(StatsUtils.canGoForward(StatsMode.WEEK, d("2026-11-13"), today, DayOfWeek.MONDAY))
        assertFalse(StatsUtils.canGoForward(StatsMode.MONTH, today, today, DayOfWeek.MONDAY))
        assertTrue(StatsUtils.canGoForward(StatsMode.MONTH, d("2026-10-05"), today, DayOfWeek.MONDAY))
    }

    @Test
    fun buildBars_monthHasOneBarPerDay_withNumberLabels() {
        val period = StatsUtils.periodFor(StatsMode.MONTH, d("2026-11-20"), DayOfWeek.MONDAY)
        val bars = StatsUtils.buildBars(listOf(row("2026-11-05", 700)), period, StatsMode.MONTH, Locale.ENGLISH)
        assertEquals(30, bars.size)
        assertEquals("1", bars.first().label)
        assertEquals("30", bars.last().label)
        assertEquals(700, bars[4].steps)
        assertEquals(0, bars[5].steps)
    }

    @Test
    fun buildBars_weekHasSevenBars() {
        val period = StatsUtils.periodFor(StatsMode.WEEK, d("2026-11-20"), DayOfWeek.MONDAY)
        val bars = StatsUtils.buildBars(listOf(row("2026-11-20", 1200)), period, StatsMode.WEEK, Locale.ENGLISH)
        assertEquals(7, bars.size)
        assertEquals("2026-11-16", bars.first().dateKey)
        assertEquals(1200, bars[4].steps)
    }

    @Test
    fun summarize_countsOnlyDaysInsidePeriod() {
        val period = StatsUtils.periodFor(StatsMode.WEEK, d("2026-11-20"), DayOfWeek.MONDAY)
        val rows = listOf(
            row("2026-11-15", 99_999),
            row("2026-11-17", 1_000, goal = true),
            row("2026-11-19", 3_000),
            row("2026-11-21", 0)
        )
        val s = StatsUtils.summarize(rows, period, d("2026-11-20"))

        assertEquals(4_000, s.totalSteps)
        assertEquals(2, s.activeDays)
        assertEquals(2_000, s.avgStepsPerActiveDay)
        assertEquals("2026-11-19", s.bestDate)
        assertEquals(3_000, s.bestSteps)
        assertEquals(1, s.goalDays)
        assertEquals(5, s.dayCount)
        assertEquals(4_000L, s.activeSeconds)
        assertEquals(4.0, s.distanceKm, 0.0001)
    }

    @Test
    fun summarize_emptyPeriod_hasNoBestDayAndZeroAverage() {
        val period = StatsUtils.periodFor(StatsMode.DAY, d("2026-11-20"), DayOfWeek.MONDAY)
        val s = StatsUtils.summarize(emptyList(), period, d("2026-11-20"))
        assertEquals(0, s.totalSteps)
        assertEquals(0, s.avgStepsPerActiveDay)
        assertNull(s.bestDate)
        assertEquals(1, s.dayCount)
    }
}
