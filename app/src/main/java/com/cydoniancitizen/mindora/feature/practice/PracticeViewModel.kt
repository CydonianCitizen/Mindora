package com.cydoniancitizen.mindora.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.progress.calculatePathProgress
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val contentRepository: MindfulnessContentRepository,
    private val sessionRepository: MindfulnessSessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PracticeUiState>(PracticeUiState.Loading)
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadPaths()
    }

    fun retry() {
        loadPaths()
    }

    private fun loadPaths() {
        loadJob?.cancel()
        _uiState.value = PracticeUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val paths = contentRepository.getPaths()
                sessionRepository.observeSessions().collect { sessions ->
                    _uiState.value = if (paths.isEmpty()) {
                        PracticeUiState.Empty
                    } else {
                        PracticeUiState.Content(
                            paths.map { path ->
                                val progress = calculatePathProgress(path, sessions)
                                MindfulnessPathSummary(
                                    id = path.id,
                                    title = path.title,
                                    description = path.description,
                                    completedSteps = progress.completedCount,
                                    totalSteps = progress.totalCount,
                                    progressFraction = progress.fraction,
                                )
                            },
                        )
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) {
                    throw error
                }
                _uiState.value = PracticeUiState.Error
            }
        }
    }
}
