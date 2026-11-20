package com.steptrack.pro.utils

import com.steptrack.pro.data.DailyActivity
import java.time.LocalDate

enum class AchievementId {
    FIRST_GOAL,
    STREAK_3,
    STREAK_7,
    STREAK_30,
    STEPS_10K_DAY,
    STEPS_20K_DAY,
    TOTAL_100K_STEPS
}

data class Achievement(val id: AchievementId, val unlockedOn: LocalDate?) {
    val unlocked: Boolean get() = unlockedOn != null
}

object StreakUtils {

    fun goalDates(rows: List<DailyActivity>): List<LocalDate> =
        rows.filter { it.goalCompleted }.map { LocalDate.parse(it.date) }.distinct().sorted()

    /**
     * Consecutive days with the goal reached. If today is not yet completed, the streak
     * is counted up to yesterday so it is not lost before the day ends.
     */
    fun current(goalDates: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in goalDates) today else today.minusDays(1)
        var count = 0
        while (day in goalDates) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun best(goalDates: Collection<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in goalDates.distinct().sorted()) {
            val p = previous
            run = if (p != null && p.plusDays(1) == date) run + 1 else 1
            if (run > best) best = run
            previous = date
        }
        return best
    }
}

object AchievementUtils {

    private val STREAK_TARGETS = listOf(3, 7, 30)

    /** Works out which achievements are unlocked, and on which date, from the stored days. */
    fun evaluate(rows: List<DailyActivity>): List<Achievement> {
        val sorted = rows.sortedBy { it.date }
        val goalDates = StreakUtils.goalDates(sorted)

        val streakReached = mutableMapOf<Int, LocalDate>()
        var run = 0
        var previous: LocalDate? = null
        for (date in goalDates) {
            val p = previous
            run = if (p != null && p.plusDays(1) == date) run + 1 else 1
            previous = date
            for (target in STREAK_TARGETS) {
                if (run >= target && target !in streakReached) streakReached[target] = date
            }
        }

        var cumulative = 0L
        var totalDate: LocalDate? = null
        for (row in sorted) {
            cumulative += row.totalSteps
            if (totalDate == null && cumulative >= 100_000L) totalDate = LocalDate.parse(row.date)
        }

        fun firstDayWith(minSteps: Int): LocalDate? =
            sorted.firstOrNull { it.totalSteps >= minSteps }?.let { LocalDate.parse(it.date) }

        return listOf(
            Achievement(AchievementId.FIRST_GOAL, goalDates.firstOrNull()),
            Achievement(AchievementId.STREAK_3, streakReached[3]),
            Achievement(AchievementId.STREAK_7, streakReached[7]),
            Achievement(AchievementId.STREAK_30, streakReached[30]),
            Achievement(AchievementId.STEPS_10K_DAY, firstDayWith(10_000)),
            Achievement(AchievementId.STEPS_20K_DAY, firstDayWith(20_000)),
            Achievement(AchievementId.TOTAL_100K_STEPS, totalDate)
        )
    }
}
