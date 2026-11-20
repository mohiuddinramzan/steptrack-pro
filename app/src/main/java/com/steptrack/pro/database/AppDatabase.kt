package com.steptrack.pro.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.steptrack.pro.data.DailyActivity
import com.steptrack.pro.data.StepDao
import com.steptrack.pro.data.TrackerState

@Database(
    entities = [DailyActivity::class, TrackerState::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun stepDao(): StepDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "steptrack.db"
                ).build().also { instance = it }
            }
    }
}
