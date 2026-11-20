package com.steptrack.pro.utils

import com.steptrack.pro.data.DailyActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalStatsTest {

    private fun d(s: String) = LocalDate.parse(s)

    private fun row(date: String, steps: Int, goal: Boolean) =
        DailyActivity(date, steps, 0.0, 0.0, 0L, goal)

    private val goalDays = setOf(d("2026-11-10"), d("2026-11-11"), d("2026-11-12"), d("2026-11-14"))

    @Test
    fun currentStreak_countsBackFromTodayIfCompleted() {
        assertEquals(1, StreakUtils.current(goalDays, d("2026-11-14")))
    }

    @Test
    fun currentStreak_keepsYesterdayStreakUntilDayEnds() {
        assertEquals(3, StreakUtils.current(goalDays, d("2026-11-13")))
        assertEquals(1, StreakUtils.current(goalDays, d("2026-11-15")))
    }

    @Test
    fun currentStreak_isZeroAfterAMissedDay() {
        assertEquals(0, StreakUtils.current(goalDays, d("2026-11-16")))
        assertEquals(0, StreakUtils.current(emptySet(), d("2026-11-16")))
    }

    @Test
    fun bestStreak_findsLongestRun() {
        assertEquals(3, StreakUtils.best(goalDays))
        assertEquals(0, StreakUtils.best(emptyList()))
    }

    @Test
    fun goalDates_onlyIncludesCompletedDaysSorted() {
        val rows = listOf(row("2026-11-12", 5, true), row("2026-11-10", 5, true), row("2026-11-11", 5, false))
        assertEquals(listOf(d("2026-11-10"), d("2026-11-12")), StreakUtils.goalDates(rows))
    }

    @Test
    fun achievements_unlockWithCorrectDates() {
        val rows = listOf(
            row("2026-11-10", 9_000, true),
            row("2026-11-11", 9_000, true),
            row("2026-11-12", 10_500, true),
            row("2026-11-14", 9_000, true)
        )
        val result = AchievementUtils.evaluate(rows).associateBy { it.id }

        assertEquals(d("2026-11-10"), result[AchievementId.FIRST_GOAL]!!.unlockedOn)
        assertEquals(d("2026-11-12"), result[AchievementId.STREAK_3]!!.unlockedOn)
        assertNull(result[AchievementId.STREAK_7]!!.unlockedOn)
        assertNull(result[AchievementId.STREAK_30]!!.unlockedOn)
        assertEquals(d("2026-11-12"), result[AchievementId.STEPS_10K_DAY]!!.unlockedOn)
        assertNull(result[AchievementId.STEPS_20K_DAY]!!.unlockedOn)
        assertNull(result[AchievementId.TOTAL_100K_STEPS]!!.unlockedOn)
    }

    @Test
    fun achievements_totalAndBigDay() {
        val rows = (1..5).map { row("2026-11-0$it", 25_000, false) }
        val result = AchievementUtils.evaluate(rows).associateBy { it.id }

        assertEquals(d("2026-11-01"), result[AchievementId.STEPS_20K_DAY]!!.unlockedOn)
        assertEquals(d("2026-11-04"), result[AchievementId.TOTAL_100K_STEPS]!!.unlockedOn)
        assertFalse(result[AchievementId.FIRST_GOAL]!!.unlocked)
    }

    @Test
    fun achievements_noData_allLocked() {
        assertTrue(AchievementUtils.evaluate(emptyList()).none { it.unlocked })
    }
}
