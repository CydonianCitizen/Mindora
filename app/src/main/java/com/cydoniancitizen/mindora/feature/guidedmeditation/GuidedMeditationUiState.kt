package com.cydoniancitizen.mindora.feature.guidedmeditation

import com.cydoniancitizen.mindora.core.content.ResolvedGuidedMeditation
import com.cydoniancitizen.mindora.core.media.PlaybackFailureResult
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import java.time.Duration

data class GuidedMeditationDetails(
    val pathId: String,
    val stepId: String,
    val title: String,
    val description: String,
    val plannedDuration: Duration,
)

internal fun ResolvedGuidedMeditation.toDetails() = GuidedMeditationDetails(
    pathId = pathId,
    stepId = step.id,
    title = step.title,
    description = step.description,
    plannedDuration = Duration.ofSeconds(step.durationSeconds.toLong()),
)

sealed interface GuidedMeditationUiState {
    data object LoadingContent : GuidedMeditationUiState
    data object Unavailable : GuidedMeditationUiState
    data object ContentFailed : GuidedMeditationUiState

    sealed interface WithContent : GuidedMeditationUiState {
        val meditation: GuidedMeditationDetails
    }

    data class Ready(
        override val meditation: GuidedMeditationDetails,
    ) : WithContent

    data class Preparing(
        override val meditation: GuidedMeditationDetails,
    ) : WithContent

    data class Playing(
        override val meditation: GuidedMeditationDetails,
        val position: Duration,
        val totalDuration: Duration,
    ) : WithContent

    data class Paused(
        override val meditation: GuidedMeditationDetails,
        val position: Duration,
        val totalDuration: Duration,
    ) : WithContent

    data class Saving(
        override val meditation: GuidedMeditationDetails,
        val activeDuration: Duration,
    ) : WithContent

    data class Finished(
        override val meditation: GuidedMeditationDetails,
        val status: MindfulnessSessionStatus,
        val activeDuration: Duration,
    ) : WithContent

    data class SaveFailed(
        override val meditation: GuidedMeditationDetails,
        val activeDuration: Duration,
    ) : WithContent

    data class PlaybackFailed(
        override val meditation: GuidedMeditationDetails,
        val result: PlaybackFailureResult,
        val activeDuration: Duration,
    ) : WithContent
}
