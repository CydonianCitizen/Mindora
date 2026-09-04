package com.cydoniancitizen.mindora.feature.settings

import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val preferences: MindoraPreferences,
        val hasOperationError: Boolean = false,
        val exportStatus: DataExportStatus = DataExportStatus.IDLE,
    ) : SettingsUiState

    data object Error : SettingsUiState
}

/** Where the last data export got to, so the screen can report it where it was started. */
enum class DataExportStatus {
    IDLE,
    RUNNING,
    DONE,
    FAILED,
}
