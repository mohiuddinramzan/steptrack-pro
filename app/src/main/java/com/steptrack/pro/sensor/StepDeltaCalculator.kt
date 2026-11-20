package com.steptrack.pro.sensor

data class DeltaResult(val delta: Long, val accepted: Boolean)

/**
 * Pure logic (no Android classes) turning cumulative TYPE_STEP_COUNTER values into new steps.
 * The sensor value counts steps since the last phone boot.
 */
object StepDeltaCalculator {

    fun calculate(
        lastValue: Long?,
        lastTimeMillis: Long?,
        lastBootCount: Int?,
        newValue: Long,
        newTimeMillis: Long,
        newBootCount: Int?
    ): DeltaResult {
        if (newValue < 0) return DeltaResult(0L, false)

        // First reading ever: we cannot know how many of these steps are "ours".
        if (lastValue == null || lastTimeMillis == null) return DeltaResult(0L, true)

        // Best case: Android tells us whether the phone rebooted.
        if (lastBootCount != null && newBootCount != null) {
            return when {
                newBootCount != lastBootCount -> DeltaResult(newValue, true)
                newValue >= lastValue -> DeltaResult(newValue - lastValue, true)
                else -> DeltaResult(0L, false) // stale / out-of-order
            }
        }

        // Fallback without boot count: a lower value at a later time means reboot.
        return when {
            newValue >= lastValue -> DeltaResult(newValue - lastValue, true)
            newTimeMillis > lastTimeMillis -> DeltaResult(newValue, true)
            else -> DeltaResult(0L, false)
        }
    }
}
