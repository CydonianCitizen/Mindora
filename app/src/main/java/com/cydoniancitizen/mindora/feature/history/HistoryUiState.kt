package com.cydoniancitizen.mindora.feature.history

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    data class Content(
        val sessions: List<MindfulnessSession>,
    ) : HistoryUiState

    data object Error : HistoryUiState
}
