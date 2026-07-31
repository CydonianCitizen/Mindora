package com.cydoniancitizen.mindora.feature.history

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession

enum class HistoryFilter {
    ALL,
    GUIDED_MEDITATION,
    FREE_MEDITATION,
    BREATHING_EXERCISE,
}

data class HistorySessionDisplayItem(
    val session: MindfulnessSession,
    val title: String,
)

data class HistoryMonthGroup(
    val monthYearLabel: String,
    val yearMonthKey: String,
    val sessions: List<HistorySessionDisplayItem>,
)

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    data class Content(
        val selectedFilter: HistoryFilter,
        val availableFilters: List<HistoryFilter>,
        val monthGroups: List<HistoryMonthGroup>,
        val totalSessionsCount: Int,
        val sessions: List<MindfulnessSession> = emptyList(),
    ) : HistoryUiState

    data object Error : HistoryUiState
}

