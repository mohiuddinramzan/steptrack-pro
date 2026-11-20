package com.steptrack.pro.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class StepMathDateUtilsTest {

    @Test
    fun distance_for10000Steps_at170cm() {
        assertEquals(7.055, StepMath.distanceKm(10_000, 170), 0.001)
    }

    @Test
    fun distance_isZero_forInvalidInput() {
        assertEquals(0.0, StepMath.distanceKm(0, 170), 0.0)
        assertEquals(0.0, StepMath.distanceKm(100, 0), 0.0)
    }

    @Test
    fun calories_for70kg_over7_055km() {
        assertEquals(281.4945, StepMath.calories(7.055, 70.0), 0.001)
    }

    @Test
    fun activeSeconds_countsOnlyGapsUpTo60Seconds() {
        assertEquals(30L, StepMath.activeSecondsFor(30_000))
        assertEquals(60L, StepMath.activeSecondsFor(60_000))
        assertEquals(0L, StepMath.activeSecondsFor(61_000))
        assertEquals(0L, StepMath.activeSecondsFor(0))
        assertEquals(0L, StepMath.activeSecondsFor(-5))
    }

    @Test
    fun dateKey_justBeforeMidnight_isStillSameDay() {
        val dhaka = ZoneId.of("Asia/Dhaka")
        val millis = Instant.parse("2026-11-19T17:59:59Z").toEpochMilli()
        assertEquals("2026-11-19", DateUtils.dateKey(millis, dhaka))
    }

    @Test
    fun dateKey_afterMidnight_isNextDay() {
        val dhaka = ZoneId.of("Asia/Dhaka")
        val millis = Instant.parse("2026-11-19T18:00:01Z").toEpochMilli()
        assertEquals("2026-11-20", DateUtils.dateKey(millis, dhaka))
    }
}
