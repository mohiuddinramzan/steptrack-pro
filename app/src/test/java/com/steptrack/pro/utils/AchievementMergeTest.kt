package com.steptrack.pro.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AchievementMergeTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun savedUnlock_staysUnlocked_evenIfCurrentDataNoLongerUnlocksIt() {
        val current = listOf(
            Achievement(AchievementId.FIRST_GOAL, null),
            Achievement(AchievementId.STREAK_3, null)
        )
        val saved = mapOf(AchievementId.FIRST_GOAL to d("2026-11-20"))

        val merged = AchievementMerge.merge(current, saved).associateBy { it.id }

        assertEquals(d("2026-11-20"), merged[AchievementId.FIRST_GOAL]!!.unlockedOn)
        assertNull(merged[AchievementId.STREAK_3]!!.unlockedOn)
    }

    @Test
    fun savedDate_winsOverCurrentDate() {
        val current = listOf(Achievement(AchievementId.FIRST_GOAL, d("2026-11-25")))
        val saved = mapOf(AchievementId.FIRST_GOAL to d("2026-11-20"))
        assertEquals(d("2026-11-20"), AchievementMerge.merge(current, saved)[0].unlockedOn)
    }

    @Test
    fun encodeDecode_roundTrip() {
        val map = mapOf(
            AchievementId.STREAK_7 to d("2026-12-01"),
            AchievementId.FIRST_GOAL to d("2026-11-20")
        )
        assertEquals(map, AchievementMerge.decode(AchievementMerge.encode(map)))
    }

    @Test
    fun decode_ignoresGarbageAndEmpty() {
        assertEquals(emptyMap<AchievementId, LocalDate>(), AchievementMerge.decode(null))
        assertEquals(emptyMap<AchievementId, LocalDate>(), AchievementMerge.decode(""))
        val decoded = AchievementMerge.decode("FIRST_GOAL=2026-11-20,BOGUS=2026-01-01,STREAK_3=notadate,broken")
        assertEquals(mapOf(AchievementId.FIRST_GOAL to d("2026-11-20")), decoded)
    }
}
