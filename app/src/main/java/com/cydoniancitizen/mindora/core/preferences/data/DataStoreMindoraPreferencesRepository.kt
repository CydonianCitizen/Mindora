package com.cydoniancitizen.mindora.core.preferences.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreMindoraPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : MindoraPreferencesRepository {
    override val preferences: Flow<MindoraPreferences> = dataStore.data.map(::read)

    /** The transform runs inside the edit, so a rejected value leaves the stored set untouched. */
    override suspend fun update(transform: (MindoraPreferences) -> MindoraPreferences) {
        dataStore.edit { values -> values.write(transform(read(values))) }
    }

    /** Anything never stored reads as the model's own default, so the defaults live in one place. */
    private fun read(values: Preferences) = with(MindoraPreferences()) {
        MindoraPreferences(
            weeklyGoalMinutes = values[weeklyGoalMinutesKey] ?: weeklyGoalMinutes,
            dailyReminderEnabled = values[dailyReminderEnabledKey] ?: dailyReminderEnabled,
            dailyReminderTime = LocalTime.of(
                values[dailyReminderHourKey] ?: dailyReminderTime.hour,
                values[dailyReminderMinuteKey] ?: dailyReminderTime.minute,
            ),
            hapticIntensity = values[hapticIntensityKey] ?: hapticIntensity,
        )
    }

    private fun MutablePreferences.write(preferences: MindoraPreferences) {
        val goal = preferences.weeklyGoalMinutes
        if (goal == null) remove(weeklyGoalMinutesKey) else set(weeklyGoalMinutesKey, goal)
        set(dailyReminderEnabledKey, preferences.dailyReminderEnabled)
        set(dailyReminderHourKey, preferences.dailyReminderTime.hour)
        set(dailyReminderMinuteKey, preferences.dailyReminderTime.minute)
        set(hapticIntensityKey, preferences.hapticIntensity)
    }

    private companion object {
        val weeklyGoalMinutesKey = intPreferencesKey("weekly_goal_minutes")
        val dailyReminderEnabledKey = booleanPreferencesKey("daily_reminder_enabled")
        val dailyReminderHourKey = intPreferencesKey("daily_reminder_hour")
        val dailyReminderMinuteKey = intPreferencesKey("daily_reminder_minute")
        val hapticIntensityKey = floatPreferencesKey("haptic_intensity")
    }
}
