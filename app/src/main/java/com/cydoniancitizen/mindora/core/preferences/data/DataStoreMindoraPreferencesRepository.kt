package com.cydoniancitizen.mindora.core.preferences.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.core.preferences.model.requireSupportedWeeklyGoal
import java.time.DateTimeException
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreMindoraPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : MindoraPreferencesRepository {
    override val preferences: Flow<MindoraPreferences> = dataStore.data.map { values ->
        val weeklyGoalMinutes = values[weeklyGoalMinutesKey]
        requireSupportedWeeklyGoal(weeklyGoalMinutes)
        MindoraPreferences(
            weeklyGoalMinutes = weeklyGoalMinutes,
            dailyReminderEnabled = values[dailyReminderEnabledKey] ?: false,
            dailyReminderTime = reminderTime(
                hour = values[dailyReminderHourKey] ?: DEFAULT_REMINDER_HOUR,
                minute = values[dailyReminderMinuteKey] ?: DEFAULT_REMINDER_MINUTE,
            ),
        )
    }

    override suspend fun setWeeklyGoalMinutes(minutes: Int?) {
        requireSupportedWeeklyGoal(minutes)
        dataStore.edit { values ->
            if (minutes == null) {
                values.remove(weeklyGoalMinutesKey)
            } else {
                values[weeklyGoalMinutesKey] = minutes
            }
        }
    }

    override suspend fun setDailyReminderEnabled(enabled: Boolean) {
        dataStore.edit { values ->
            values[dailyReminderEnabledKey] = enabled
        }
    }

    override suspend fun setDailyReminderTime(time: LocalTime) {
        dataStore.edit { values ->
            values[dailyReminderHourKey] = time.hour
            values[dailyReminderMinuteKey] = time.minute
        }
    }

    private fun reminderTime(hour: Int, minute: Int): LocalTime = try {
        LocalTime.of(hour, minute)
    } catch (error: DateTimeException) {
        throw IllegalStateException("Stored reminder time is invalid.", error)
    }

    private companion object {
        const val DEFAULT_REMINDER_HOUR = 20
        const val DEFAULT_REMINDER_MINUTE = 0

        val weeklyGoalMinutesKey = intPreferencesKey("weekly_goal_minutes")
        val dailyReminderEnabledKey = booleanPreferencesKey("daily_reminder_enabled")
        val dailyReminderHourKey = intPreferencesKey("daily_reminder_hour")
        val dailyReminderMinuteKey = intPreferencesKey("daily_reminder_minute")
    }
}
