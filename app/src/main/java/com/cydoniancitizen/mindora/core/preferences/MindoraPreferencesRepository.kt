package com.cydoniancitizen.mindora.core.preferences

import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import kotlinx.coroutines.flow.Flow

interface MindoraPreferencesRepository {
    val preferences: Flow<MindoraPreferences>

    /**
     * Applies [transform] to the stored set as a single read-modify-write, so a new preference
     * costs a field on [MindoraPreferences] instead of another method here.
     */
    suspend fun update(transform: (MindoraPreferences) -> MindoraPreferences)
}
