package com.steptrack.pro.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TimeUtilsTest {

    private fun millis(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun oneSecondBeforeMidnight() {
        val dhaka = ZoneId.of("Asia/Dhaka")
        assertEquals(1_000L, TimeUtils.millisUntilNextMidnight(millis("2026-11-19T17:59:59Z"), dhaka))
    }

    @Test
    fun exactlyAtMidnight_waitsAFullDay() {
        val dhaka = ZoneId.of("Asia/Dhaka")
        assertEquals(86_400_000L, TimeUtils.millisUntilNextMidnight(millis("2026-11-19T18:00:00Z"), dhaka))
    }

    @Test
    fun dstEndDay_has25Hours() {
        val newYork = ZoneId.of("America/New_York")
        assertEquals(90_000_000L, TimeUtils.millisUntilNextMidnight(millis("2026-11-01T04:00:00Z"), newYork))
    }
}
