package com.cydoniancitizen.mindora.feature.freemeditation

import androidx.lifecycle.SavedStateHandle
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import com.cydoniancitizen.mindora.testsupport.TestMindfulnessCatalogue
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FreeMeditationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is setup with ten minute default`() {
        val viewModel = viewModel()

        val setup = viewModel.state<FreeMeditationUiState.Setup>()

        assertEquals(Duration.ofMinutes(10), setup.selectedDuration)
        assertEquals(
            listOf(5L, 10L, 15L, 20L).map(Duration::ofMinutes),
            setup.availableDurations,
        )
    }

    @Test
    fun `each production duration can be selected`() {
        val viewModel = viewModel()

        listOf(5L, 10L, 15L, 20L).forEach { minutes ->
            viewModel.selectDuration(Duration.ofMinutes(minutes))
            assertEquals(
                Duration.ofMinutes(minutes),
                viewModel.state<FreeMeditationUiState.Setup>().selectedDuration,
            )
        }
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

        val running = viewModel.state<FreeMeditationUiState.Running>()
        assertEquals(STARTED_AT, running.startedAt)
        assertEquals(42_000, running.resumedAtElapsedRealtimeMillis)
        assertEquals(1, time.instantReadCount)
        assertEquals(1, time.elapsedReadCount)
        viewModel.pause()
    }

    @Test
    fun `delayed refresh catches up from monotonic time`() {
        val time = FakeTimeSource(elapsedRealtimeMillis = 1_000)
        val viewModel = viewModel(time = time)
        viewModel.start()

        time.elapsedRealtimeMillis = 62_500
        viewModel.refreshTime()

        val running = viewModel.state<FreeMeditationUiState.Running>()
        assertEquals(Duration.ofMillis(61_500), running.activeDuration)
        assertEquals(Duration.ofMillis(538_500), running.remainingDuration)
        viewModel.pause()
    }

    @Test
    fun `pause freezes active duration and excludes paused time after resume`() {
        val time = FakeTimeSource(elapsedRealtimeMillis = 10_000)
        val viewModel = viewModel(time = time)
        viewModel.start()
        time.elapsedRealtimeMillis = 20_000

        viewModel.pause()
        time.elapsedRealtimeMillis = 50_000
        viewModel.refreshTime()

        assertEquals(
            Duration.ofSeconds(10),
            viewModel.state<FreeMeditationUiState.Paused>().activeDuration,
        )

        viewModel.resume()
        time.elapsedRealtimeMillis = 55_000
        viewModel.refreshTime()

        assertEquals(
            Duration.ofSeconds(15),
            viewModel.state<FreeMeditationUiState.Running>().activeDuration,
        )
        viewModel.pause()
    }

    @Test
    fun `natural completion saves one completed session clamped to plan`() = runTest {
        val repository = FakeSessionRepository()
        val time = FakeTimeSource(
            instant = STARTED_AT,
            elapsedRealtimeMillis = 1_000,
        )
        val viewModel = viewModel(repository, time)
        viewModel.selectDuration(Duration.ofMinutes(5))
        viewModel.start()

        time.elapsedRealtimeMillis = 313_000
        viewModel.refreshTime()
        viewModel.refreshTime()
        viewModel.confirmEnd()
        runCurrent()

        val session = repository.saved.single()
        assertEquals(MindfulnessSessionStatus.COMPLETED, session.status)
        assertEquals(Duration.ofMinutes(5), session.activeDuration)
        assertEquals(Duration.ofMinutes(5), session.plannedDuration)
        assertEquals(1, repository.attempts.size)
        assertFalse(viewModel.hasActiveTicker)
        assertEquals(session, viewModel.state<FreeMeditationUiState.Finished>().savedSession)
    }

    @Test
    fun `manual early end saves interrupted free meditation fields`() = runTest {
        val repository = FakeSessionRepository()
        val time = FakeTimeSource(
            instant = STARTED_AT,
            elapsedRealtimeMillis = 100,
        )
        val viewModel = viewModel(repository, time)
        viewModel.selectDuration(Duration.ofMinutes(15))
        viewModel.start()
        time.elapsedRealtimeMillis = 12_445

        viewModel.requestEnd()
        assertTrue(viewModel.state<FreeMeditationUiState.Running>().confirmEnd)
        viewModel.confirmEnd()
        runCurrent()

        val session = repository.saved.single()
        assertEquals(MindfulnessSessionStatus.INTERRUPTED, session.status)
        assertEquals(MindfulnessSessionType.FREE_MEDITATION, session.type)
        assertEquals(null, session.sourcePathId)
        assertEquals(null, session.sourceStepId)
        assertEquals(STARTED_AT, session.startedAt)
        assertEquals(Duration.ofMillis(12_345), session.activeDuration)
        assertEquals(Duration.ofMinutes(15), session.plannedDuration)
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
        assertEquals(
            Duration.ofMinutes(10),
            viewModel.state<FreeMeditationUiState.Setup>().selectedDuration,
        )
        assertFalse(viewModel.hasActiveTicker)
    }

    @Test
    fun `ending at or after plan saves completed`() = runTest {
        val repository = FakeSessionRepository()
        val time = FakeTimeSource(elapsedRealtimeMillis = 5_000)
        val viewModel = viewModel(repository, time)
        viewModel.selectDuration(Duration.ofMinutes(5))
        viewModel.start()
        time.elapsedRealtimeMillis += Duration.ofMinutes(5).toMillis()

        viewModel.requestEnd()
        runCurrent()

        assertEquals(
            MindfulnessSessionStatus.COMPLETED,
            repository.saved.single().status,
        )
    }

    @Test
    fun `save failure retains pending session and retry reuses its id`() = runTest {
        val repository = FakeSessionRepository(failAdds = true)
        val time = FakeTimeSource(elapsedRealtimeMillis = 1_000)
        val viewModel = viewModel(repository, time)
        viewModel.start()
        time.elapsedRealtimeMillis = 2_000
        viewModel.requestEnd()
        viewModel.confirmEnd()
        runCurrent()

        val failed = viewModel.state<FreeMeditationUiState.SaveFailed>()
        val pendingId = failed.pendingSession.id
        assertEquals(pendingId, repository.attempts.single().id)

        repository.failAdds = false
        viewModel.retrySave()
        viewModel.retrySave()
        runCurrent()

        assertEquals(listOf(pendingId, pendingId), repository.attempts.map { it.id })
        assertEquals(pendingId, repository.saved.single().id)
        assertEquals(
            pendingId,
            viewModel.state<FreeMeditationUiState.Finished>().savedSession.id,
        )
    }

    @Test
    fun `discard drops failed pending session without persistence`() = runTest {
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
        assertTrue(viewModel.uiState.value is FreeMeditationUiState.Setup)
    }

    @Test
    fun `duplicate runtime actions keep one ticker and one insertion`() = runTest {
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
    fun `wall clock changes do not affect active duration or original start`() = runTest {
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
    fun `valid linked free step resolves catalogue content with fixed duration`() = runTest {
        val viewModel = viewModel(stepId = "free-step")

        assertTrue(viewModel.uiState.value is FreeMeditationUiState.LoadingContent)
        advanceUntilIdle()

        val setup = viewModel.state<FreeMeditationUiState.Setup>()
        assertEquals("Free step", setup.linkedContent?.title)
        assertEquals("Free description.", setup.linkedContent?.description)
        assertEquals(Duration.ofSeconds(120), setup.selectedDuration)
        assertTrue(setup.availableDurations.isEmpty())

        viewModel.selectDuration(Duration.ofMinutes(20))
        assertEquals(
            Duration.ofSeconds(120),
            viewModel.state<FreeMeditationUiState.Setup>().selectedDuration,
        )
    }

    @Test
    fun `unknown guided and breathing linked steps are unavailable`() = runTest {
        listOf("missing", "guided-step", "breathing-step").forEach { stepId ->
            val viewModel = viewModel(stepId = stepId)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is FreeMeditationUiState.Unavailable)
        }
    }

    @Test
    fun `linked completed session persists catalogue identity and duration`() = runTest {
        val repository = FakeSessionRepository()
        val time = FakeTimeSource(elapsedRealtimeMillis = 1_000)
        val viewModel = viewModel(repository, time, stepId = "free-step")
        advanceUntilIdle()
        viewModel.start()
        time.elapsedRealtimeMillis += Duration.ofSeconds(120).toMillis()

        viewModel.refreshTime()
        runCurrent()

        val session = repository.saved.single()
        assertEquals(MindfulnessSessionStatus.COMPLETED, session.status)
        assertEquals("first-path", session.sourcePathId)
        assertEquals("free-step", session.sourceStepId)
        assertEquals(Duration.ofSeconds(120), session.plannedDuration)
    }

    @Test
    fun `linked interrupted retry preserves identity and UUID`() = runTest {
        val repository = FakeSessionRepository(failAdds = true)
        val time = FakeTimeSource(elapsedRealtimeMillis = 1_000)
        val viewModel = viewModel(repository, time, stepId = "free-step")
        advanceUntilIdle()
        viewModel.start()
        time.elapsedRealtimeMillis = 2_000
        viewModel.requestEnd()
        viewModel.confirmEnd()
        runCurrent()
        val pending = viewModel.state<FreeMeditationUiState.SaveFailed>().pendingSession

        repository.failAdds = false
        viewModel.retrySave()
        runCurrent()

        assertEquals(MindfulnessSessionStatus.INTERRUPTED, pending.status)
        assertEquals("first-path", pending.sourcePathId)
        assertEquals("free-step", pending.sourceStepId)
        assertEquals(listOf(pending.id, pending.id), repository.attempts.map { it.id })
    }

    private fun viewModel(
        repository: FakeSessionRepository = FakeSessionRepository(),
        time: FakeTimeSource = FakeTimeSource(),
        stepId: String? = null,
        contentRepository: MindfulnessContentRepository = FakeContentRepository(),
    ) = FreeMeditationViewModel(
        SavedStateHandle(stepId?.let { mapOf("stepId" to it) }.orEmpty()),
        contentRepository,
        repository,
        time,
    )

    private inline fun <reified T : FreeMeditationUiState> FreeMeditationViewModel.state(): T {
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
            if (failAdds) {
                error("Test persistence failure.")
            }
            saved += session
        }
    }

    private class FakeContentRepository : MindfulnessContentRepository {
        override suspend fun getPaths(): List<MindfulnessPath> =
            TestMindfulnessCatalogue.paths
    }

    private companion object {
        val STARTED_AT: Instant = Instant.parse("2026-07-29T08:00:00Z")
    }
}
