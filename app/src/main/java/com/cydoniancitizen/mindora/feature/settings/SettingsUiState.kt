package com.cydoniancitizen.mindora.feature.settings

import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import java.time.LocalTime

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val preferences: MindoraPreferences,
        val hasOperationError: Boolean = false,
        val exportStatus: DataExportStatus = DataExportStatus.IDLE,
    ) : SettingsUiState

    data object Error : SettingsUiState
}

/**
 * Everything the settings screen can do, in one bundle: a new setting adds a callback here rather
 * than another parameter to every composable between the screen and the section that uses it.
 */
data class SettingsActions(
    val onWeeklyGoalSelected: (Int?) -> Unit = {},
    val onReminderToggle: (Boolean) -> Unit = {},
    val onReminderTimeSelected: (LocalTime) -> Unit = {},
    val onOpenNotificationSettings: () -> Unit = {},
    val onHapticIntensitySelected: (Float) -> Unit = {},
    val onLanguageSelected: (AppLanguage) -> Unit = {},
    val onExportClick: () -> Unit = {},
    val onDismissExportStatus: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

/** Where the last data export got to, so the screen can report it where it was started. */
enum class DataExportStatus {
    IDLE,
    RUNNING,
    DONE,
    FAILED,
}
