package com.cydoniancitizen.mindora.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MeditationLibraryRepository
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
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
    private val libraryRepository: MeditationLibraryRepository? = null,
) : ViewModel() {

    private val selectedFilterState = MutableStateFlow(HistoryFilter.ALL)
    val selectedFilter: StateFlow<HistoryFilter> = selectedFilterState

    /**
     * Titles for the sessions that came from bundled content, whether that was a path step or a
     * library meditation. History reads by name or not at all, so a failed load is not an error
     * here: the rows fall back to their session type.
     */
    private val catalogueTitlesFlow = flow {
        val titles = try {
            (contentRepository?.getPaths() ?: emptyList())
                .flatMap { it.steps }
                .associate { it.id to it.title } +
                (libraryRepository?.getMeditations() ?: emptyList())
                    .associate { it.id to it.title }
        } catch (_: Exception) {
            emptyMap()
        }
        emit(titles)
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        sessionRepository.observeSessions(),
        selectedFilterState,
        catalogueTitlesFlow,
    ) { sessions, filter, stepTitleMap ->
        if (sessions.isEmpty()) {
            HistoryUiState.Empty
        } else {
            val sortedSessions = sessions.sortedByDescending { it.startedAt }
            val filteredSessions = filter.type
                ?.let { type -> sortedSessions.filter { it.type == type } }
                ?: sortedSessions

            HistoryUiState.Content(
                selectedFilter = filter,
                monthGroups = groupSessionsByMonth(filteredSessions, stepTitleMap),
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

