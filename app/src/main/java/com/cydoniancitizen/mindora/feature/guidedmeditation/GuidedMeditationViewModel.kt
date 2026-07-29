package com.cydoniancitizen.mindora.feature.guidedmeditation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.ResolvedGuidedMeditation
import com.cydoniancitizen.mindora.core.content.findGuidedMeditation
import com.cydoniancitizen.mindora.core.media.GuidedMeditationPlayback
import com.cydoniancitizen.mindora.core.media.GuidedPlaybackState
import com.cydoniancitizen.mindora.core.media.PlaybackFailureResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class GuidedMeditationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: MindfulnessContentRepository,
    private val playback: GuidedMeditationPlayback,
) : ViewModel() {
    private val stepId = savedStateHandle.get<String>(STEP_ID_ARGUMENT)
    private val content = MutableStateFlow<ContentResolution>(ContentResolution.Loading)

    val uiState: StateFlow<GuidedMeditationUiState> = combine(
        content,
        playback.state,
        ::mapState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GuidedMeditationUiState.LoadingContent,
    )

    init {
        resolveContent()
    }

    fun start() {
        val state = uiState.value
        if (state is GuidedMeditationUiState.Ready ||
            state is GuidedMeditationUiState.PlaybackFailed &&
            state.result == PlaybackFailureResult.NOTHING_SAVED
        ) {
            stepId?.let(playback::start)
        }
    }

    fun play() = playback.play()

    fun pause() = playback.pause()

    fun end() = playback.end()

    fun retrySave() = playback.retrySave()

    fun discard() = playback.discard()

    fun clear() = playback.clear()

    override fun onCleared() {
        playback.release()
        super.onCleared()
    }

    private fun resolveContent() {
        val routeStepId = stepId
        if (routeStepId.isNullOrBlank()) {
            content.value = ContentResolution.Unavailable
            return
        }
        viewModelScope.launch {
            content.value = try {
                contentRepository.findGuidedMeditation(routeStepId)
                    ?.let(ContentResolution::Available)
                    ?: ContentResolution.Unavailable
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ContentResolution.Failed
            }
        }
    }

    private fun mapState(
        contentResolution: ContentResolution,
        playbackState: GuidedPlaybackState,
    ): GuidedMeditationUiState {
        val meditation = when (contentResolution) {
            ContentResolution.Loading -> return GuidedMeditationUiState.LoadingContent
            ContentResolution.Unavailable -> return GuidedMeditationUiState.Unavailable
            ContentResolution.Failed -> return GuidedMeditationUiState.ContentFailed
            is ContentResolution.Available -> contentResolution.meditation.toDetails()
        }
        fun matches(runtimeStepId: String?): Boolean = runtimeStepId == meditation.stepId
        fun total(mediaDuration: Duration?): Duration = mediaDuration ?: meditation.plannedDuration

        return when (playbackState) {
            GuidedPlaybackState.Connecting,
            GuidedPlaybackState.Idle,
            -> GuidedMeditationUiState.Ready(meditation)

            GuidedPlaybackState.Disconnected -> GuidedMeditationUiState.PlaybackFailed(
                meditation = meditation,
                result = PlaybackFailureResult.NOTHING_SAVED,
                activeDuration = Duration.ZERO,
            )

            is GuidedPlaybackState.Preparing -> if (matches(playbackState.stepId)) {
                GuidedMeditationUiState.Preparing(meditation)
            } else {
                GuidedMeditationUiState.Ready(meditation)
            }

            is GuidedPlaybackState.Playing -> if (matches(playbackState.stepId)) {
                GuidedMeditationUiState.Playing(
                    meditation,
                    playbackState.position,
                    total(playbackState.mediaDuration),
                )
            } else {
                GuidedMeditationUiState.Ready(meditation)
            }

            is GuidedPlaybackState.Paused -> if (matches(playbackState.stepId)) {
                GuidedMeditationUiState.Paused(
                    meditation,
                    playbackState.position,
                    total(playbackState.mediaDuration),
                )
            } else {
                GuidedMeditationUiState.Ready(meditation)
            }

            is GuidedPlaybackState.Saving -> if (matches(playbackState.stepId)) {
                GuidedMeditationUiState.Saving(meditation, playbackState.activeDuration)
            } else {
                GuidedMeditationUiState.Ready(meditation)
            }

            is GuidedPlaybackState.Finished -> if (matches(playbackState.stepId)) {
                GuidedMeditationUiState.Finished(
                    meditation,
                    playbackState.status,
                    playbackState.activeDuration,
                )
            } else {
                GuidedMeditationUiState.Ready(meditation)
            }

            is GuidedPlaybackState.SaveFailed -> if (matches(playbackState.stepId)) {
                GuidedMeditationUiState.SaveFailed(meditation, playbackState.activeDuration)
            } else {
                GuidedMeditationUiState.Ready(meditation)
            }

            is GuidedPlaybackState.PlaybackFailed -> if (
                playbackState.stepId == null || matches(playbackState.stepId)
            ) {
                GuidedMeditationUiState.PlaybackFailed(
                    meditation,
                    playbackState.result,
                    playbackState.activeDuration,
                )
            } else {
                GuidedMeditationUiState.Ready(meditation)
            }
        }
    }

    private sealed interface ContentResolution {
        data object Loading : ContentResolution
        data object Unavailable : ContentResolution
        data object Failed : ContentResolution
        data class Available(
            val meditation: ResolvedGuidedMeditation,
        ) : ContentResolution
    }

    companion object {
        const val STEP_ID_ARGUMENT = "stepId"
    }
}
