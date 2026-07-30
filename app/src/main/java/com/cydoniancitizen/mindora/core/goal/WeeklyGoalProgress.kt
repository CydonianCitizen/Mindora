package com.cydoniancitizen.mindora.core.goal

import com.cydoniancitizen.mindora.core.preferences.model.requireSupportedWeeklyGoal
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class WeeklyGoalProgress(
    val practicedDuration: Duration,
    val targetDuration: Duration?,
    val fraction: Float,
    val isConfigured: Boolean,
    val isReached: Boolean,
)

fun calculateWeeklyGoalProgress(
    sessions: List<MindfulnessSession>,
    weeklyGoalMinutes: Int?,
    now: Instant,
    zoneId: ZoneId,
    firstDayOfWeek: DayOfWeek,
): WeeklyGoalProgress {
    requireSupportedWeeklyGoal(weeklyGoalMinutes)
    val currentDate = now.atZone(zoneId).toLocalDate()
    val weekStart = currentDate
        .with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        .atStartOfDay(zoneId)
        .toInstant()
    val weekEnd = weekStart.atZone(zoneId).plusWeeks(1).toInstant()
    val practicedDuration = sessions.asSequence()
        .filter { !it.startedAt.isBefore(weekStart) && it.startedAt.isBefore(weekEnd) }
        .map(MindfulnessSession::activeDuration)
        .filter { it > Duration.ZERO }
        .fold(Duration.ZERO, Duration::plus)
    val targetDuration = weeklyGoalMinutes?.let { Duration.ofMinutes(it.toLong()) }
    val isReached = targetDuration != null && practicedDuration >= targetDuration
    val fraction = if (targetDuration == null) {
        0f
    } else {
        (practicedDuration.toMillis().toDouble() / targetDuration.toMillis())
            .coerceIn(0.0, 1.0)
            .toFloat()
    }

    return WeeklyGoalProgress(
        practicedDuration = practicedDuration,
        targetDuration = targetDuration,
        fraction = fraction,
        isConfigured = targetDuration != null,
        isReached = isReached,
    )
}
