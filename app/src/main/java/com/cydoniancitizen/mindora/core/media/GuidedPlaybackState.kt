package com.cydoniancitizen.mindora.core.media

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import java.time.Duration

sealed interface GuidedPlaybackState {
    data object Connecting : GuidedPlaybackState
    data object Disconnected : GuidedPlaybackState
    data object Idle : GuidedPlaybackState

    sealed interface WithMedia : GuidedPlaybackState {
        val stepId: String
        val position: Duration
        val mediaDuration: Duration?
    }

    data class Preparing(
        override val stepId: String,
        override val position: Duration,
        override val mediaDuration: Duration?,
    ) : WithMedia

    data class Playing(
        override val stepId: String,
        override val position: Duration,
        override val mediaDuration: Duration?,
    ) : WithMedia

    data class Paused(
        override val stepId: String,
        override val position: Duration,
        override val mediaDuration: Duration?,
    ) : WithMedia

    data class Saving(
        val stepId: String,
        val activeDuration: Duration,
    ) : GuidedPlaybackState

    data class Finished(
        val stepId: String,
        val status: MindfulnessSessionStatus,
        val activeDuration: Duration,
    ) : GuidedPlaybackState

    data class SaveFailed(
        val stepId: String,
        val activeDuration: Duration,
    ) : GuidedPlaybackState

    data class PlaybackFailed(
        val stepId: String?,
        val result: PlaybackFailureResult,
        val activeDuration: Duration,
    ) : GuidedPlaybackState
}

enum class PlaybackFailureResult {
    NOTHING_SAVED,
    INTERRUPTED_SAVED,
}
