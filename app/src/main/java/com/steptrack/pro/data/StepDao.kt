package com.steptrack.pro.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Query("SELECT * FROM daily_activity WHERE date = :date")
    suspend fun getDay(date: String): DailyActivity?

    @Query("SELECT * FROM daily_activity WHERE date = :date")
    fun observeDay(date: String): Flow<DailyActivity?>

    @Query("SELECT * FROM daily_activity WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeRange(startDate: String, endDate: String): Flow<List<DailyActivity>>

    @Upsert
    suspend fun upsertDay(activity: DailyActivity)

    @Query("SELECT * FROM tracker_state WHERE id = 1")
    suspend fun getState(): TrackerState?

    @Query("SELECT * FROM tracker_state WHERE id = 1")
    fun observeState(): Flow<TrackerState?>

    @Upsert
    suspend fun upsertState(state: TrackerState)
}
