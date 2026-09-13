package com.cydoniancitizen.mindora.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MeditationLibraryRepository
import com.cydoniancitizen.mindora.core.content.model.LibraryMeditation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: MeditationLibraryRepository,
) : ViewModel() {
    private var meditations: List<LibraryMeditation> = emptyList()
    private var selectedCategoryId: String? = null

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Null shows the whole library, which is also what re-selecting the current category does. */
    fun selectCategory(categoryId: String?) {
        selectedCategoryId = categoryId
        if (_uiState.value is LibraryUiState.Content) {
            _uiState.value = content()
        }
    }

    fun retry() {
        _uiState.value = LibraryUiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                meditations = libraryRepository.getMeditations()
                _uiState.value = content()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = LibraryUiState.Error
            }
        }
    }

    private fun content(): LibraryUiState.Content = LibraryUiState.Content(
        meditations = meditations
            .filter { selectedCategoryId == null || it.category.id == selectedCategoryId }
            .map { meditation ->
                MeditationCardUiModel(
                    id = meditation.id,
                    title = meditation.title,
                    description = meditation.description,
                    category = meditation.category.label,
                    goal = meditation.goal.label,
                    durationMinutes = meditation.durationMinutes,
                    level = meditation.level,
                )
            },
        // Categories come from the catalogue, so a new one shows up here on its own.
        categories = meditations.map { it.category }.distinct(),
        selectedCategoryId = selectedCategoryId,
    )
}
