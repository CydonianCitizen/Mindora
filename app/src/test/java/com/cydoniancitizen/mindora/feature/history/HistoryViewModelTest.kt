package com.cydoniancitizen.mindora.feature.history

import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `sessions produce content state ordered newest first and grouped by month`() = runTest {
        val sessions = listOf(olderSession, newerSession)
        val viewModel = HistoryViewModel(FakeSessionRepository(flowOf(sessions)))

        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Content
        assertEquals(HistoryFilter.ALL, state.selectedFilter)
        assertEquals(1, state.monthGroups.size)
        assertEquals("January 2026", state.monthGroups[0].monthYearLabel)
        assertEquals(2, state.monthGroups[0].sessions.size)
        assertEquals("newer", state.monthGroups[0].sessions[0].session.id)
        assertEquals("older", state.monthGroups[0].sessions[1].session.id)
    }

    @Test
    fun `sessions in different months produce multiple month groups ordered newest first`() = runTest {
        val janSession = testSession("jan", "2026-01-15T10:00:00Z", MindfulnessSessionType.FREE_MEDITATION)
        val febSession = testSession("feb", "2026-02-10T10:00:00Z", MindfulnessSessionType.BREATHING_EXERCISE)
        val viewModel = HistoryViewModel(FakeSessionRepository(flowOf(listOf(janSession, febSession))))

        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Content
        assertEquals(2, state.monthGroups.size)
        assertEquals("February 2026", state.monthGroups[0].monthYearLabel)
        assertEquals("January 2026", state.monthGroups[1].monthYearLabel)
    }

    @Test
    fun `filtering by guided meditation returns only guided sessions`() = runTest {
        val freeSession = testSession("free", "2026-01-15T10:00:00Z", MindfulnessSessionType.FREE_MEDITATION)
        val guidedSession = testSession("guided", "2026-01-16T10:00:00Z", MindfulnessSessionType.GUIDED_MEDITATION)
        val viewModel = HistoryViewModel(FakeSessionRepository(flowOf(listOf(freeSession, guidedSession))))

        advanceUntilIdle()
        viewModel.selectFilter(HistoryFilter.GUIDED_MEDITATION)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Content
        assertEquals(HistoryFilter.GUIDED_MEDITATION, state.selectedFilter)
        assertEquals(1, state.monthGroups[0].sessions.size)
        assertEquals("guided", state.monthGroups[0].sessions[0].session.id)
    }

    @Test
    fun `filtering with no matching sessions returns empty monthGroups`() = runTest {
        val freeSession = testSession("free", "2026-01-15T10:00:00Z", MindfulnessSessionType.FREE_MEDITATION)
        val viewModel = HistoryViewModel(FakeSessionRepository(flowOf(listOf(freeSession))))

        advanceUntilIdle()
        viewModel.selectFilter(HistoryFilter.BREATHING_EXERCISE)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HistoryUiState.Content
        assertEquals(HistoryFilter.BREATHING_EXERCISE, state.selectedFilter)
        assertTrue(state.monthGroups.isEmpty())
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

    @Test
    fun `groupSessionsByMonth formats localized month and year`() {
        val session1 = testSession("s1", "2025-10-24T14:30:00Z", MindfulnessSessionType.FREE_MEDITATION)
        val session2 = testSession("s2", "2025-09-12T09:00:00Z", MindfulnessSessionType.BREATHING_EXERCISE)

        val groups = HistoryViewModel.groupSessionsByMonth(
            sessions = listOf(session1, session2),
            stepTitleMap = emptyMap(),
            zoneId = ZoneId.of("UTC"),
            locale = Locale.US,
        )

        assertEquals(2, groups.size)
        assertEquals("October 2025", groups[0].monthYearLabel)
        assertEquals("September 2025", groups[1].monthYearLabel)
    }

    private class FakeSessionRepository(
        private val sessions: Flow<List<MindfulnessSession>>,
    ) : MindfulnessSessionRepository {
        override fun observeSessions(): Flow<List<MindfulnessSession>> = sessions

        override suspend fun addSession(session: MindfulnessSession) = Unit
    }

    private companion object {
        val newerSession = testSession("newer", "2026-01-02T00:00:00Z", MindfulnessSessionType.FREE_MEDITATION)
        val olderSession = testSession("older", "2026-01-01T00:00:00Z", MindfulnessSessionType.FREE_MEDITATION)

        fun testSession(
            id: String,
            startedAt: String,
            type: MindfulnessSessionType = MindfulnessSessionType.FREE_MEDITATION,
        ) = MindfulnessSession(
            id = id,
            type = type,
            status = MindfulnessSessionStatus.COMPLETED,
            sourcePathId = null,
            sourceStepId = null,
            startedAt = Instant.parse(startedAt),
            activeDuration = Duration.ofMinutes(5),
            plannedDuration = null,
        )
    }
}
