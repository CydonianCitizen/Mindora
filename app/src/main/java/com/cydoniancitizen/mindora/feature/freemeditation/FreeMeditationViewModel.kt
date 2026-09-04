package com.cydoniancitizen.mindora.feature.freemeditation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.decodeContentRouteId
import com.cydoniancitizen.mindora.core.content.findStep
import com.cydoniancitizen.mindora.core.content.model.FreeMeditationStep
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class FreeMeditationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: MindfulnessContentRepository,
    private val repository: MindfulnessSessionRepository,
    private val timeSource: SessionTimeSource,
) : ViewModel() {
    private val hasLinkedStep = savedStateHandle.contains(STEP_ID_ARGUMENT)
    private val stepId = savedStateHandle.get<String>(STEP_ID_ARGUMENT)
        ?.takeIf(String::isNotBlank)
    private var linkedContent: LinkedFreeMeditationDetails? = null
    private val _uiState = MutableStateFlow<FreeMeditationUiState>(
        if (hasLinkedStep) {
            FreeMeditationUiState.LoadingContent
        } else {
            FreeMeditationUiState.Setup()
        },
    )
    val uiState: StateFlow<FreeMeditationUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    internal val hasActiveTicker: Boolean
        get() = tickerJob?.isActive == true

    init {
        if (hasLinkedStep) resolveContent()
    }

    fun selectDuration(duration: Duration) {
        val setup = _uiState.value as? FreeMeditationUiState.Setup ?: return
        if (duration in setup.availableDurations) {
            _uiState.value = setup.copy(selectedDuration = duration)
        }
    }

    fun start() {
        val setup = _uiState.value as? FreeMeditationUiState.Setup ?: return
        val startedAt = timeSource.nowInstant()
        val elapsedRealtime = timeSource.elapsedRealtimeMillis()
        _uiState.value = FreeMeditationUiState.Running(
            plannedDuration = setup.selectedDuration,
            startedAt = startedAt,
            accumulatedActiveDuration = Duration.ZERO,
            resumedAtElapsedRealtimeMillis = elapsedRealtime,
            activeDuration = Duration.ZERO,
            remainingDuration = setup.selectedDuration,
        )
        startTicker()
    }

    fun pause() {
        val running = _uiState.value as? FreeMeditationUiState.Running ?: return
        val activeDuration = currentActiveDuration(running)
        if (activeDuration >= running.plannedDuration) {
            finish(running, MindfulnessSessionStatus.COMPLETED, running.plannedDuration)
            return
        }
        stopTicker()
        _uiState.value = FreeMeditationUiState.Paused(
            plannedDuration = running.plannedDuration,
            startedAt = running.startedAt,
            activeDuration = activeDuration,
            remainingDuration = running.plannedDuration.minus(activeDuration),
        )
    }

    fun resume() {
        val paused = _uiState.value as? FreeMeditationUiState.Paused ?: return
        _uiState.value = FreeMeditationUiState.Running(
            plannedDuration = paused.plannedDuration,
            startedAt = paused.startedAt,
            accumulatedActiveDuration = paused.activeDuration,
            resumedAtElapsedRealtimeMillis = timeSource.elapsedRealtimeMillis(),
            activeDuration = paused.activeDuration,
            remainingDuration = paused.remainingDuration,
        )
        startTicker()
    }

    fun refreshTime() {
        val running = _uiState.value as? FreeMeditationUiState.Running ?: return
        val activeDuration = currentActiveDuration(running)
        if (activeDuration >= running.plannedDuration) {
            finish(running, MindfulnessSessionStatus.COMPLETED, running.plannedDuration)
        } else {
            _uiState.value = running.copy(
                activeDuration = activeDuration,
                remainingDuration = running.plannedDuration.minus(activeDuration),
            )
        }
    }

    fun requestEnd() {
        when (val state = _uiState.value) {
            is FreeMeditationUiState.Running -> {
                val activeDuration = currentActiveDuration(state)
                if (activeDuration >= state.plannedDuration) {
                    finish(state, MindfulnessSessionStatus.COMPLETED, state.plannedDuration)
                } else {
                    _uiState.value = state.copy(
                        activeDuration = activeDuration,
                        remainingDuration = state.plannedDuration.minus(activeDuration),
                        confirmEnd = true,
                    )
                }
            }

            is FreeMeditationUiState.Paused -> {
                _uiState.value = state.copy(confirmEnd = true)
            }

            else -> Unit
        }
    }

    fun dismissEndRequest() {
        when (val state = _uiState.value) {
            is FreeMeditationUiState.Running -> {
                _uiState.value = state.copy(confirmEnd = false)
            }

            is FreeMeditationUiState.Paused -> {
                _uiState.value = state.copy(confirmEnd = false)
            }

            else -> Unit
        }
    }

    fun confirmEnd() {
        when (val state = _uiState.value) {
            is FreeMeditationUiState.Running -> {
                finishOrReset(
                    plannedDuration = state.plannedDuration,
                    startedAt = state.startedAt,
                    activeDuration = currentActiveDuration(state),
                )
            }

            is FreeMeditationUiState.Paused -> {
                finishOrReset(
                    plannedDuration = state.plannedDuration,
                    startedAt = state.startedAt,
                    activeDuration = state.activeDuration,
                )
            }

            else -> Unit
        }
    }

    fun retrySave() {
        val failed = _uiState.value as? FreeMeditationUiState.SaveFailed ?: return
        _uiState.value = FreeMeditationUiState.Saving(failed.pendingSession)
        save(failed.pendingSession)
    }

    fun discard() {
        val failed = _uiState.value as? FreeMeditationUiState.SaveFailed ?: return
        _uiState.value = setupState(requireNotNull(failed.pendingSession.plannedDuration))
    }

    private fun finishOrReset(
        plannedDuration: Duration,
        startedAt: Instant,
        activeDuration: Duration,
    ) {
        stopTicker()
        if (activeDuration.isZero || activeDuration.isNegative) {
            _uiState.value = setupState(plannedDuration)
            return
        }
        val status = if (activeDuration >= plannedDuration) {
            MindfulnessSessionStatus.COMPLETED
        } else {
            MindfulnessSessionStatus.INTERRUPTED
        }
        finish(
            plannedDuration = plannedDuration,
            startedAt = startedAt,
            status = status,
            activeDuration = activeDuration.coerceAtMost(plannedDuration),
        )
    }

    private fun finish(
        running: FreeMeditationUiState.Running,
        status: MindfulnessSessionStatus,
        activeDuration: Duration,
    ) {
        finish(
            plannedDuration = running.plannedDuration,
            startedAt = running.startedAt,
            status = status,
            activeDuration = activeDuration,
        )
    }

    private fun finish(
        plannedDuration: Duration,
        startedAt: Instant,
        status: MindfulnessSessionStatus,
        activeDuration: Duration,
    ) {
        if (_uiState.value !is FreeMeditationUiState.Running &&
            _uiState.value !is FreeMeditationUiState.Paused
        ) {
            return
        }
        stopTicker()
        val session = MindfulnessSession(
            id = UUID.randomUUID().toString(),
            type = MindfulnessSessionType.FREE_MEDITATION,
            status = status,
            sourcePathId = linkedContent?.pathId,
            sourceStepId = linkedContent?.stepId,
            startedAt = startedAt,
            activeDuration = activeDuration,
            plannedDuration = plannedDuration,
        )
        _uiState.value = FreeMeditationUiState.Saving(session)
        save(session)
    }

    private fun save(session: MindfulnessSession) {
        viewModelScope.launch {
            try {
                repository.addSession(session)
                val saving = _uiState.value as? FreeMeditationUiState.Saving
                if (saving?.pendingSession?.id == session.id) {
                    _uiState.value = FreeMeditationUiState.Finished(session)
                }
            } catch (error: Exception) {
                if (error is CancellationException) {
                    throw error
                }
                val saving = _uiState.value as? FreeMeditationUiState.Saving
                if (saving?.pendingSession?.id == session.id) {
                    _uiState.value = FreeMeditationUiState.SaveFailed(session)
                }
            }
        }
    }

    private fun currentActiveDuration(
        running: FreeMeditationUiState.Running,
    ): Duration {
        val runningSegmentMillis = max(
            0L,
            timeSource.elapsedRealtimeMillis() - running.resumedAtElapsedRealtimeMillis,
        )
        return running.accumulatedActiveDuration.plusMillis(runningSegmentMillis)
    }

    private fun resolveContent() {
        val requestedStepId = stepId
        if (requestedStepId.isNullOrBlank()) {
            _uiState.value = FreeMeditationUiState.Unavailable
            return
        }
        viewModelScope.launch {
            _uiState.value = try {
                val paths = contentRepository.getPaths()
                val located = paths.findStep(requestedStepId)
                    ?: decodeContentRouteId(requestedStepId)?.let(paths::findStep)
                val step = located?.step as? FreeMeditationStep
                if (located == null || step == null) {
                    FreeMeditationUiState.Unavailable
                } else {
                    val details = LinkedFreeMeditationDetails(
                        pathId = located.path.id,
                        stepId = step.id,
                        title = step.title,
                        description = step.description,
                        plannedDuration = Duration.ofSeconds(
                            step.suggestedDurationSeconds.toLong(),
                        ),
                    )
                    linkedContent = details
                    FreeMeditationUiState.Setup(linkedContent = details)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                FreeMeditationUiState.Unavailable
            }
        }
    }

    private fun setupState(selectedDuration: Duration): FreeMeditationUiState.Setup =
        FreeMeditationUiState.Setup(
            linkedContent = linkedContent,
            selectedDuration = selectedDuration,
        )

    private fun startTicker() {
        stopTicker()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MILLIS)
                refreshTime()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 1_000L
        const val STEP_ID_ARGUMENT = "stepId"
    }
}

