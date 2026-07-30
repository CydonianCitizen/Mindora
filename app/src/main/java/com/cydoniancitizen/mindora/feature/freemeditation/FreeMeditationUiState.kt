package com.cydoniancitizen.mindora.feature.freemeditation

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import java.time.Duration
import java.time.Instant

internal object FreeMeditationDurations {
    val options: List<Duration> = listOf(5L, 10L, 15L, 20L).map(Duration::ofMinutes)
    val default: Duration = Duration.ofMinutes(10)
}

data class LinkedFreeMeditationDetails(
    val pathId: String,
    val stepId: String,
    val title: String,
    val description: String,
    val plannedDuration: Duration,
)

sealed interface FreeMeditationUiState {
    data object LoadingContent : FreeMeditationUiState
    data object Unavailable : FreeMeditationUiState

    data class Setup(
        val linkedContent: LinkedFreeMeditationDetails? = null,
        val selectedDuration: Duration =
            linkedContent?.plannedDuration ?: FreeMeditationDurations.default,
        val availableDurations: List<Duration> =
            if (linkedContent == null) FreeMeditationDurations.options else emptyList(),
    ) : FreeMeditationUiState

    data class Running(
        val plannedDuration: Duration,
        val startedAt: Instant,
        val accumulatedActiveDuration: Duration,
        val resumedAtElapsedRealtimeMillis: Long,
        val activeDuration: Duration,
        val remainingDuration: Duration,
        val confirmEnd: Boolean = false,
    ) : FreeMeditationUiState

    data class Paused(
        val plannedDuration: Duration,
        val startedAt: Instant,
        val activeDuration: Duration,
        val remainingDuration: Duration,
        val confirmEnd: Boolean = false,
    ) : FreeMeditationUiState

    data class Saving(
        val pendingSession: MindfulnessSession,
    ) : FreeMeditationUiState

    data class Finished(
        val savedSession: MindfulnessSession,
    ) : FreeMeditationUiState

    data class SaveFailed(
        val pendingSession: MindfulnessSession,
    ) : FreeMeditationUiState
}
