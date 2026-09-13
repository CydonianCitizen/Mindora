package com.cydoniancitizen.mindora.core.media

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import java.time.Duration

sealed interface GuidedPlaybackState {
    data object Connecting : GuidedPlaybackState
    data object Disconnected : GuidedPlaybackState
    data object Idle : GuidedPlaybackState

    sealed interface ForContent : GuidedPlaybackState {
        val stepId: String?
    }

    data class Preparing(
        override val stepId: String,
        val position: Duration,
        val mediaDuration: Duration?,
    ) : ForContent

    data class Playing(
        override val stepId: String,
        val position: Duration,
        val mediaDuration: Duration?,
    ) : ForContent

    data class Paused(
        override val stepId: String,
        val position: Duration,
        val mediaDuration: Duration?,
    ) : ForContent

    data class Saving(
        override val stepId: String,
        val activeDuration: Duration,
    ) : ForContent

    data class Finished(
        override val stepId: String,
        val status: MindfulnessSessionStatus,
        val activeDuration: Duration,
    ) : ForContent

    data class SaveFailed(
        override val stepId: String,
        val activeDuration: Duration,
    ) : ForContent

    data class PlaybackFailed(
        override val stepId: String?,
        val result: PlaybackFailureResult,
        val activeDuration: Duration,
    ) : ForContent
}

enum class PlaybackFailureResult {
    NOTHING_SAVED,
    INTERRUPTED_SAVED,
}

internal enum class PlaybackPhase {
    IDLE, PREPARING, PLAYING, PAUSED, SAVING, FINISHED, SAVE_FAILED, PLAYBACK_FAILED,
}
