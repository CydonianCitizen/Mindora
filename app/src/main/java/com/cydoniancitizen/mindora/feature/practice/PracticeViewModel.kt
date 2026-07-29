package com.cydoniancitizen.mindora.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val contentRepository: MindfulnessContentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PracticeUiState>(PracticeUiState.Loading)
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        loadPaths()
    }

    fun retry() {
        loadPaths()
    }

    private fun loadPaths() {
        _uiState.value = PracticeUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val paths = contentRepository.getPaths()
                if (paths.isEmpty()) {
                    PracticeUiState.Empty
                } else {
                    PracticeUiState.Content(paths)
                }
            } catch (error: Exception) {
                if (error is CancellationException) {
                    throw error
                }
                PracticeUiState.Error
            }
        }
    }
}
