package com.steptrack.pro.service

import android.util.Log
import com.steptrack.pro.utils.TimeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Runs [onMidnight] once after every local midnight while the scope is alive. */
object MidnightWatcher {

    fun launch(scope: CoroutineScope, onMidnight: suspend () -> Unit): Job =
        scope.launch {
            while (isActive) {
                delay(TimeUtils.millisUntilNextMidnight(System.currentTimeMillis()) + 1_000L)
                try {
                    onMidnight()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("MidnightWatcher", "Midnight task failed", e)
                }
            }
        }
}
