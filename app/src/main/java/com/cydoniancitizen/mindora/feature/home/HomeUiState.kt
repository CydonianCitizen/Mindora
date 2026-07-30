package com.cydoniancitizen.mindora.feature.home

import java.time.Duration

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val practicedDuration: Duration,
        val targetMinutes: Int?,
        val progressFraction: Float,
        val isReached: Boolean,
    ) : HomeUiState

    data object Error : HomeUiState
}
