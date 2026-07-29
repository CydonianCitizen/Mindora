package com.cydoniancitizen.mindora.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: MindfulnessSessionRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = repository.observeSessions()
        .map<List<MindfulnessSession>, HistoryUiState> { sessions ->
            if (sessions.isEmpty()) {
                HistoryUiState.Empty
            } else {
                HistoryUiState.Content(sessions)
            }
        }
        .catch {
            emit(HistoryUiState.Error)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HistoryUiState.Loading,
        )
}
