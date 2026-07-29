package com.cydoniancitizen.mindora.feature.history

import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is loading`() = runTest {
        val viewModel = HistoryViewModel(FakeSessionRepository(flowOf(emptyList())))

        assertSame(HistoryUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
    }

    @Test
    fun `empty sessions produce empty state`() = runTest {
        val viewModel = HistoryViewModel(FakeSessionRepository(flowOf(emptyList())))

        advanceUntilIdle()

        assertSame(HistoryUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `sessions produce content state without changing order`() = runTest {
        val sessions = listOf(newerSession, olderSession)
        val viewModel = HistoryViewModel(FakeSessionRepository(flowOf(sessions)))

        advanceUntilIdle()

        assertEquals(HistoryUiState.Content(sessions), viewModel.uiState.value)
    }

    @Test
    fun `repository failure produces error state`() = runTest {
        val failingFlow = flow<List<MindfulnessSession>> {
            error("Test failure.")
        }
        val viewModel = HistoryViewModel(FakeSessionRepository(failingFlow))

        advanceUntilIdle()

        assertSame(HistoryUiState.Error, viewModel.uiState.value)
    }


    private class FakeSessionRepository(
        private val sessions: Flow<List<MindfulnessSession>>,
    ) : MindfulnessSessionRepository {
        override fun observeSessions(): Flow<List<MindfulnessSession>> = sessions

        override suspend fun addSession(session: MindfulnessSession) = Unit
    }

    private companion object {
        val newerSession = testSession("newer", "2026-01-02T00:00:00Z")
        val olderSession = testSession("older", "2026-01-01T00:00:00Z")

        fun testSession(id: String, startedAt: String) = MindfulnessSession(
            id = id,
            type = MindfulnessSessionType.FREE_MEDITATION,
            status = MindfulnessSessionStatus.COMPLETED,
            sourcePathId = null,
            sourceStepId = null,
            startedAt = Instant.parse(startedAt),
            activeDuration = Duration.ofMinutes(5),
            plannedDuration = null,
        )
    }
}
