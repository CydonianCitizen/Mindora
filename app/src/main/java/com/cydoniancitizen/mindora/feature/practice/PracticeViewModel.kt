package com.cydoniancitizen.mindora.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.goal.calculateWeeklyGoalProgress
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.progress.calculatePathProgress
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val contentRepository: MindfulnessContentRepository,
    private val sessionRepository: MindfulnessSessionRepository,
    private val preferencesRepository: MindoraPreferencesRepository,
    private val timeSource: SessionTimeSource,
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
                combine(
                    sessionRepository.observeSessions(),
                    preferencesRepository.preferences,
                ) { sessions, preferences ->
                    val now = timeSource.nowInstant()
                    val zoneId = ZoneId.systemDefault()
                    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

                    val progress = calculateWeeklyGoalProgress(
                        sessions = sessions,
                        weeklyGoalMinutes = preferences.weeklyGoalMinutes,
                        now = now,
                        zoneId = zoneId,
                        firstDayOfWeek = firstDayOfWeek,
                    )

                    val weeklyGoal = WeeklyGoalUiModel(
                        practicedDuration = progress.practicedDuration,
                        targetMinutes = preferences.weeklyGoalMinutes,
                        progressFraction = progress.fraction,
                        isReached = progress.isReached,
                    )

                    if (paths.isEmpty()) {
                        PracticeUiState.Empty(weeklyGoal = weeklyGoal)
                    } else {
                        PracticeUiState.Content(
                            paths = paths.map { path ->
                                val pathProgress = calculatePathProgress(path, sessions)
                                MindfulnessPathSummary(
                                    id = path.id,
                                    title = path.title,
                                    description = path.description,
                                    completedSteps = pathProgress.completedCount,
                                    totalSteps = pathProgress.totalCount,
                                    progressFraction = pathProgress.fraction,
                                )
                            },
                            weeklyGoal = weeklyGoal,
                        )
                    }
                }.collect { state ->
                    _uiState.value = state
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
