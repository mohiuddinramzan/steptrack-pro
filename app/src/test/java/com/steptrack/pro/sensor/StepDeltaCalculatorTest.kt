package com.steptrack.pro.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepDeltaCalculatorTest {

    private fun calc(
        lastV: Long?, lastT: Long?, lastB: Int?,
        v: Long, t: Long, b: Int?
    ) = StepDeltaCalculator.calculate(lastV, lastT, lastB, v, t, b)

    @Test
    fun firstReading_hasZeroDelta_andIsAccepted() {
        val r = calc(null, null, null, 5000, 1000, 1)
        assertEquals(0L, r.delta)
        assertTrue(r.accepted)
    }

    @Test
    fun sameBoot_increase_addsDifference() {
        val r = calc(5000, 1000, 1, 5120, 2000, 1)
        assertEquals(120L, r.delta)
        assertTrue(r.accepted)
    }

    @Test
    fun identicalValue_isNotCountedTwice() {
        val r = calc(5000, 1000, 1, 5000, 2000, 1)
        assertEquals(0L, r.delta)
        assertTrue(r.accepted)
    }

    @Test
    fun reboot_detectedByBootCount_usesNewValue() {
        val r = calc(9000, 1000, 1, 40, 2000, 2)
        assertEquals(40L, r.delta)
        assertTrue(r.accepted)
    }

    @Test
    fun reboot_detectedByBootCount_evenWhenCounterIsLarger() {
        val r = calc(100, 1000, 1, 300, 2000, 2)
        assertEquals(300L, r.delta)
        assertTrue(r.accepted)
    }

    @Test
    fun sameBoot_lowerValue_isRejectedAsStale() {
        val r = calc(9000, 2000, 1, 8000, 1000, 1)
        assertEquals(0L, r.delta)
        assertFalse(r.accepted)
    }

    @Test
    fun noBootCount_lowerValueAtLaterTime_isTreatedAsReboot() {
        val r = calc(9000, 1000, null, 40, 2000, null)
        assertEquals(40L, r.delta)
        assertTrue(r.accepted)
    }

    @Test
    fun noBootCount_lowerValueAtEarlierTime_isRejectedAsStale() {
        val r = calc(9000, 2000, null, 8000, 1000, null)
        assertEquals(0L, r.delta)
        assertFalse(r.accepted)
    }

    @Test
    fun negativeValue_isRejected() {
        val r = calc(100, 1000, 1, -5, 2000, 1)
        assertEquals(0L, r.delta)
        assertFalse(r.accepted)
    }
}
