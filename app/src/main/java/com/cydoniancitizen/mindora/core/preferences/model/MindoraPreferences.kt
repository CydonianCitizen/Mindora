package com.cydoniancitizen.mindora.core.preferences.model

import java.time.LocalTime

/**
 * The whole preference set, validating itself: no repository, view model or test can hold an
 * unsupported value, so the checks live here instead of being repeated at every write.
 */
data class MindoraPreferences(
    val weeklyGoalMinutes: Int? = null,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: LocalTime = LocalTime.of(20, 0),
    val hapticIntensity: Float = DEFAULT_HAPTIC_INTENSITY,
) {
    init {
        requireSupportedWeeklyGoal(weeklyGoalMinutes)
        requireSupportedHapticIntensity(hapticIntensity)
    }
}

const val DEFAULT_HAPTIC_INTENSITY = 0.4f

val supportedWeeklyGoalMinutes = listOf(30, 60, 90, 120)

fun requireSupportedWeeklyGoal(minutes: Int?) {
    require(minutes == null || minutes in supportedWeeklyGoalMinutes) {
        "Unsupported weekly goal: $minutes."
    }
}

fun requireSupportedHapticIntensity(intensity: Float) {
    require(intensity.isFinite() && intensity in 0f..1f) {
        "Unsupported haptic intensity: $intensity."
    }
}
