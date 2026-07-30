package com.cydoniancitizen.mindora.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.goal.calculateWeeklyGoalProgress
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionRepository: MindfulnessSessionRepository,
    preferencesRepository: MindoraPreferencesRepository,
    timeSource: SessionTimeSource,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.observeSessions(),
        preferencesRepository.preferences,
    ) { sessions, preferences ->
        val progress = calculateWeeklyGoalProgress(
            sessions = sessions,
            weeklyGoalMinutes = preferences.weeklyGoalMinutes,
            now = timeSource.nowInstant(),
            zoneId = ZoneId.systemDefault(),
            firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
        )
        HomeUiState.Content(
            practicedDuration = progress.practicedDuration,
            targetMinutes = preferences.weeklyGoalMinutes,
            progressFraction = progress.fraction,
            isReached = progress.isReached,
        ) as HomeUiState
    }.catch {
        emit(HomeUiState.Error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState.Loading,
    )
}
