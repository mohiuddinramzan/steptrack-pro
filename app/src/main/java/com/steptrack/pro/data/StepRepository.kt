package com.steptrack.pro.data

import androidx.room.withTransaction
import com.steptrack.pro.database.AppDatabase
import com.steptrack.pro.sensor.StepDeltaCalculator
import com.steptrack.pro.utils.DateUtils
import com.steptrack.pro.utils.StepMath
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId

class StepRepository(private val db: AppDatabase) {

    private val dao = db.stepDao()

    /**
     * Stores one sensor reading. Returns the number of new steps added (0 if none or rejected).
     * Everything happens in one transaction, so a crash cannot leave half-saved data.
     */
    suspend fun recordSensorReading(
        counterValue: Long,
        eventTimeMillis: Long,
        bootCount: Int?,
        profile: UserProfile,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long = db.withTransaction {
        val state = dao.getState()
        val result = StepDeltaCalculator.calculate(
            lastValue = state?.lastCounterValue,
            lastTimeMillis = state?.lastEventTimeMillis,
            lastBootCount = state?.lastBootCount,
            newValue = counterValue,
            newTimeMillis = eventTimeMillis,
            newBootCount = bootCount
        )

        if (!result.accepted) {
            0L
        } else {
            if (result.delta > 0) {
                val key = DateUtils.dateKey(eventTimeMillis, zone)
                val existing = dao.getDay(key)
                val totalSteps = ((existing?.totalSteps ?: 0).toLong() + result.delta)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
                val gapMs = if (state != null) eventTimeMillis - state.lastEventTimeMillis else 0L
                val distanceKm = StepMath.distanceKm(totalSteps, profile.heightCm)

                dao.upsertDay(
                    DailyActivity(
                        date = key,
                        totalSteps = totalSteps,
                        distanceKm = distanceKm,
                        calories = StepMath.calories(distanceKm, profile.weightKg),
                        activeSeconds = (existing?.activeSeconds ?: 0L) + StepMath.activeSecondsFor(gapMs),
                        goalCompleted = profile.stepGoal > 0 && totalSteps >= profile.stepGoal
                    )
                )
            }
            dao.upsertState(
                TrackerState(
                    lastCounterValue = counterValue,
                    lastEventTimeMillis = eventTimeMillis,
                    lastBootCount = bootCount
                )
            )
            result.delta
        }
    }

    suspend fun getDay(dateKey: String): DailyActivity? = dao.getDay(dateKey)

    fun observeDay(dateKey: String): Flow<DailyActivity?> = dao.observeDay(dateKey)

    fun observeRange(startKey: String, endKey: String): Flow<List<DailyActivity>> =
        dao.observeRange(startKey, endKey)
}
