package com.steptrack.pro.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLogicTest {

    @Test
    fun height_acceptsValidRangeOnly() {
        assertEquals(170, ProfileValidator.parseHeight("170"))
        assertEquals(170, ProfileValidator.parseHeight(" 170 "))
        assertNull(ProfileValidator.parseHeight("99"))
        assertNull(ProfileValidator.parseHeight("251"))
        assertNull(ProfileValidator.parseHeight("abc"))
        assertNull(ProfileValidator.parseHeight(""))
    }

    @Test
    fun weight_acceptsCommaAndRange() {
        assertEquals(72.5, ProfileValidator.parseWeight("72,5")!!, 0.0001)
        assertEquals(70.0, ProfileValidator.parseWeight("70")!!, 0.0001)
        assertNull(ProfileValidator.parseWeight("19.9"))
        assertNull(ProfileValidator.parseWeight("301"))
        assertNull(ProfileValidator.parseWeight("NaN"))
        assertNull(ProfileValidator.parseWeight(""))
    }

    @Test
    fun goal_acceptsValidRangeOnly() {
        assertEquals(10_000, ProfileValidator.parseGoal("10000"))
        assertEquals(100, ProfileValidator.parseGoal("100"))
        assertNull(ProfileValidator.parseGoal("99"))
        assertNull(ProfileValidator.parseGoal("100001"))
        assertNull(ProfileValidator.parseGoal("12.5"))
    }

    @Test
    fun goalNotification_onlyOncePerDay() {
        assertTrue(GoalNotifier.shouldNotify(true, null, "2026-11-20"))
        assertTrue(GoalNotifier.shouldNotify(true, "2026-11-19", "2026-11-20"))
        assertFalse(GoalNotifier.shouldNotify(true, "2026-11-20", "2026-11-20"))
        assertFalse(GoalNotifier.shouldNotify(false, null, "2026-11-20"))
    }
}
