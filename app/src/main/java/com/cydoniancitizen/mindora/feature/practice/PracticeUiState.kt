package com.cydoniancitizen.mindora.feature.practice

import java.time.Duration

data class MindfulnessPathSummary(
    val id: String,
    val title: String,
    val description: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val progressFraction: Float,
)

data class WeeklyGoalUiModel(
    val practicedDuration: Duration = Duration.ZERO,
    val targetMinutes: Int? = null,
    val progressFraction: Float = 0f,
    val isReached: Boolean = false,
)

sealed interface PracticeUiState {
    val weeklyGoal: WeeklyGoalUiModel
        get() = WeeklyGoalUiModel()

    data object Loading : PracticeUiState

    data class Empty(
        override val weeklyGoal: WeeklyGoalUiModel = WeeklyGoalUiModel(),
    ) : PracticeUiState

    data class Content(
        val paths: List<MindfulnessPathSummary>,
        override val weeklyGoal: WeeklyGoalUiModel = WeeklyGoalUiModel(),
    ) : PracticeUiState

    data object Error : PracticeUiState
}
