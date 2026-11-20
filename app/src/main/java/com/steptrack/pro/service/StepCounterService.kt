package com.steptrack.pro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.steptrack.pro.MainActivity
import com.steptrack.pro.R
import com.steptrack.pro.data.SettingsStore
import com.steptrack.pro.data.StepRepository
import com.steptrack.pro.database.AppDatabase
import com.steptrack.pro.utils.BootInfo
import com.steptrack.pro.utils.DateUtils
import com.steptrack.pro.utils.GoalNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.text.NumberFormat

class StepCounterService : Service(), SensorEventListener {

    private data class Reading(val counter: Long, val timeMillis: Long, val bootCount: Int?)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val readings = Channel<Reading>(Channel.UNLIMITED)
    private lateinit var sensorManager: SensorManager
    private lateinit var repository: StepRepository
    private lateinit var settings: SettingsStore
    private var listening = false
    private var midnightJob: Job? = null


    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        repository = StepRepository(AppDatabase.get(this))
        settings = SettingsStore(this)
        createChannels()

        // One consumer processes readings strictly in arrival order.
        scope.launch {
            for (reading in readings) {
                try {
                    repository.recordSensorReading(
                        counterValue = reading.counter,
                        eventTimeMillis = reading.timeMillis,
                        bootCount = reading.bootCount,
                        profile = settings.load()
                    )
                    checkGoalNotification()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save step reading", e)
                }
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(null),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Foreground service not permitted", e)
            stopSelf()
            return START_NOT_STICKY
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Foreground service start not allowed", e)
            stopSelf()
            return START_NOT_STICKY
        }

        registerSensor()
        scope.launch { updateNotification() }
        midnightJob?.cancel()
        midnightJob = MidnightWatcher.launch(scope) {
            if (listening) sensorManager.flush(this@StepCounterService)
            updateNotification()
        }
        return START_STICKY
    }

    private fun registerSensor() {
        if (listening) return
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            Log.w(TAG, "No step counter sensor")
            stopSelf()
            return
        }
        listening = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL,
            BATCH_LATENCY_US
        )
        if (!listening) Log.w(TAG, "registerListener failed")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER || event.values.isEmpty()) return

        val raw = event.values[0].toLong()
        // event.timestamp should be nanoseconds since boot. Some phones report another clock,
        // so an unreasonable age falls back to the current time instead of a wrong date.
        val ageMs = (SystemClock.elapsedRealtimeNanos() - event.timestamp) / 1_000_000L
        val now = System.currentTimeMillis()
        val eventTime = if (ageMs in 0L..MAX_EVENT_AGE_MS) now - ageMs else now


        readings.trySend(
            Reading(
                counter = raw,
                timeMillis = eventTime,
                bootCount = BootInfo.readBootCount(this)
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        if (listening) {
            sensorManager.unregisterListener(this)
            listening = false
        }
        readings.close()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                GOAL_CHANNEL_ID,
                getString(R.string.channel_goal_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    /** Posts one "goal reached" notification per day. */
    private suspend fun checkGoalNotification() {
        val today = DateUtils.dateKey(System.currentTimeMillis())
        val day = repository.getDay(today) ?: return
        if (!GoalNotifier.shouldNotify(day.goalCompleted, settings.lastGoalNotifiedDate(), today)) return

        val manager = getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return

        val steps = NumberFormat.getInstance().format(day.totalSteps)
        val notification = NotificationCompat.Builder(this, GOAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(getString(R.string.goal_notif_title))
            .setContentText(getString(R.string.goal_notif_text, steps))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        manager.notify(GOAL_NOTIFICATION_ID, notification)
        settings.setLastGoalNotifiedDate(today)
    }

    private suspend fun updateNotification() {
        try {
            val key = DateUtils.dateKey(System.currentTimeMillis())
            val steps = repository.getDay(key)?.totalSteps ?: 0
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(steps))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }

    private fun buildNotification(steps: Int?): Notification {
        val text = if (steps == null) {
            getString(R.string.notif_starting)
        } else {
            getString(R.string.notif_steps_today, NumberFormat.getInstance().format(steps))
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "StepCounterService"
        private const val CHANNEL_ID = "step_tracking"
        private const val GOAL_CHANNEL_ID = "goal_alerts"
        private const val NOTIFICATION_ID = 1001
        private const val GOAL_NOTIFICATION_ID = 1002
        private const val BATCH_LATENCY_US = 60_000_000
        private const val MAX_EVENT_AGE_MS = 6L * 60 * 60 * 1000

        fun isStepSensorAvailable(context: Context): Boolean {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            return manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        }

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, StepCounterService::class.java)
                )
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Could not start service", e)
            }
        }
    }
}
