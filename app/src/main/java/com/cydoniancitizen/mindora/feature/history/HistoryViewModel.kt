package com.cydoniancitizen.mindora.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: MindfulnessSessionRepository,
    private val contentRepository: MindfulnessContentRepository? = null,
) : ViewModel() {

    private val selectedFilterState = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter: StateFlow<HistoryFilter> = selectedFilterState

    private val pathsFlow = flow {
        val paths = try {
            contentRepository?.getPaths() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        emit(paths)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        sessionRepository.observeSessions(),
        selectedFilterState,
        pathsFlow,
    ) { sessions, filter, paths ->
        if (sessions.isEmpty()) {
            HistoryUiState.Empty
        } else {
            val stepTitleMap = buildStepTitleMap(paths)
            val sortedSessions = sessions.sortedByDescending { it.startedAt }
            val filteredSessions = when (filter) {
                HistoryFilter.ALL -> sortedSessions
                HistoryFilter.GUIDED_MEDITATION -> sortedSessions.filter { it.type == MindfulnessSessionType.GUIDED_MEDITATION }
                HistoryFilter.FREE_MEDITATION -> sortedSessions.filter { it.type == MindfulnessSessionType.FREE_MEDITATION }
                HistoryFilter.BREATHING_EXERCISE -> sortedSessions.filter { it.type == MindfulnessSessionType.BREATHING_EXERCISE }
            }

            val monthGroups = groupSessionsByMonth(filteredSessions, stepTitleMap)

            HistoryUiState.Content(
                selectedFilter = filter,
                availableFilters = HistoryFilter.entries,
                monthGroups = monthGroups,
                totalSessionsCount = sessions.size,
                sessions = filteredSessions,
            )
        }
    }.catch {
        emit(HistoryUiState.Error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HistoryUiState.Loading,
    )

    fun selectFilter(filter: HistoryFilter) {
        selectedFilterState.value = filter
    }

    companion object {
        fun buildStepTitleMap(paths: List<MindfulnessPath>): Map<String, String> {
            val map = mutableMapOf<String, String>()
            for (path in paths) {
                for (step in path.steps) {
                    map[step.id] = step.title
                }
            }
            return map
        }

        fun groupSessionsByMonth(
            sessions: List<MindfulnessSession>,
            stepTitleMap: Map<String, String> = emptyMap(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): List<HistoryMonthGroup> {
            if (sessions.isEmpty()) return emptyList()

            val grouped = sessions.groupBy { session ->
                YearMonth.from(session.startedAt.atZone(zoneId))
            }

            return grouped.entries
                .sortedByDescending { it.key }
                .map { (yearMonth, monthSessions) ->
                    val items = monthSessions.map { session ->
                        HistorySessionDisplayItem(
                            session = session,
                            catalogueTitle = session.sourceStepId?.let { stepTitleMap[it] },
                        )
                    }
                    HistoryMonthGroup(
                        yearMonth = yearMonth,
                        sessions = items,
                    )
                }
        }
    }
}

