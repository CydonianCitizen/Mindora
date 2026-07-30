package com.cydoniancitizen.mindora.feature.pathdetail

import androidx.lifecycle.SavedStateHandle
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import com.cydoniancitizen.mindora.testsupport.TestMindfulnessCatalogue
import com.cydoniancitizen.mindora.testsupport.testSession
import java.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PathDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is loading`() = runTest {
        val viewModel = viewModel()

        assertSame(PathDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `valid encoded path preserves step order types ordinals and durations`() = runTest {
        val sessions = MutableSharedFlow<List<MindfulnessSession>>()
        val viewModel = viewModel(pathId = "first%2Dpath", sessions = sessions)
        runCurrent()
        sessions.emit(emptyList())
        runCurrent()

        val content = viewModel.uiState.value as PathDetailUiState.Content
        assertEquals("First path", content.title)
        assertEquals("First path description.", content.description)
        assertEquals(
            listOf("guided-step", "free-step", "breathing-step"),
            content.steps.map { it.id },
        )
        assertEquals(listOf(1, 2, 3), content.steps.map { it.ordinal })
        assertEquals(
            listOf(
                PathStepType.GUIDED_MEDITATION,
                PathStepType.FREE_MEDITATION,
                PathStepType.BREATHING_EXERCISE,
            ),
            content.steps.map { it.type },
        )
        assertEquals(
            listOf(
                Duration.ofSeconds(180),
                Duration.ofSeconds(120),
                Duration.ofSeconds(20),
            ),
            content.steps.map { it.displayDuration },
        )
        assertEquals(listOf(false, false, false), content.steps.map { it.completed })
    }

    @Test
    fun `matching completed sessions update step completion automatically`() = runTest {
        val sessions = MutableSharedFlow<List<MindfulnessSession>>()
        val viewModel = viewModel(sessions = sessions)
        runCurrent()
        sessions.emit(emptyList())
        runCurrent()

        sessions.emit(
            listOf(
                testSession("first"),
                testSession("duplicate"),
                testSession(
                    "interrupted",
                    status = MindfulnessSessionStatus.INTERRUPTED,
                    stepId = "free-step",
                ),
            ),
        )
        runCurrent()

        val content = viewModel.uiState.value as PathDetailUiState.Content
        assertEquals(1, content.completedSteps)
        assertEquals(3, content.totalSteps)
        assertEquals(1f / 3f, content.progressFraction)
        assertEquals(listOf(true, false, false), content.steps.map { it.completed })
    }

    @Test
    fun `missing unknown and repository failures expose calm terminal states`() = runTest {
        val missing = viewModel(pathId = null)
        assertSame(PathDetailUiState.Unavailable, missing.uiState.value)

        val unknown = viewModel(pathId = "unknown")
        advanceUntilIdle()
        assertSame(PathDetailUiState.Unavailable, unknown.uiState.value)

        val catalogueFailure = viewModel(contentFails = true)
        advanceUntilIdle()
        assertSame(PathDetailUiState.Error, catalogueFailure.uiState.value)

        val sessionFailure = viewModel(
            sessions = flow {
                error("sessions")
            },
        )
        advanceUntilIdle()
        assertSame(PathDetailUiState.Error, sessionFailure.uiState.value)
    }

    private fun viewModel(
        pathId: String? = "first-path",
        sessions: Flow<List<MindfulnessSession>> = MutableSharedFlow(),
        contentFails: Boolean = false,
    ) = PathDetailViewModel(
        savedStateHandle = SavedStateHandle(
            pathId?.let { mapOf(PathDetailViewModel.PATH_ID_ARGUMENT to it) }.orEmpty(),
        ),
        contentRepository = object : MindfulnessContentRepository {
            override suspend fun getPaths(): List<MindfulnessPath> {
                if (contentFails) error("catalogue")
                return TestMindfulnessCatalogue.paths
            }
        },
        sessionRepository = object : MindfulnessSessionRepository {
            override fun observeSessions(): Flow<List<MindfulnessSession>> = sessions
            override suspend fun addSession(session: MindfulnessSession) = Unit
        },
    )
}
