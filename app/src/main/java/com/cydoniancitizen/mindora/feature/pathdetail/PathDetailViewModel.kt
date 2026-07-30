package com.cydoniancitizen.mindora.feature.pathdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.decodeContentRouteId
import com.cydoniancitizen.mindora.core.content.findPath
import com.cydoniancitizen.mindora.core.content.model.BreathingExerciseStep
import com.cydoniancitizen.mindora.core.content.model.FreeMeditationStep
import com.cydoniancitizen.mindora.core.content.model.GuidedMeditationStep
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.progress.calculatePathProgress
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PathDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: MindfulnessContentRepository,
    private val sessionRepository: MindfulnessSessionRepository,
) : ViewModel() {
    private val pathId = savedStateHandle.get<String>(PATH_ID_ARGUMENT)
        ?.takeIf(String::isNotBlank)
    private val _uiState = MutableStateFlow<PathDetailUiState>(PathDetailUiState.Loading)
    val uiState: StateFlow<PathDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val requestedPathId = pathId
        if (requestedPathId == null) {
            _uiState.value = PathDetailUiState.Unavailable
            return
        }
        viewModelScope.launch {
            try {
                val paths = contentRepository.getPaths()
                val path = paths.findPath(requestedPathId)
                    ?: decodeContentRouteId(requestedPathId)?.let(paths::findPath)
                if (path == null) {
                    _uiState.value = PathDetailUiState.Unavailable
                    return@launch
                }
                sessionRepository.observeSessions().collect { sessions ->
                    _uiState.value = path.toUiState(sessions)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.value = PathDetailUiState.Error
            }
        }
    }

    private fun MindfulnessPath.toUiState(
        sessions: List<MindfulnessSession>,
    ): PathDetailUiState.Content {
        val progress = calculatePathProgress(this, sessions)
        return PathDetailUiState.Content(
            title = title,
            description = description,
            completedSteps = progress.completedCount,
            totalSteps = progress.totalCount,
            progressFraction = progress.fraction,
            steps = steps.mapIndexed { index, step ->
                val (type, duration) = when (step) {
                    is GuidedMeditationStep -> PathStepType.GUIDED_MEDITATION to
                        Duration.ofSeconds(step.durationSeconds.toLong())

                    is FreeMeditationStep -> PathStepType.FREE_MEDITATION to
                        Duration.ofSeconds(step.suggestedDurationSeconds.toLong())

                    is BreathingExerciseStep -> PathStepType.BREATHING_EXERCISE to
                        Duration.ofSeconds(
                            (
                                step.inhaleSeconds.toLong() +
                                    step.holdAfterInhaleSeconds +
                                    step.exhaleSeconds +
                                    step.holdAfterExhaleSeconds
                                ) * step.cycles,
                        )
                }
                PathStepUiModel(
                    id = step.id,
                    title = step.title,
                    description = step.description,
                    type = type,
                    ordinal = index + 1,
                    completed = step.id in progress.completedStepIds,
                    displayDuration = duration,
                )
            },
        )
    }

    companion object {
        const val PATH_ID_ARGUMENT = "pathId"
    }
}
