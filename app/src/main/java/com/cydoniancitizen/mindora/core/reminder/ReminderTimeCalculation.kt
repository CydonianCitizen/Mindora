package com.cydoniancitizen.mindora.core.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

fun calculateNextReminderInstant(
    now: Instant,
    time: LocalTime,
    zoneId: ZoneId,
): Instant {
    val today = now.atZone(zoneId).toLocalDate()
    val todayCandidate = today.atTime(time).atZone(zoneId).toInstant()
    return if (todayCandidate.isAfter(now)) {
        todayCandidate
    } else {
        today.plusDays(1).atTime(time).atZone(zoneId).toInstant()
    }
}
