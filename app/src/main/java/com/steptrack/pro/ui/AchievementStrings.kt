package com.steptrack.pro.ui

import com.steptrack.pro.R
import com.steptrack.pro.utils.AchievementId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object AchievementStrings {

    fun title(id: AchievementId): Int = when (id) {
        AchievementId.FIRST_GOAL -> R.string.ach_first_goal_title
        AchievementId.STREAK_3 -> R.string.ach_streak_3_title
        AchievementId.STREAK_7 -> R.string.ach_streak_7_title
        AchievementId.STREAK_30 -> R.string.ach_streak_30_title
        AchievementId.STEPS_10K_DAY -> R.string.ach_steps_10k_title
        AchievementId.STEPS_20K_DAY -> R.string.ach_steps_20k_title
        AchievementId.TOTAL_100K_STEPS -> R.string.ach_total_100k_title
    }

    fun description(id: AchievementId): Int = when (id) {
        AchievementId.FIRST_GOAL -> R.string.ach_first_goal_desc
        AchievementId.STREAK_3 -> R.string.ach_streak_3_desc
        AchievementId.STREAK_7 -> R.string.ach_streak_7_desc
        AchievementId.STREAK_30 -> R.string.ach_streak_30_desc
        AchievementId.STEPS_10K_DAY -> R.string.ach_steps_10k_desc
        AchievementId.STEPS_20K_DAY -> R.string.ach_steps_20k_desc
        AchievementId.TOTAL_100K_STEPS -> R.string.ach_total_100k_desc
    }

    fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}
