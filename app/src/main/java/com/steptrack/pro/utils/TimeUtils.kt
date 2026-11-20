package com.steptrack.pro.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

object TimeUtils {

    /** Milliseconds from [nowMillis] until the next local midnight (handles 23h/25h DST days). */
    fun millisUntilNextMidnight(
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
        return Duration.between(now, nextMidnight).toMillis()
    }

    /** Emits today's date key now, and again each time the date changes. */
    fun todayKeyFlow(zone: ZoneId = ZoneId.systemDefault()): Flow<String> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(DateUtils.dateKey(now, zone))
            delay(millisUntilNextMidnight(now, zone) + 1_000L)
        }
    }.distinctUntilChanged()
}
