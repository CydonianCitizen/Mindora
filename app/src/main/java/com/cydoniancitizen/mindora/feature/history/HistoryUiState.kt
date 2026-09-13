package com.cydoniancitizen.mindora.feature.history

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.YearMonth

/** [type] is the session type a filter keeps; null keeps every session. */
enum class HistoryFilter(val type: MindfulnessSessionType?) {
    ALL(null),
    GUIDED_MEDITATION(MindfulnessSessionType.GUIDED_MEDITATION),
    FREE_MEDITATION(MindfulnessSessionType.FREE_MEDITATION),
    BREATHING_EXERCISE(MindfulnessSessionType.BREATHING_EXERCISE),
    WHITE_NOISE(MindfulnessSessionType.WHITE_NOISE),
}

data class HistorySessionDisplayItem(
    val session: MindfulnessSession,
    val catalogueTitle: String?,
)

data class HistoryMonthGroup(
    val yearMonth: YearMonth,
    val sessions: List<HistorySessionDisplayItem>,
)

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    data class Content(
        val selectedFilter: HistoryFilter,
        val monthGroups: List<HistoryMonthGroup>,
    ) : HistoryUiState

    data object Error : HistoryUiState
}

