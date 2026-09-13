package com.cydoniancitizen.mindora.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MeditationLibraryRepository
import com.cydoniancitizen.mindora.core.content.decodeContentRouteId
import com.cydoniancitizen.mindora.core.content.findMeditation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MeditationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: MeditationLibraryRepository,
) : ViewModel() {
    private val meditationId = savedStateHandle.get<String>(MEDITATION_ID_ARGUMENT)
        ?.takeIf(String::isNotBlank)
    private val _uiState = MutableStateFlow<MeditationDetailUiState>(MeditationDetailUiState.Loading)
    val uiState: StateFlow<MeditationDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val requestedId = meditationId
        if (requestedId == null) {
            _uiState.value = MeditationDetailUiState.Unavailable
            return
        }
        viewModelScope.launch {
            _uiState.value = try {
                val meditations = libraryRepository.getMeditations()
                val meditation = meditations.findMeditation(requestedId)
                    ?: decodeContentRouteId(requestedId)?.let(meditations::findMeditation)
                if (meditation == null) {
                    MeditationDetailUiState.Unavailable
                } else {
                    MeditationDetailUiState.Content(
                        id = meditation.id,
                        title = meditation.title,
                        description = meditation.description,
                        technique = meditation.technique.label,
                        category = meditation.category.label,
                        goal = meditation.goal.label,
                        durationMinutes = meditation.durationMinutes,
                        level = meditation.level,
                        steps = meditation.steps.map { it.text },
                        safetyNotes = meditation.safetyNotes,
                        tags = meditation.tags,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                MeditationDetailUiState.Error
            }
        }
    }

    companion object {
        const val MEDITATION_ID_ARGUMENT = "meditationId"
    }
}
