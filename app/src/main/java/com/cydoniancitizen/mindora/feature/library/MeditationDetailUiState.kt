package com.cydoniancitizen.mindora.feature.library

import com.cydoniancitizen.mindora.core.content.model.MeditationLevel

sealed interface MeditationDetailUiState {
    data object Loading : MeditationDetailUiState
    data object Unavailable : MeditationDetailUiState
    data object Error : MeditationDetailUiState

    data class Content(
        val id: String,
        val title: String,
        val description: String,
        val technique: String,
        val category: String,
        val goal: String,
        val durationMinutes: Int,
        val level: MeditationLevel,
        val steps: List<String>,
        val safetyNotes: String?,
        val tags: List<String>,
    ) : MeditationDetailUiState
}
