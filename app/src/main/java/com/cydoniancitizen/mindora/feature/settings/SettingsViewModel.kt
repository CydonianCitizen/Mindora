package com.cydoniancitizen.mindora.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.export.MindoraDataExporter
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.core.reminder.MindfulnessReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: MindoraPreferencesRepository,
    private val reminderScheduler: MindfulnessReminderScheduler,
    private val dataExporter: MindoraDataExporter,
) : ViewModel() {
    private val operationError = MutableStateFlow(false)
    private val exportStatus = MutableStateFlow(DataExportStatus.IDLE)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        operationError,
        exportStatus,
    ) { preferences, hasOperationError, export ->
        SettingsUiState.Content(preferences, hasOperationError, export) as SettingsUiState
    }.catch {
        emit(SettingsUiState.Error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState.Loading,
    )

    fun setWeeklyGoal(minutes: Int?) {
        viewModelScope.launch {
            operationError.value = false
            try {
                preferencesRepository.setWeeklyGoalMinutes(minutes)
            } catch (error: Exception) {
                recordFailure(error)
            }
        }
    }

    fun enableDailyReminder() {
        val preferences = currentPreferences() ?: return
        viewModelScope.launch {
            operationError.value = false
            try {
                reminderScheduler.scheduleDaily(preferences.dailyReminderTime)
                preferencesRepository.setDailyReminderEnabled(true)
            } catch (error: Exception) {
                try {
                    reminderScheduler.cancel()
                } catch (cancelError: Exception) {
                    if (cancelError is CancellationException) throw cancelError
                }
                recordFailure(error)
            }
        }
    }

    fun disableDailyReminder() {
        viewModelScope.launch {
            operationError.value = false
            try {
                preferencesRepository.setDailyReminderEnabled(false)
                reminderScheduler.cancel()
            } catch (error: Exception) {
                recordFailure(error)
            }
        }
    }

    fun setDailyReminderTime(time: LocalTime) {
        val previous = currentPreferences() ?: return
        viewModelScope.launch {
            operationError.value = false
            try {
                if (previous.dailyReminderEnabled) {
                    reminderScheduler.scheduleDaily(time)
                }
                preferencesRepository.setDailyReminderTime(time)
            } catch (error: Exception) {
                if (previous.dailyReminderEnabled) {
                    try {
                        reminderScheduler.scheduleDaily(previous.dailyReminderTime)
                    } catch (restoreError: Exception) {
                        if (restoreError is CancellationException) throw restoreError
                    }
                }
                recordFailure(error)
            }
        }
    }

    fun clearOperationError() {
        operationError.value = false
    }

    /**
     * Writes the export to a destination the system's save dialog already returned, so by the time
     * this runs the user has chosen where their data goes.
     */
    fun exportData(destination: Uri) {
        viewModelScope.launch {
            exportStatus.value = DataExportStatus.RUNNING
            exportStatus.value = try {
                dataExporter.exportTo(destination)
                DataExportStatus.DONE
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                DataExportStatus.FAILED
            }
        }
    }

    fun clearExportStatus() {
        exportStatus.value = DataExportStatus.IDLE
    }

    private fun currentPreferences(): MindoraPreferences? =
        (uiState.value as? SettingsUiState.Content)?.preferences

    private fun recordFailure(error: Exception) {
        if (error is CancellationException) throw error
        operationError.value = true
    }
}
