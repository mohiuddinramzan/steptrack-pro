package com.steptrack.pro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per calendar day. [date] is "yyyy-MM-dd" in the device's local time zone. */
@Entity(tableName = "daily_activity")
data class DailyActivity(
    @PrimaryKey val date: String,
    val totalSteps: Int,
    val distanceKm: Double,
    val calories: Double,
    val activeSeconds: Long,
    val goalCompleted: Boolean
)

/** Single-row table (id = 1) remembering the last accepted sensor reading. */
@Entity(tableName = "tracker_state")
data class TrackerState(
    @PrimaryKey val id: Int = 1,
    val lastCounterValue: Long,
    val lastEventTimeMillis: Long,
    val lastBootCount: Int?
)
