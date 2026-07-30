package com.cydoniancitizen.mindora.feature.settings

import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val preferences: MindoraPreferences,
        val hasOperationError: Boolean = false,
    ) : SettingsUiState

    data object Error : SettingsUiState
}
