package com.steptrack.pro.utils

/** Estimates only. Constants are common approximations and will be tunable later. */
object StepMath {

    const val STRIDE_FACTOR = 0.415
    const val CALORIES_PER_KG_PER_KM = 0.57
    const val MAX_ACTIVE_GAP_MS = 60_000L

    fun distanceKm(steps: Int, heightCm: Int): Double {
        if (steps <= 0 || heightCm <= 0) return 0.0
        val strideCm = heightCm * STRIDE_FACTOR
        return steps * strideCm / 100_000.0
    }

    fun calories(distanceKm: Double, weightKg: Double): Double {
        if (distanceKm <= 0.0 || weightKg <= 0.0) return 0.0
        return weightKg * distanceKm * CALORIES_PER_KG_PER_KM
    }

    /** Time between two step events counts as walking only if the gap is up to 60 seconds. */
    fun activeSecondsFor(gapMs: Long): Long =
        if (gapMs in 1..MAX_ACTIVE_GAP_MS) gapMs / 1000 else 0L
}
