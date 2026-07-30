package com.cydoniancitizen.mindora.feature.home

import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is loading`() = runTest {
        val viewModel = viewModel()

        assertSame(HomeUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
    }

    @Test
    fun `disabled goal produces content with practiced time`() = runTest {
        val sessions = MutableStateFlow(listOf(session("one", 5)))
        val viewModel = viewModel(sessions = sessions)

        advanceUntilIdle()

        assertEquals(
            HomeUiState.Content(Duration.ofMinutes(5), null, 0f, false),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `room and preference updates recalculate enabled goal`() = runTest {
        val sessions = MutableStateFlow(emptyList<MindfulnessSession>())
        val preferences = FakePreferencesRepository()
        val viewModel = viewModel(sessions, preferences)
        advanceUntilIdle()

        preferences.setWeeklyGoalMinutes(60)
        sessions.value = listOf(session("completed", 20))
        advanceUntilIdle()
        assertEquals(1f / 3f, (viewModel.uiState.value as HomeUiState.Content).progressFraction)

        sessions.value = listOf(
            session("completed", 20),
            session("interrupted", 40, MindfulnessSessionStatus.INTERRUPTED),
        )
        advanceUntilIdle()
        val reached = viewModel.uiState.value as HomeUiState.Content
        assertEquals(Duration.ofMinutes(60), reached.practicedDuration)
        assertTrue(reached.isReached)
        assertEquals(1f, reached.progressFraction)
        assertEquals(listOf(60), preferences.goalWrites)
    }

    @Test
    fun `repository failure produces error`() = runTest {
        val failingSessions = flow<List<MindfulnessSession>> { error("sessions") }
        val viewModel = viewModel(sessions = failingSessions)

        advanceUntilIdle()

        assertSame(HomeUiState.Error, viewModel.uiState.value)
    }

    private fun viewModel(
        sessions: Flow<List<MindfulnessSession>> = MutableStateFlow(emptyList()),
        preferences: FakePreferencesRepository = FakePreferencesRepository(),
    ) = HomeViewModel(
        sessionRepository = FakeSessionRepository(sessions),
        preferencesRepository = preferences,
        timeSource = object : SessionTimeSource {
            override fun nowInstant(): Instant = NOW
            override fun elapsedRealtimeMillis(): Long = 0
        },
    )

    private class FakeSessionRepository(
        private val sessions: Flow<List<MindfulnessSession>>,
    ) : MindfulnessSessionRepository {
        override fun observeSessions(): Flow<List<MindfulnessSession>> = sessions
        override suspend fun addSession(session: MindfulnessSession) = Unit
    }

    private class FakePreferencesRepository : MindoraPreferencesRepository {
        private val state = MutableStateFlow(MindoraPreferences())
        val goalWrites = mutableListOf<Int?>()
        override val preferences: Flow<MindoraPreferences> = state

        override suspend fun setWeeklyGoalMinutes(minutes: Int?) {
            goalWrites += minutes
            state.value = state.value.copy(weeklyGoalMinutes = minutes)
        }

        override suspend fun setDailyReminderEnabled(enabled: Boolean) {
            state.value = state.value.copy(dailyReminderEnabled = enabled)
        }

        override suspend fun setDailyReminderTime(time: LocalTime) {
            state.value = state.value.copy(dailyReminderTime = time)
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-07-29T12:00:00Z")

        fun session(
            id: String,
            minutes: Long,
            status: MindfulnessSessionStatus = MindfulnessSessionStatus.COMPLETED,
        ) = MindfulnessSession(
            id = id,
            type = MindfulnessSessionType.FREE_MEDITATION,
            status = status,
            sourcePathId = null,
            sourceStepId = null,
            startedAt = NOW,
            activeDuration = Duration.ofMinutes(minutes),
            plannedDuration = null,
        )
    }
}
