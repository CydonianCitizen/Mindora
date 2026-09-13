package com.cydoniancitizen.mindora.feature.whitenoise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.media.GuidedMeditationPlayback
import com.cydoniancitizen.mindora.core.media.GuidedPlaybackState
import com.cydoniancitizen.mindora.core.media.WhiteNoiseCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Drives a standalone White Noise session. It reuses the guided playback connection unchanged —
 * same service, same session, same command channel — and only maps the shared
 * [GuidedPlaybackState] onto the small [WhiteNoiseUiState] this screen needs.
 */
@HiltViewModel
class WhiteNoiseViewModel @Inject constructor(
    private val playback: GuidedMeditationPlayback,
) : ViewModel() {
    private val sound = WhiteNoiseCatalog.default
    private val selectedDuration = MutableStateFlow(WhiteNoiseDurations.default)

    val uiState: StateFlow<WhiteNoiseUiState> = combine(
        selectedDuration,
        playback.state,
        ::mapState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WhiteNoiseUiState.Setup(),
    )

    fun selectDuration(duration: Duration) {
        if (uiState.value is WhiteNoiseUiState.Setup && duration in WhiteNoiseDurations.options) {
            selectedDuration.value = duration
        }
    }

    fun start() {
        if (uiState.value is WhiteNoiseUiState.Setup) {
            playback.startWhiteNoise(sound.id, selectedDuration.value)
        }
    }

    fun stop() = playback.end()

    fun retrySave() = playback.retrySave()

    fun discard() = playback.discard()

    fun done() = playback.clear()

    override fun onCleared() {
        playback.release()
        super.onCleared()
    }

    private fun mapState(
        duration: Duration,
        playbackState: GuidedPlaybackState,
    ): WhiteNoiseUiState {
        val owner = (playbackState as? GuidedPlaybackState.ForContent)?.stepId
        if (owner != null && owner != sound.id) {
            return WhiteNoiseUiState.Setup(selectedDuration = duration)
        }
        return when (playbackState) {
            GuidedPlaybackState.Connecting,
            GuidedPlaybackState.Disconnected,
            GuidedPlaybackState.Idle,
            -> WhiteNoiseUiState.Setup(selectedDuration = duration)

            is GuidedPlaybackState.Preparing ->
                WhiteNoiseUiState.Running(duration, preparing = true)

            is GuidedPlaybackState.Playing ->
                WhiteNoiseUiState.Running(duration)

            is GuidedPlaybackState.Paused ->
                WhiteNoiseUiState.Running(duration)

            is GuidedPlaybackState.Saving ->
                WhiteNoiseUiState.Saving

            is GuidedPlaybackState.Finished ->
                WhiteNoiseUiState.Finished(playbackState.status, playbackState.activeDuration)

            is GuidedPlaybackState.SaveFailed ->
                WhiteNoiseUiState.SaveFailed

            is GuidedPlaybackState.PlaybackFailed -> WhiteNoiseUiState.Setup(selectedDuration = duration)
        }
    }
}
