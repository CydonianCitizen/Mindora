package com.cydoniancitizen.mindora.feature.freemeditation

import com.cydoniancitizen.mindora.core.content.model.MeditationStep
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import java.time.Duration
import java.time.Instant

internal object FreeMeditationDurations {
    val options: List<Duration> = listOf(5L, 10L, 15L, 20L, 30L).map(Duration::ofMinutes)
    val default: Duration = Duration.ofMinutes(10)
}

/** [pathId] is null for a library meditation: it stands on its own rather than inside a path. */
data class LinkedFreeMeditationDetails(
    val pathId: String?,
    val stepId: String,
    val title: String,
    val description: String,
    val plannedDuration: Duration,
    /** The instructions of a library meditation, in order. Empty for a path step, which has none. */
    val steps: List<MeditationStep> = emptyList(),
)

/**
 * The instruction that belongs to this moment of the session.
 *
 * A step that states its own length holds the screen for exactly that long; the steps that state
 * nothing share what is left, in equal parts. So a practice paced by its own text is paced by its
 * text, and one that says nothing about timing still moves at a steady rhythm. The catalogue is
 * validated so the stated lengths always fit, which is why nothing here has to rescale them.
 *
 * Returns null when there is nothing to say: a session with no steps, or a zero duration.
 */
fun List<MeditationStep>.stepCue(
    remainingDuration: Duration,
    plannedDuration: Duration,
): String? {
    val plannedMillis = plannedDuration.toMillis()
    if (isEmpty() || plannedMillis <= 0L) return null
    val statedMillis = sumOf { (it.durationSeconds ?: 0) * MILLIS_PER_SECOND }
    val openSteps = count { it.durationSeconds == null }
    val openShareMillis = if (openSteps > 0) {
        ((plannedMillis - statedMillis) / openSteps).coerceAtLeast(0L)
    } else {
        0L
    }
    val elapsedMillis = (plannedMillis - remainingDuration.toMillis()).coerceIn(0L, plannedMillis)
    var endMillis = 0L
    for (step in this) {
        endMillis += step.durationSeconds?.times(MILLIS_PER_SECOND) ?: openShareMillis
        if (elapsedMillis < endMillis) return step.text
    }
    return last().text
}

private const val MILLIS_PER_SECOND = 1_000L

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
        val steps: List<MeditationStep> = emptyList(),
    ) : FreeMeditationUiState

    data class Paused(
        val plannedDuration: Duration,
        val startedAt: Instant,
        val activeDuration: Duration,
        val remainingDuration: Duration,
        val confirmEnd: Boolean = false,
        val steps: List<MeditationStep> = emptyList(),
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
