package com.cydoniancitizen.mindora.feature.guidedmeditation

import androidx.lifecycle.SavedStateHandle
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.model.BreathingExerciseStep
import com.cydoniancitizen.mindora.core.content.model.FreeMeditationStep
import com.cydoniancitizen.mindora.core.content.model.GuidedMeditationStep
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.media.GuidedMeditationPlayback
import com.cydoniancitizen.mindora.core.media.GuidedPlaybackState
import com.cydoniancitizen.mindora.core.media.PlaybackFailureResult
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GuidedMeditationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is loading content`() = runTest {
        val viewModel = viewModel()

        assertSame(GuidedMeditationUiState.LoadingContent, viewModel.uiState.value)
        advanceUntilIdle()
    }

    @Test
    fun `valid global step resolves ready with containing path and fields`() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        val ready = viewModel.state<GuidedMeditationUiState.Ready>()
        assertEquals("path-one", ready.meditation.pathId)
        assertEquals("guided-one", ready.meditation.stepId)
        assertEquals("Guided title", ready.meditation.title)
        assertEquals("Guided description", ready.meditation.description)
        assertEquals(Duration.ofMinutes(5), ready.meditation.plannedDuration)
    }

    @Test
    fun `unknown and wrong step types are unavailable`() = runTest {
        listOf("missing", "breathing-one", "free-one").forEach { stepId ->
            val viewModel = viewModel(stepId = stepId)
            advanceUntilIdle()
            assertSame(GuidedMeditationUiState.Unavailable, viewModel.uiState.value)
        }
    }

    @Test
    fun `malformed repository failure becomes content error`() = runTest {
        val viewModel = viewModel(repository = FakeContentRepository(failsToLoad = true))

        advanceUntilIdle()

        assertSame(GuidedMeditationUiState.ContentFailed, viewModel.uiState.value)
    }

    @Test
    fun `playing paused saving and finished states map authoritative playback`() = runTest {
        val playback = FakePlayback()
        val viewModel = viewModel(playback = playback)
        advanceUntilIdle()

        playback.mutableState.value = GuidedPlaybackState.Playing(
            "guided-one",
            Duration.ofSeconds(12),
            Duration.ofMinutes(5),
        )
        advanceUntilIdle()
        assertEquals(Duration.ofSeconds(12), viewModel.state<GuidedMeditationUiState.Playing>().position)

        playback.mutableState.value = GuidedPlaybackState.Paused(
            "guided-one",
            Duration.ofSeconds(13),
            Duration.ofMinutes(5),
        )
        advanceUntilIdle()
        assertEquals(Duration.ofSeconds(13), viewModel.state<GuidedMeditationUiState.Paused>().position)

        playback.mutableState.value = GuidedPlaybackState.Saving(
            "guided-one",
            Duration.ofSeconds(20),
        )
        advanceUntilIdle()
        assertEquals(Duration.ofSeconds(20), viewModel.state<GuidedMeditationUiState.Saving>().activeDuration)

        playback.mutableState.value = GuidedPlaybackState.Finished(
            "guided-one",
            MindfulnessSessionStatus.COMPLETED,
            Duration.ofSeconds(21),
        )
        advanceUntilIdle()
        assertEquals(
            MindfulnessSessionStatus.COMPLETED,
            viewModel.state<GuidedMeditationUiState.Finished>().status,
        )
    }

    @Test
    fun `save and playback failures map without raw errors`() = runTest {
        val playback = FakePlayback()
        val viewModel = viewModel(playback = playback)
        advanceUntilIdle()

        playback.mutableState.value = GuidedPlaybackState.SaveFailed(
            "guided-one",
            Duration.ofSeconds(30),
        )
        advanceUntilIdle()
        assertEquals(Duration.ofSeconds(30), viewModel.state<GuidedMeditationUiState.SaveFailed>().activeDuration)

        playback.mutableState.value = GuidedPlaybackState.PlaybackFailed(
            "guided-one",
            PlaybackFailureResult.INTERRUPTED_SAVED,
            Duration.ofSeconds(30),
        )
        advanceUntilIdle()
        assertEquals(
            PlaybackFailureResult.INTERRUPTED_SAVED,
            viewModel.state<GuidedMeditationUiState.PlaybackFailed>().result,
        )
    }

    @Test
    fun `view model forwards only guided playback actions`() = runTest {
        val playback = FakePlayback()
        val viewModel = viewModel(playback = playback)
        advanceUntilIdle()

        viewModel.start()
        playback.mutableState.value = GuidedPlaybackState.Paused(
            "guided-one",
            Duration.ZERO,
            Duration.ofMinutes(5),
        )
        advanceUntilIdle()
        viewModel.play()
        viewModel.pause()
        viewModel.end()
        viewModel.retrySave()
        viewModel.discard()
        viewModel.clear()

        assertEquals(listOf("guided-one"), playback.started)
        assertEquals(1, playback.playCount)
        assertEquals(1, playback.pauseCount)
        assertEquals(1, playback.endCount)
        assertEquals(1, playback.retryCount)
        assertEquals(1, playback.discardCount)
        assertEquals(1, playback.clearCount)
    }

    private fun viewModel(
        stepId: String = "guided-one",
        repository: MindfulnessContentRepository = FakeContentRepository(),
        playback: FakePlayback = FakePlayback(),
    ) = GuidedMeditationViewModel(
        SavedStateHandle(mapOf(GuidedMeditationViewModel.STEP_ID_ARGUMENT to stepId)),
        repository,
        playback,
    )

    private inline fun <reified T : GuidedMeditationUiState>
        GuidedMeditationViewModel.state(): T {
        val state = uiState.value
        assertTrue("Expected ${T::class.java.simpleName}, was $state", state is T)
        return state as T
    }

    private class FakeContentRepository(
        private val failsToLoad: Boolean = false,
    ) : MindfulnessContentRepository {
        override suspend fun getPaths(): List<MindfulnessPath> {
            if (failsToLoad) error("bad catalogue")
            return listOf(path)
        }
    }

    @Test
    fun `foreign content and nullable failures preserve ownership`() = runTest {
        val playback = FakePlayback()
        val viewModel = viewModel(playback = playback)
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
            assertTrue("Unexpected mapping for $state", viewModel.uiState.value is GuidedMeditationUiState.Ready)
        }
        playback.mutableState.value = GuidedPlaybackState.PlaybackFailed(
            null, PlaybackFailureResult.NOTHING_SAVED, Duration.ZERO,
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is GuidedMeditationUiState.PlaybackFailed)
    }

    @Test
    fun `all owned states retain their screen mapping`() = runTest {
        val playback = FakePlayback()
        val viewModel = viewModel(playback = playback)
        advanceUntilIdle()
        listOf(
            GuidedPlaybackState.Preparing("guided-one", Duration.ZERO, null) to GuidedMeditationUiState.Preparing::class,
            GuidedPlaybackState.Playing("guided-one", Duration.ZERO, null) to GuidedMeditationUiState.Playing::class,
            GuidedPlaybackState.Paused("guided-one", Duration.ZERO, null) to GuidedMeditationUiState.Paused::class,
            GuidedPlaybackState.Saving("guided-one", Duration.ZERO) to GuidedMeditationUiState.Saving::class,
            GuidedPlaybackState.Finished("guided-one", MindfulnessSessionStatus.COMPLETED, Duration.ZERO) to GuidedMeditationUiState.Finished::class,
            GuidedPlaybackState.SaveFailed("guided-one", Duration.ZERO) to GuidedMeditationUiState.SaveFailed::class,
            GuidedPlaybackState.PlaybackFailed("guided-one", PlaybackFailureResult.NOTHING_SAVED, Duration.ZERO) to GuidedMeditationUiState.PlaybackFailed::class,
        ).forEach { (state, expected) ->
            playback.mutableState.value = state
            advanceUntilIdle()
            assertEquals("Unexpected mapping for $state", expected, viewModel.uiState.value::class)
        }
    }

    private class FakePlayback : GuidedMeditationPlayback {
        val mutableState = MutableStateFlow<GuidedPlaybackState>(GuidedPlaybackState.Idle)
        override val state: StateFlow<GuidedPlaybackState> = mutableState
        val started = mutableListOf<String>()
        var playCount = 0
        var pauseCount = 0
        var endCount = 0
        var retryCount = 0
        var discardCount = 0
        var clearCount = 0

        override fun start(stepId: String) { started += stepId }
        override fun startWhiteNoise(soundId: String, duration: java.time.Duration) {
            started += soundId
        }
        override fun play() { playCount++ }
        override fun pause() { pauseCount++ }
        override fun end() { endCount++ }
        override fun retrySave() { retryCount++ }
        override fun discard() { discardCount++ }
        override fun clear() { clearCount++ }
        override fun release() = Unit
    }

    private companion object {
        val path = MindfulnessPath(
            id = "path-one",
            title = "Path",
            description = "Path description",
            steps = listOf(
                GuidedMeditationStep(
                    id = "guided-one",
                    title = "Guided title",
                    description = "Guided description",
                    durationSeconds = 300,
                    audioAsset = "audio/guided.mp3",
                ),
                BreathingExerciseStep(
                    id = "breathing-one",
                    title = "Breathing",
                    description = "Breathe",
                    inhaleSeconds = 4,
                    holdAfterInhaleSeconds = 0,
                    exhaleSeconds = 6,
                    holdAfterExhaleSeconds = 0,
                    cycles = 5,
                ),
                FreeMeditationStep(
                    id = "free-one",
                    title = "Free",
                    description = "Free meditation",
                    suggestedDurationSeconds = 300,
                ),
            ),
        )
    }
}
