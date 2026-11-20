package com.steptrack.pro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.steptrack.pro.service.StepCounterService
import com.steptrack.pro.utils.PermissionUtils

/** Restarts step tracking after a phone restart or an app update. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (!PermissionUtils.hasActivityRecognition(context)) return
                if (!StepCounterService.isStepSensorAvailable(context)) return
                try {
                    StepCounterService.start(context)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Could not restart tracking", e)
                }
            }
        }
    }
}
