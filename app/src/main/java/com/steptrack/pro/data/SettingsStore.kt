package com.steptrack.pro.data

import android.content.Context
import android.content.SharedPreferences
import com.steptrack.pro.utils.AchievementId
import com.steptrack.pro.utils.AchievementMerge
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

/** Stores the user's profile, goal and unlocked achievements on the phone only. */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): UserProfile {
        val defaults = UserProfile()
        return UserProfile(
            heightCm = prefs.getInt(KEY_HEIGHT, defaults.heightCm),
            weightKg = prefs.getString(KEY_WEIGHT, null)?.toDoubleOrNull() ?: defaults.weightKg,
            stepGoal = prefs.getInt(KEY_GOAL, defaults.stepGoal)
        )
    }

    fun save(profile: UserProfile) {
        prefs.edit()
            .putInt(KEY_HEIGHT, profile.heightCm)
            .putString(KEY_WEIGHT, profile.weightKg.toString())
            .putInt(KEY_GOAL, profile.stepGoal)
            .apply()
    }

    fun lastGoalNotifiedDate(): String? = prefs.getString(KEY_LAST_GOAL_NOTIFIED, null)

    fun setLastGoalNotifiedDate(date: String) {
        prefs.edit().putString(KEY_LAST_GOAL_NOTIFIED, date).apply()
    }

    fun savedUnlocks(): Map<AchievementId, LocalDate> =
        AchievementMerge.decode(prefs.getString(KEY_UNLOCKS, null))

    fun saveUnlocks(unlocks: Map<AchievementId, LocalDate>) {
        prefs.edit().putString(KEY_UNLOCKS, AchievementMerge.encode(unlocks)).apply()
    }

    /** Emits the current profile, then again whenever it changes. */
    fun profileFlow(): Flow<UserProfile> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(load())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(load())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    companion object {
        private const val FILE_NAME = "steptrack_settings"
        private const val KEY_HEIGHT = "height_cm"
        private const val KEY_WEIGHT = "weight_kg"
        private const val KEY_GOAL = "step_goal"
        private const val KEY_LAST_GOAL_NOTIFIED = "last_goal_notified"
        private const val KEY_UNLOCKS = "achievement_unlocks"
    }
}
