package com.cydoniancitizen.mindora.core.preferences

import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

interface MindoraPreferencesRepository {
    val preferences: Flow<MindoraPreferences>

    suspend fun setWeeklyGoalMinutes(minutes: Int?)

    suspend fun setDailyReminderEnabled(enabled: Boolean)

    suspend fun setDailyReminderTime(time: LocalTime)
}
