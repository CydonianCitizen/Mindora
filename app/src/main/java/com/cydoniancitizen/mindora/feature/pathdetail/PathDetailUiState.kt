package com.cydoniancitizen.mindora.feature.pathdetail

import java.time.Duration

enum class PathStepType {
    GUIDED_MEDITATION,
    FREE_MEDITATION,
    BREATHING_EXERCISE,
}

data class PathStepUiModel(
    val id: String,
    val title: String,
    val description: String,
    val type: PathStepType,
    val ordinal: Int,
    val completed: Boolean,
    val displayDuration: Duration,
)

sealed interface PathDetailUiState {
    data object Loading : PathDetailUiState
    data object Unavailable : PathDetailUiState
    data object Error : PathDetailUiState

    data class Content(
        val title: String,
        val description: String,
        val completedSteps: Int,
        val totalSteps: Int,
        val progressFraction: Float,
        val steps: List<PathStepUiModel>,
    ) : PathDetailUiState
}
