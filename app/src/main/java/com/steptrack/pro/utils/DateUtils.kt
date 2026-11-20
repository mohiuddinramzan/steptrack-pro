package com.steptrack.pro.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Returns the local calendar date of [epochMillis] as "yyyy-MM-dd". */
    fun dateKey(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().format(formatter)
}
