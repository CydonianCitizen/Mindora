package com.cydoniancitizen.mindora.feature.practice

import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.testsupport.TestMindfulnessCatalogue
import com.cydoniancitizen.mindora.testsupport.testSession
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is loading`() = runTest {
        val sessions = MutableSharedFlow<List<MindfulnessSession>>()
        val viewModel = viewModel(sessions = sessions)

        assertSame(PracticeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `empty catalogue becomes empty after session source emits`() = runTest {
        val sessions = MutableSharedFlow<List<MindfulnessSession>>()
        val viewModel = viewModel(paths = emptyList(), sessions = sessions)
        runCurrent()

        sessions.emit(emptyList())
        runCurrent()

        assertEquals(PracticeUiState.Empty(), viewModel.uiState.value)
    }

    @Test
    fun `empty catalogue preserves weekly goal data`() = runTest {
        val sessions = MutableSharedFlow<List<MindfulnessSession>>()
        val viewModel = PracticeViewModel(
            contentRepository = FakeContentRepository { emptyList() },
            sessionRepository = FakeSessionRepository(sessions),
            preferencesRepository = FakePreferencesRepository(
                MindoraPreferences(weeklyGoalMinutes = 30),
            ),
            timeSource = FixedTimeSource,
        )
        runCurrent()

        sessions.emit(listOf(testSession("completed")))
        runCurrent()

        val weeklyGoal = (viewModel.uiState.value as PracticeUiState.Empty).weeklyGoal
        assertEquals(Duration.ofSeconds(30), weeklyGoal.practicedDuration)
        assertEquals(30, weeklyGoal.targetMinutes)
        assertEquals(1f / 60f, weeklyGoal.progressFraction)
    }

    @Test
    fun `populated catalogue preserves order totals and derives progress`() = runTest {
        val sessions = MutableSharedFlow<List<MindfulnessSession>>()
        val viewModel = viewModel(sessions = sessions)
        runCurrent()

        sessions.emit(
            listOf(
                testSession("completed"),
                testSession(
                    "interrupted",
                    status = MindfulnessSessionStatus.INTERRUPTED,
                    stepId = "free-step",
                ),
            ),
        )
        runCurrent()

        val content = viewModel.uiState.value as PracticeUiState.Content
        assertEquals(listOf("first-path", "second-path"), content.paths.map { it.id })
        assertEquals(listOf(3, 1), content.paths.map { it.totalSteps })
        assertEquals(1, content.paths.first().completedSteps)
        assertEquals(1f / 3f, content.paths.first().progressFraction)
    }

    @Test
    fun `Room emissions update progress without reload`() = runTest {
        val sessions = MutableSharedFlow<List<MindfulnessSession>>()
        val contentRepository = FakeContentRepository { TestMindfulnessCatalogue.paths }
        val viewModel = viewModel(contentRepository = contentRepository, sessions = sessions)
        runCurrent()
        sessions.emit(emptyList())
        runCurrent()
        assertEquals(
            0,
            (viewModel.uiState.value as PracticeUiState.Content).paths.first().completedSteps,
        )

        sessions.emit(listOf(testSession("completed")))
        runCurrent()

        assertEquals(
            1,
            (viewModel.uiState.value as PracticeUiState.Content).paths.first().completedSteps,
        )
        assertEquals(1, contentRepository.loadCount)
    }

    @Test
    fun `refresh recalculates weekly progress when only time changes`() = runTest {
        val sessions = MutableStateFlow(listOf(testSession("completed")))
        val timeSource = MutableTimeSource(Instant.parse("2026-07-30T12:00:00Z"))
        val viewModel = viewModel(sessions = sessions, timeSource = timeSource)
        runCurrent()
        assertEquals(
            Duration.ofSeconds(30),
            (viewModel.uiState.value as PracticeUiState.Content).weeklyGoal.practicedDuration,
        )

        timeSource.now = Instant.parse("2026-08-10T12:00:00Z")
        viewModel.refreshWeeklyGoal()
        runCurrent()

        assertEquals(
            Duration.ZERO,
            (viewModel.uiState.value as PracticeUiState.Content).weeklyGoal.practicedDuration,
        )
    }

    @Test
    fun `catalogue and session failures produce error`() = runTest {
        val catalogueFailure = viewModel(
            contentRepository = FakeContentRepository { error("catalogue") },
        )
        advanceUntilIdle()
        assertSame(PracticeUiState.Error, catalogueFailure.uiState.value)

        val sessionFailure = PracticeViewModel(
            contentRepository = FakeContentRepository { TestMindfulnessCatalogue.paths },
            sessionRepository = FakeSessionRepository(
                flow {
                    error("sessions")
                },
            ),
            preferencesRepository = FakePreferencesRepository(MindoraPreferences()),
            timeSource = FixedTimeSource,
        )
        advanceUntilIdle()
        assertSame(PracticeUiState.Error, sessionFailure.uiState.value)
    }

    @Test
    fun `retry reloads both sources after error`() = runTest {
        val content = FakeContentRepository { error("first") }
        val sessions = MutableSharedFlow<List<MindfulnessSession>>()
        val viewModel = viewModel(contentRepository = content, sessions = sessions)
        advanceUntilIdle()
        content.load = { TestMindfulnessCatalogue.paths }

        viewModel.retry()
        assertSame(PracticeUiState.Loading, viewModel.uiState.value)
        runCurrent()
        sessions.emit(emptyList())
        runCurrent()

        assertEquals(2, content.loadCount)
        assertEquals(PracticeUiState.Content::class, viewModel.uiState.value::class)
    }

    private fun viewModel(
        paths: List<MindfulnessPath> = TestMindfulnessCatalogue.paths,
        sessions: Flow<List<MindfulnessSession>> = MutableSharedFlow(),
        contentRepository: FakeContentRepository = FakeContentRepository { paths },
        timeSource: SessionTimeSource = FixedTimeSource,
    ) = PracticeViewModel(
        contentRepository = contentRepository,
        sessionRepository = FakeSessionRepository(sessions),
        preferencesRepository = FakePreferencesRepository(MindoraPreferences()),
        timeSource = timeSource,
    )

    private class FakeContentRepository(
        var load: suspend () -> List<MindfulnessPath>,
    ) : MindfulnessContentRepository {
        var loadCount = 0
            private set

        override suspend fun getPaths(): List<MindfulnessPath> {
            loadCount += 1
            return load()
        }
    }

    private class FakeSessionRepository(
        private val sessions: Flow<List<MindfulnessSession>>,
    ) : MindfulnessSessionRepository {
        override fun observeSessions(): Flow<List<MindfulnessSession>> = sessions
        override suspend fun addSession(session: MindfulnessSession) = Unit
    }

    private class FakePreferencesRepository(
        initialValue: MindoraPreferences,
    ) : MindoraPreferencesRepository {
        private val state = MutableStateFlow(initialValue)
        override val preferences: Flow<MindoraPreferences> = state

        override suspend fun setWeeklyGoalMinutes(minutes: Int?) {
            state.value = state.value.copy(weeklyGoalMinutes = minutes)
        }

        override suspend fun setDailyReminderEnabled(enabled: Boolean) {
            state.value = state.value.copy(dailyReminderEnabled = enabled)
        }

        override suspend fun setDailyReminderTime(time: LocalTime) {
            state.value = state.value.copy(dailyReminderTime = time)
        }
    }

    private object FixedTimeSource : SessionTimeSource {
        override fun nowInstant(): Instant = Instant.parse("2026-07-30T12:00:00Z")
        override fun elapsedRealtimeMillis(): Long = 0L
    }

    private class MutableTimeSource(var now: Instant) : SessionTimeSource {
        override fun nowInstant(): Instant = now
        override fun elapsedRealtimeMillis(): Long = 0L
    }
}
