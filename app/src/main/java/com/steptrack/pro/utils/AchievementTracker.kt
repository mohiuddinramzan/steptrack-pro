package com.steptrack.pro.utils

import com.steptrack.pro.data.DailyActivity
import com.steptrack.pro.data.SettingsStore
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Pure helpers for keeping the first unlock date of each achievement. */
object AchievementMerge {

    /** A date saved earlier always wins, so an unlocked achievement never becomes locked again. */
    fun merge(current: List<Achievement>, saved: Map<AchievementId, LocalDate>): List<Achievement> =
        current.map { Achievement(it.id, saved[it.id] ?: it.unlockedOn) }

    fun encode(unlocks: Map<AchievementId, LocalDate>): String =
        unlocks.entries
            .sortedBy { it.key.name }
            .joinToString(",") { "${it.key.name}=${it.value}" }

    fun decode(text: String?): Map<AchievementId, LocalDate> {
        if (text.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<AchievementId, LocalDate>()
        for (part in text.split(',')) {
            val pieces = part.split('=', limit = 2)
            if (pieces.size != 2) continue
            val id = AchievementId.values().firstOrNull { it.name == pieces[0].trim() } ?: continue
            val date = try {
                LocalDate.parse(pieces[1].trim())
            } catch (e: DateTimeParseException) {
                continue
            }
            result[id] = date
        }
        return result
    }
}

object AchievementTracker {

    /** Evaluates the stored days and remembers any newly unlocked achievement. */
    fun resolve(settings: SettingsStore, rows: List<DailyActivity>): List<Achievement> {
        val saved = settings.savedUnlocks()
        val merged = AchievementMerge.merge(AchievementUtils.evaluate(rows), saved)
        val fresh = merged.filter { it.unlockedOn != null && it.id !in saved }
        if (fresh.isNotEmpty()) {
            val all = saved.toMutableMap()
            for (achievement in fresh) {
                achievement.unlockedOn?.let { all[achievement.id] = it }
            }
            settings.saveUnlocks(all)
        }
        return merged
    }
}
