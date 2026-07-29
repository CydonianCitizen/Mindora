package com.cydoniancitizen.mindora.feature.practice

import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath

sealed interface PracticeUiState {
    data object Loading : PracticeUiState

    data object Empty : PracticeUiState

    data class Content(
        val paths: List<MindfulnessPath>,
    ) : PracticeUiState

    data object Error : PracticeUiState
}
