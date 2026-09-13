package com.cydoniancitizen.mindora.feature.whitenoise

import com.cydoniancitizen.mindora.core.media.GuidedMeditationPlayback
import com.cydoniancitizen.mindora.core.media.PlaybackFailureResult
import com.cydoniancitizen.mindora.core.media.GuidedPlaybackState
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WhiteNoiseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state offers the four durations with ten minutes selected`() = runTest {
        val viewModel = WhiteNoiseViewModel(FakePlayback())
        advanceUntilIdle()

        val setup = viewModel.uiState.value as WhiteNoiseUiState.Setup
        assertEquals(Duration.ofMinutes(10), setup.selectedDuration)
        assertEquals(
            listOf(5L, 10L, 20L, 30L).map(Duration::ofMinutes),
            setup.availableDurations,
        )
    }

    @Test
    fun `start plays the white noise sound for the selected duration`() = runTest {
        val playback = FakePlayback()
        val viewModel = WhiteNoiseViewModel(playback)
        advanceUntilIdle()

        viewModel.selectDuration(Duration.ofMinutes(20))
        viewModel.start()

        assertEquals(listOf("white-noise" to Duration.ofMinutes(20)), playback.started)
    }

    @Test
    fun `an unknown duration is ignored`() = runTest {
        val viewModel = WhiteNoiseViewModel(FakePlayback())
        advanceUntilIdle()

        viewModel.selectDuration(Duration.ofMinutes(7))

        assertEquals(
            Duration.ofMinutes(10),
            (viewModel.uiState.value as WhiteNoiseUiState.Setup).selectedDuration,
        )
    }

    @Test
    fun `playing the sound shows the running state with the chosen total`() = runTest {
        val playback = FakePlayback()
        val viewModel = WhiteNoiseViewModel(playback)
        advanceUntilIdle()
        viewModel.selectDuration(Duration.ofMinutes(5))
        viewModel.start()

        playback.mutableState.value = GuidedPlaybackState.Playing(
            stepId = "white-noise",
            position = Duration.ofSeconds(3),
            mediaDuration = Duration.ofSeconds(30),
        )
        advanceUntilIdle()

        val running = viewModel.uiState.value as WhiteNoiseUiState.Running
        assertEquals(Duration.ofMinutes(5), running.total)
        assertTrue(!running.preparing)
    }

    @Test
    fun `a saved session surfaces as finished`() = runTest {
        val playback = FakePlayback()
        val viewModel = WhiteNoiseViewModel(playback)
        advanceUntilIdle()

        playback.mutableState.value = GuidedPlaybackState.Finished(
            stepId = "white-noise",
            status = MindfulnessSessionStatus.COMPLETED,
            activeDuration = Duration.ofMinutes(10),
        )
        advanceUntilIdle()

        val finished = viewModel.uiState.value as WhiteNoiseUiState.Finished
        assertEquals(MindfulnessSessionStatus.COMPLETED, finished.status)
        assertEquals(Duration.ofMinutes(10), finished.activeDuration)
    }

    @Test
    fun `stop asks the service to end the session`() = runTest {
        val playback = FakePlayback()
        val viewModel = WhiteNoiseViewModel(playback)
        advanceUntilIdle()

        viewModel.stop()

        assertEquals(1, playback.endCount)
    }

    @Test
    fun `foreign content and nullable failures preserve ownership`() = runTest {
        val playback = FakePlayback()
        val viewModel = WhiteNoiseViewModel(playback)
        advanceUntilIdle()
        listOf(
            GuidedPlaybackState.Preparing("other", Duration.ZERO, null),
            GuidedPlaybackState.Playing("other", Duration.ZERO, null),
            GuidedPlaybackState.Paused("other", Duration.ZERO, null),
            GuidedPlaybackState.Saving("other", Duration.ZERO),
            GuidedPlaybackState.Finished("other", MindfulnessSessionStatus.COMPLETED, Duration.ZERO),
            GuidedPlaybackState.SaveFailed("other", Duration.ZERO),
            GuidedPlaybackState.PlaybackFailed("other", PlaybackFailureResult.NOTHING_SAVED, Duration.ZERO),
        ).forEach { state ->
            playback.mutableState.value = state
            advanceUntilIdle()
            assertTrue("Unexpected mapping for $state", viewModel.uiState.value is WhiteNoiseUiState.Setup)
        }
        playback.mutableState.value = GuidedPlaybackState.PlaybackFailed(
            null, PlaybackFailureResult.NOTHING_SAVED, Duration.ZERO,
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is WhiteNoiseUiState.Setup)
    }

    @Test
    fun `all owned states retain their screen mapping`() = runTest {
        val playback = FakePlayback()
        val viewModel = WhiteNoiseViewModel(playback)
        advanceUntilIdle()
        listOf(
            GuidedPlaybackState.Preparing("white-noise", Duration.ZERO, null) to WhiteNoiseUiState.Running::class,
            GuidedPlaybackState.Playing("white-noise", Duration.ZERO, null) to WhiteNoiseUiState.Running::class,
            GuidedPlaybackState.Paused("white-noise", Duration.ZERO, null) to WhiteNoiseUiState.Running::class,
            GuidedPlaybackState.Saving("white-noise", Duration.ZERO) to WhiteNoiseUiState.Saving::class,
            GuidedPlaybackState.Finished("white-noise", MindfulnessSessionStatus.COMPLETED, Duration.ZERO) to WhiteNoiseUiState.Finished::class,
            GuidedPlaybackState.SaveFailed("white-noise", Duration.ZERO) to WhiteNoiseUiState.SaveFailed::class,
            GuidedPlaybackState.PlaybackFailed("white-noise", PlaybackFailureResult.NOTHING_SAVED, Duration.ZERO) to WhiteNoiseUiState.Setup::class,
        ).forEach { (state, expected) ->
            playback.mutableState.value = state
            advanceUntilIdle()
            assertEquals("Unexpected mapping for $state", expected, viewModel.uiState.value::class)
        }
    }

    private class FakePlayback : GuidedMeditationPlayback {
        val mutableState = MutableStateFlow<GuidedPlaybackState>(GuidedPlaybackState.Idle)
        override val state: StateFlow<GuidedPlaybackState> = mutableState
        val started = mutableListOf<Pair<String, Duration>>()
        var endCount = 0
        var retryCount = 0
        var discardCount = 0
        var clearCount = 0

        override fun start(stepId: String) = error("guided start is not used here")
        override fun startWhiteNoise(soundId: String, duration: Duration) {
            started += soundId to duration
        }
        override fun play() = Unit
        override fun pause() = Unit
        override fun end() { endCount++ }
        override fun retrySave() { retryCount++ }
        override fun discard() { discardCount++ }
        override fun clear() { clearCount++ }
        override fun release() = Unit
    }
}
