package com.cydoniancitizen.mindora.feature.breathing

import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BreathingExerciseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has fixed production configuration`() {
        val setup = viewModel().state<BreathingExerciseUiState.Setup>()

        assertEquals(Duration.ofSeconds(4), setup.config.inhaleDuration)
        assertEquals(Duration.ZERO, setup.config.holdAfterInhaleDuration)
        assertEquals(Duration.ofSeconds(6), setup.config.exhaleDuration)
        assertEquals(Duration.ZERO, setup.config.holdAfterExhaleDuration)
        assertEquals(5, setup.totalCycles)
        assertEquals(Duration.ofSeconds(50), setup.plannedDuration)
    }

    @Test
    fun `start captures wall and monotonic baselines once`() {
        val time = FakeTimeSource(
            instant = STARTED_AT,
            elapsedRealtimeMillis = 42_000,
        )
        val viewModel = viewModel(time = time)

        viewModel.start()
        viewModel.start()

        val running = viewModel.state<BreathingExerciseUiState.Running>()
        assertEquals(STARTED_AT, running.startedAt)
        assertEquals(42_000, running.resumedAtElapsedRealtimeMillis)
        assertEquals(BreathingPhase.INHALE, running.currentPhase)
        assertEquals(1, running.currentCycle)
        assertEquals(1, time.instantReadCount)
        assertEquals(1, time.elapsedReadCount)
        viewModel.pause()
    }

    @Test
    fun `delayed ticker refresh catches up without drift`() {
        val time = FakeTimeSource(elapsedRealtimeMillis = 1_000)
        val viewModel = viewModel(time = time)
        viewModel.start()

        time.elapsedRealtimeMillis = 27_500
        viewModel.refreshTime()

        val running = viewModel.state<BreathingExerciseUiState.Running>()
        assertEquals(Duration.ofMillis(26_500), running.activeDuration)
        assertEquals(BreathingPhase.EXHALE, running.currentPhase)
        assertEquals(3, running.currentCycle)
        assertEquals(Duration.ofMillis(3_500), running.phaseRemainingDuration)
        assertEquals(Duration.ofMillis(23_500), running.totalRemainingDuration)
        viewModel.pause()
    }

    @Test
    fun `pause freezes phase and excludes paused time after resume`() {
        val time = FakeTimeSource(elapsedRealtimeMillis = 10_000)
        val viewModel = viewModel(time = time)
        viewModel.start()
        time.elapsedRealtimeMillis = 13_500

        viewModel.pause()
        val paused = viewModel.state<BreathingExerciseUiState.Paused>()
        assertEquals(Duration.ofMillis(3_500), paused.activeDuration)
        assertEquals(BreathingPhase.INHALE, paused.currentPhase)
        assertEquals(Duration.ofMillis(500), paused.phaseRemainingDuration)
        assertFalse(viewModel.hasActiveTicker)

        time.elapsedRealtimeMillis = 43_500
        viewModel.refreshTime()
        assertEquals(
            Duration.ofMillis(3_500),
            viewModel.state<BreathingExerciseUiState.Paused>().activeDuration,
        )

        viewModel.resume()
        time.elapsedRealtimeMillis = 45_000
        viewModel.refreshTime()

        val resumed = viewModel.state<BreathingExerciseUiState.Running>()
        assertEquals(Duration.ofSeconds(5), resumed.activeDuration)
        assertEquals(BreathingPhase.EXHALE, resumed.currentPhase)
        assertEquals(Duration.ofSeconds(5), resumed.phaseRemainingDuration)
        viewModel.pause()
    }

    @Test
    fun `natural completion saves one completed breathing session`() = runTest {
        val repository = FakeSessionRepository()
        val time = FakeTimeSource(
            instant = STARTED_AT,
            elapsedRealtimeMillis = 1_000,
        )
        val viewModel = viewModel(repository, time)
        viewModel.start()

        time.elapsedRealtimeMillis = 56_000
        viewModel.refreshTime()
        viewModel.refreshTime()
        viewModel.confirmEnd()
        runCurrent()

        val session = repository.saved.single()
        assertEquals(MindfulnessSessionStatus.COMPLETED, session.status)
        assertEquals(MindfulnessSessionType.BREATHING_EXERCISE, session.type)
        assertEquals(null, session.sourcePathId)
        assertEquals(null, session.sourceStepId)
        assertEquals(STARTED_AT, session.startedAt)
        assertEquals(Duration.ofSeconds(50), session.activeDuration)
        assertEquals(Duration.ofSeconds(50), session.plannedDuration)
        assertEquals(1, repository.attempts.size)
        assertFalse(viewModel.hasActiveTicker)

        val finished = viewModel.state<BreathingExerciseUiState.Finished>()
        assertEquals(MindfulnessSessionStatus.COMPLETED, finished.savedSessionStatus)
        assertEquals(5, finished.completedCycles)
    }

    @Test
    fun `manual early ending saves exact interrupted session`() = runTest {
        val repository = FakeSessionRepository()
        val time = FakeTimeSource(
            instant = STARTED_AT,
            elapsedRealtimeMillis = 100,
        )
        val viewModel = viewModel(repository, time)
        viewModel.start()
        time.elapsedRealtimeMillis = 12_445

        viewModel.requestEnd()
        assertTrue(viewModel.state<BreathingExerciseUiState.Running>().confirmEnd)
        viewModel.confirmEnd()
        runCurrent()

        val session = repository.saved.single()
        assertEquals(MindfulnessSessionStatus.INTERRUPTED, session.status)
        assertEquals(MindfulnessSessionType.BREATHING_EXERCISE, session.type)
        assertEquals(null, session.sourcePathId)
        assertEquals(null, session.sourceStepId)
        assertEquals(Duration.ofMillis(12_345), session.activeDuration)
        assertEquals(Duration.ofSeconds(50), session.plannedDuration)
        assertTrue(session.id.isNotBlank())
    }

    @Test
    fun `ending at zero returns to setup without saving`() = runTest {
        val repository = FakeSessionRepository()
        val viewModel = viewModel(repository)
        viewModel.start()

        viewModel.requestEnd()
        viewModel.confirmEnd()
        runCurrent()

        assertTrue(repository.attempts.isEmpty())
        assertTrue(viewModel.uiState.value is BreathingExerciseUiState.Setup)
        assertFalse(viewModel.hasActiveTicker)
    }

    @Test
    fun `ending at completion saves completed`() = runTest {
        val repository = FakeSessionRepository()
        val time = FakeTimeSource(elapsedRealtimeMillis = 5_000)
        val viewModel = viewModel(repository, time)
        viewModel.start()
        time.elapsedRealtimeMillis += Duration.ofSeconds(50).toMillis()

        viewModel.requestEnd()
        runCurrent()

        assertEquals(
            MindfulnessSessionStatus.COMPLETED,
            repository.saved.single().status,
        )
    }

    @Test
    fun `save failure retry reuses pending UUID and succeeds once`() = runTest {
        val repository = FakeSessionRepository(failAdds = true)
        val time = FakeTimeSource(elapsedRealtimeMillis = 1_000)
        val viewModel = viewModel(repository, time)
        viewModel.start()
        time.elapsedRealtimeMillis = 2_000
        viewModel.requestEnd()
        viewModel.confirmEnd()
        runCurrent()

        val failed = viewModel.state<BreathingExerciseUiState.SaveFailed>()
        val pendingId = failed.pendingSession.id
        assertEquals(pendingId, repository.attempts.single().id)

        repository.failAdds = false
        viewModel.retrySave()
        viewModel.retrySave()
        runCurrent()

        assertEquals(listOf(pendingId, pendingId), repository.attempts.map { it.id })
        assertEquals(pendingId, repository.saved.single().id)
        assertTrue(viewModel.uiState.value is BreathingExerciseUiState.Finished)
    }

    @Test
    fun `discard clears failed session without saving`() = runTest {
        val repository = FakeSessionRepository(failAdds = true)
        val time = FakeTimeSource(elapsedRealtimeMillis = 10_000)
        val viewModel = viewModel(repository, time)
        viewModel.start()
        time.elapsedRealtimeMillis = 11_000
        viewModel.requestEnd()
        viewModel.confirmEnd()
        runCurrent()

        viewModel.discard()

        assertTrue(repository.saved.isEmpty())
        assertTrue(viewModel.uiState.value is BreathingExerciseUiState.Setup)
    }

    @Test
    fun `duplicate actions keep one ticker and one insertion`() = runTest {
        val repository = FakeSessionRepository()
        val time = FakeTimeSource(elapsedRealtimeMillis = 0)
        val viewModel = viewModel(repository, time)

        viewModel.start()
        viewModel.start()
        assertTrue(viewModel.hasActiveTicker)
        viewModel.pause()
        assertFalse(viewModel.hasActiveTicker)
        viewModel.resume()
        viewModel.resume()
        assertTrue(viewModel.hasActiveTicker)

        time.elapsedRealtimeMillis = 1_000
        viewModel.requestEnd()
        viewModel.confirmEnd()
        viewModel.confirmEnd()
        viewModel.retrySave()
        runCurrent()

        assertEquals(1, repository.attempts.size)
        assertFalse(viewModel.hasActiveTicker)
    }

    @Test
    fun `wall clock changes do not affect duration or original start`() = runTest {
        val repository = FakeSessionRepository()
        val time = FakeTimeSource(
            instant = STARTED_AT,
            elapsedRealtimeMillis = 8_000,
        )
        val viewModel = viewModel(repository, time)
        viewModel.start()
        time.instant = STARTED_AT.plus(Duration.ofDays(30))
        time.elapsedRealtimeMillis = 10_500

        viewModel.requestEnd()
        viewModel.confirmEnd()
        runCurrent()

        val session = repository.saved.single()
        assertEquals(STARTED_AT, session.startedAt)
        assertEquals(Duration.ofMillis(2_500), session.activeDuration)
        assertEquals(1, time.instantReadCount)
    }

    @Test
    fun `resume lifecycle recalculation catches up from monotonic time`() {
        val time = FakeTimeSource(elapsedRealtimeMillis = 2_000)
        val viewModel = viewModel(time = time)
        viewModel.start()

        time.elapsedRealtimeMillis = 46_250
        viewModel.refreshTime()

        val state = viewModel.state<BreathingExerciseUiState.Running>()
        assertEquals(Duration.ofMillis(44_250), state.activeDuration)
        assertEquals(5, state.currentCycle)
        assertEquals(BreathingPhase.EXHALE, state.currentPhase)
        viewModel.pause()
    }

    private fun viewModel(
        repository: FakeSessionRepository = FakeSessionRepository(),
        time: FakeTimeSource = FakeTimeSource(),
    ) = BreathingExerciseViewModel(repository, time)

    private inline fun <reified T : BreathingExerciseUiState>
        BreathingExerciseViewModel.state(): T {
        val state = uiState.value
        assertTrue("Expected ${T::class.java.simpleName}, was $state", state is T)
        return state as T
    }

    private class FakeTimeSource(
        var instant: Instant = STARTED_AT,
        var elapsedRealtimeMillis: Long = 0,
    ) : SessionTimeSource {
        var instantReadCount = 0
            private set
        var elapsedReadCount = 0
            private set

        override fun nowInstant(): Instant {
            instantReadCount += 1
            return instant
        }

        override fun elapsedRealtimeMillis(): Long {
            elapsedReadCount += 1
            return elapsedRealtimeMillis
        }
    }

    private class FakeSessionRepository(
        var failAdds: Boolean = false,
    ) : MindfulnessSessionRepository {
        val attempts = mutableListOf<MindfulnessSession>()
        val saved = mutableListOf<MindfulnessSession>()

        override fun observeSessions(): Flow<List<MindfulnessSession>> = flowOf(saved)

        override suspend fun addSession(session: MindfulnessSession) {
            attempts += session
            if (failAdds) error("Test persistence failure.")
            saved += session
        }
    }

    private companion object {
        val STARTED_AT: Instant = Instant.parse("2026-07-29T08:00:00Z")
    }
}
