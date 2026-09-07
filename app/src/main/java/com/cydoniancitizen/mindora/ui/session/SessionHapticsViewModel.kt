package com.cydoniancitizen.mindora.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.model.DEFAULT_HAPTIC_INTENSITY
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The one setting a running session needs, shared by every session screen. A session is not the
 * place to report a preference read failure, so an unreadable value falls back to the default.
 */
@HiltViewModel
class SessionHapticsViewModel @Inject constructor(
    preferencesRepository: MindoraPreferencesRepository,
) : ViewModel() {
    val intensity: StateFlow<Float> = preferencesRepository.preferences
        .map { it.hapticIntensity }
        .catch { emit(DEFAULT_HAPTIC_INTENSITY) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DEFAULT_HAPTIC_INTENSITY,
        )
}
