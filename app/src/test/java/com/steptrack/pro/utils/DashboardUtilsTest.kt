package com.steptrack.pro.utils

import com.steptrack.pro.data.DailyActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardUtilsTest {

    private fun row(date: String, steps: Int) =
        DailyActivity(date, steps, 0.0, 0.0, 0L, false)

    @Test
    fun percent_basicAndCapped() {
        assertEquals(50, DashboardUtils.percent(5_000, 10_000))
        assertEquals(100, DashboardUtils.percent(10_000, 10_000))
        assertEquals(150, DashboardUtils.percent(15_000, 10_000))
        assertEquals(999, DashboardUtils.percent(Int.MAX_VALUE, 1))
        assertEquals(0, DashboardUtils.percent(500, 0))
    }

    @Test
    fun fraction_isClampedBetweenZeroAndOne() {
        assertEquals(0.5f, DashboardUtils.fraction(5_000, 10_000), 0.0001f)
        assertEquals(1f, DashboardUtils.fraction(25_000, 10_000), 0.0001f)
        assertEquals(0f, DashboardUtils.fraction(100, 0), 0.0001f)
    }

    @Test
    fun formatDuration_variousLengths() {
        assertEquals("0 min", DashboardUtils.formatDuration(0))
        assertEquals("0 min", DashboardUtils.formatDuration(59))
        assertEquals("10 min", DashboardUtils.formatDuration(600))
        assertEquals("1h 05m", DashboardUtils.formatDuration(3_900))
        assertEquals("2h 00m", DashboardUtils.formatDuration(7_200))
        assertEquals("0 min", DashboardUtils.formatDuration(-30))
    }

    @Test
    fun buildWeek_returnsSevenDaysEndingToday() {
        val rows = listOf(
            row("2026-11-10", 9_999),
            row("2026-11-18", 500),
            row("2026-11-20", 1_200)
        )
        val week = DashboardUtils.buildWeek(rows, "2026-11-20")

        assertEquals(7, week.size)
        assertEquals("2026-11-14", week.first().dateKey)
        assertEquals("2026-11-20", week.last().dateKey)
        assertEquals(500, week[4].steps)
        assertEquals(1_200, week[6].steps)
        assertEquals(0, week[0].steps)
        assertEquals(0, week.sumOf { it.steps } - 1_700)
    }
}
