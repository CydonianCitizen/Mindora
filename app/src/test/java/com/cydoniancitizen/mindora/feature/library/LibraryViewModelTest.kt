package com.cydoniancitizen.mindora.feature.library

import com.cydoniancitizen.mindora.core.content.MeditationLibraryRepository
import com.cydoniancitizen.mindora.core.content.model.LibraryMeditation
import com.cydoniancitizen.mindora.feature.practice.MainDispatcherRule
import com.cydoniancitizen.mindora.testsupport.TestMindfulnessCatalogue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `library loads every meditation with the categories it can be narrowed by`() = runTest {
        val viewModel = LibraryViewModel(FakeLibraryRepository())

        advanceUntilIdle()

        val state = viewModel.uiState.value as LibraryUiState.Content
        assertEquals(listOf("meditation-calm", "meditation-rest"), state.meditations.map { it.id })
        assertEquals(listOf("breath", "rest"), state.categories.map { it.id })
        assertNull(state.selectedCategoryId)
    }

    @Test
    fun `selecting a category narrows the list and keeps every category reachable`() = runTest {
        val viewModel = LibraryViewModel(FakeLibraryRepository())
        advanceUntilIdle()

        viewModel.selectCategory("rest")

        val state = viewModel.uiState.value as LibraryUiState.Content
        assertEquals(listOf("meditation-rest"), state.meditations.map { it.id })
        assertEquals("rest", state.selectedCategoryId)
        assertEquals(listOf("breath", "rest"), state.categories.map { it.id })
    }

    @Test
    fun `clearing the category brings the whole library back`() = runTest {
        val viewModel = LibraryViewModel(FakeLibraryRepository())
        advanceUntilIdle()
        viewModel.selectCategory("breath")

        viewModel.selectCategory(null)

        val state = viewModel.uiState.value as LibraryUiState.Content
        assertEquals(2, state.meditations.size)
        assertNull(state.selectedCategoryId)
    }

    @Test
    fun `a failing library reports an error that can be retried`() = runTest {
        val viewModel = LibraryViewModel(FakeLibraryRepository(failFirstRead = true))
        advanceUntilIdle()
        assertSame(LibraryUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, (viewModel.uiState.value as LibraryUiState.Content).meditations.size)
    }

    private class FakeLibraryRepository(
        private var failFirstRead: Boolean = false,
    ) : MeditationLibraryRepository {
        override suspend fun getMeditations(): List<LibraryMeditation> {
            if (failFirstRead) {
                failFirstRead = false
                error("Test catalogue failure.")
            }
            return TestMindfulnessCatalogue.meditations
        }
    }
}
