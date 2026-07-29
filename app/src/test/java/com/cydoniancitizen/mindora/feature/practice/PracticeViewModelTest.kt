package com.cydoniancitizen.mindora.feature.practice

import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
        val repository = FakeContentRepository { emptyList() }

        val viewModel = PracticeViewModel(repository)

        assertSame(PracticeUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
    }

    @Test
    fun `paths produce content state`() = runTest {
        val paths = listOf(testPath)
        val viewModel = PracticeViewModel(FakeContentRepository { paths })

        advanceUntilIdle()

        assertEquals(PracticeUiState.Content(paths), viewModel.uiState.value)
    }

    @Test
    fun `no paths produce empty state`() = runTest {
        val viewModel = PracticeViewModel(FakeContentRepository { emptyList() })

        advanceUntilIdle()

        assertSame(PracticeUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `repository failure produces error state`() = runTest {
        val viewModel = PracticeViewModel(
            FakeContentRepository {
                error("Test failure.")
            },
        )

        advanceUntilIdle()

        assertSame(PracticeUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `retry reloads after an error`() = runTest {
        val repository = FakeContentRepository {
            error("Test failure.")
        }
        val viewModel = PracticeViewModel(repository)
        advanceUntilIdle()
        repository.load = { listOf(testPath) }

        viewModel.retry()
        assertSame(PracticeUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(PracticeUiState.Content(listOf(testPath)), viewModel.uiState.value)
        assertEquals(2, repository.loadCount)
    }

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

    private companion object {
        val testPath = MindfulnessPath(
            id = "test-path",
            title = "Test path",
            description = "Test description.",
            steps = emptyList(),
        )
    }
}
