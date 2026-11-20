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
import com.steptrack.pro.data.StepRepository
import com.steptrack.pro.data.UserProfile
import com.steptrack.pro.database.AppDatabase
import com.steptrack.pro.utils.BootInfo
import com.steptrack.pro.utils.DateUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val profile = UserProfile()
    private var listening = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        repository = StepRepository(AppDatabase.get(this))
        createChannel()

        // One consumer processes readings strictly in arrival order.
        scope.launch {
            for (reading in readings) {
                try {
                    repository.recordSensorReading(
                        counterValue = reading.counter,
                        eventTimeMillis = reading.timeMillis,
                        bootCount = reading.bootCount,
                        profile = profile
                    )
                    updateNotification()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save step reading", e)
                }
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
        // event.timestamp is time since boot; convert to wall-clock so batched events keep their real date.
        val ageMs = (SystemClock.elapsedRealtimeNanos() - event.timestamp) / 1_000_000L
        val eventTime = System.currentTimeMillis() - ageMs.coerceAtLeast(0L)
        readings.trySend(
            Reading(
                counter = event.values[0].toLong(),
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

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private suspend fun updateNotification() {
        val key = DateUtils.dateKey(System.currentTimeMillis())
        val steps = repository.getDay(key)?.totalSteps ?: 0
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(steps))
    }

    private fun buildNotification(steps: Int?): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = if (steps == null) {
            getString(R.string.notif_starting)
        } else {
            getString(R.string.notif_steps_today, NumberFormat.getInstance().format(steps))
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "StepCounterService"
        private const val CHANNEL_ID = "step_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val BATCH_LATENCY_US = 60_000_000

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
