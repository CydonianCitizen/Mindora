package com.cydoniancitizen.mindora.feature.practice

data class MindfulnessPathSummary(
    val id: String,
    val title: String,
    val description: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val progressFraction: Float,
)

sealed interface PracticeUiState {
    data object Loading : PracticeUiState

    data object Empty : PracticeUiState

    data class Content(
        val paths: List<MindfulnessPathSummary>,
    ) : PracticeUiState

    data object Error : PracticeUiState
}
