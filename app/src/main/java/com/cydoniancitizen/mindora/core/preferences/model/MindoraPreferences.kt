package com.cydoniancitizen.mindora.core.preferences.model

import java.time.LocalTime

data class MindoraPreferences(
    val weeklyGoalMinutes: Int? = null,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: LocalTime = LocalTime.of(20, 0),
)

val supportedWeeklyGoalMinutes = listOf(30, 60, 90, 120)

fun requireSupportedWeeklyGoal(minutes: Int?) {
    require(minutes == null || minutes in supportedWeeklyGoalMinutes) {
        "Unsupported weekly goal: $minutes."
    }
}
