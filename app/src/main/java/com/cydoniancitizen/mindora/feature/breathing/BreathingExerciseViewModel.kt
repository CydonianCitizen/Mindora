package com.cydoniancitizen.mindora.feature.breathing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.decodeContentRouteId
import com.cydoniancitizen.mindora.core.content.findStep
import com.cydoniancitizen.mindora.core.content.model.BreathingExerciseStep
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
class BreathingExerciseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: MindfulnessContentRepository,
    private val repository: MindfulnessSessionRepository,
    private val timeSource: SessionTimeSource,
) : ViewModel() {
    private val hasLinkedStep = savedStateHandle.contains(STEP_ID_ARGUMENT)
    private val stepId = savedStateHandle.get<String>(STEP_ID_ARGUMENT)
        ?.takeIf(String::isNotBlank)
    private var config = ProductionBreathingExerciseConfig
    private var linkedContent: LinkedBreathingExerciseDetails? = null
    private val _uiState = MutableStateFlow<BreathingExerciseUiState>(
        if (hasLinkedStep) {
            BreathingExerciseUiState.LoadingContent
        } else {
            BreathingExerciseUiState.Setup(config)
        },
    )
    val uiState: StateFlow<BreathingExerciseUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    internal val hasActiveTicker: Boolean
        get() = tickerJob?.isActive == true

    init {
        if (hasLinkedStep) resolveContent()
    }

    fun start() {
        if (_uiState.value !is BreathingExerciseUiState.Setup) return

        val startedAt = timeSource.nowInstant()
        val elapsedRealtime = timeSource.elapsedRealtimeMillis()
        val timeline = calculateBreathingTimeline(config, Duration.ZERO)
            as BreathingTimeline.Active
        _uiState.value = timeline.toRunning(
            config = config,
            startedAt = startedAt,
            accumulatedActiveDuration = Duration.ZERO,
            resumedAtElapsedRealtimeMillis = elapsedRealtime,
        )
        startTicker()
    }

    fun pause() {
        val running = _uiState.value as? BreathingExerciseUiState.Running ?: return
        when (val timeline = calculateBreathingTimeline(config, currentActiveDuration(running))) {
            is BreathingTimeline.Complete -> finish(
                startedAt = running.startedAt,
                timeline = timeline,
                status = MindfulnessSessionStatus.COMPLETED,
            )

            is BreathingTimeline.Active -> {
                stopTicker()
                _uiState.value = timeline.toPaused(
                    config = config,
                    startedAt = running.startedAt,
                )
            }
        }
    }

    fun resume() {
        val paused = _uiState.value as? BreathingExerciseUiState.Paused ?: return
        val timeline = calculateBreathingTimeline(config, paused.activeDuration)
            as? BreathingTimeline.Active ?: return
        _uiState.value = timeline.toRunning(
            config = config,
            startedAt = paused.startedAt,
            accumulatedActiveDuration = paused.activeDuration,
            resumedAtElapsedRealtimeMillis = timeSource.elapsedRealtimeMillis(),
        )
        startTicker()
    }

    fun refreshTime() {
        val running = _uiState.value as? BreathingExerciseUiState.Running ?: return
        when (val timeline = calculateBreathingTimeline(config, currentActiveDuration(running))) {
            is BreathingTimeline.Complete -> finish(
                startedAt = running.startedAt,
                timeline = timeline,
                status = MindfulnessSessionStatus.COMPLETED,
            )

            is BreathingTimeline.Active -> {
                _uiState.value = timeline.toRunning(
                    config = config,
                    startedAt = running.startedAt,
                    accumulatedActiveDuration = running.accumulatedActiveDuration,
                    resumedAtElapsedRealtimeMillis = running.resumedAtElapsedRealtimeMillis,
                    confirmEnd = running.confirmEnd,
                )
            }
        }
    }

    fun requestEnd() {
        when (val state = _uiState.value) {
            is BreathingExerciseUiState.Running -> {
                when (
                    val timeline = calculateBreathingTimeline(
                        config,
                        currentActiveDuration(state),
                    )
                ) {
                    is BreathingTimeline.Complete -> finish(
                        startedAt = state.startedAt,
                        timeline = timeline,
                        status = MindfulnessSessionStatus.COMPLETED,
                    )

                    is BreathingTimeline.Active -> {
                        _uiState.value = timeline.toRunning(
                            config = config,
                            startedAt = state.startedAt,
                            accumulatedActiveDuration = state.accumulatedActiveDuration,
                            resumedAtElapsedRealtimeMillis =
                                state.resumedAtElapsedRealtimeMillis,
                            confirmEnd = true,
                        )
                    }
                }
            }

            is BreathingExerciseUiState.Paused -> {
                _uiState.value = state.copy(confirmEnd = true)
            }

            else -> Unit
        }
    }

    fun dismissEndRequest() {
        when (val state = _uiState.value) {
            is BreathingExerciseUiState.Running -> {
                _uiState.value = state.copy(confirmEnd = false)
            }

            is BreathingExerciseUiState.Paused -> {
                _uiState.value = state.copy(confirmEnd = false)
            }

            else -> Unit
        }
    }

    fun confirmEnd() {
        when (val state = _uiState.value) {
            is BreathingExerciseUiState.Running -> {
                finishOrReset(
                    startedAt = state.startedAt,
                    elapsedDuration = currentActiveDuration(state),
                )
            }

            is BreathingExerciseUiState.Paused -> {
                finishOrReset(
                    startedAt = state.startedAt,
                    elapsedDuration = state.activeDuration,
                )
            }

            else -> Unit
        }
    }

    fun retrySave() {
        val failed = _uiState.value as? BreathingExerciseUiState.SaveFailed ?: return
        _uiState.value = BreathingExerciseUiState.Saving(
            pendingSession = failed.pendingSession,
            completedCycles = failed.completedCycles,
        )
        save(failed.pendingSession, failed.completedCycles)
    }

    fun discard() {
        if (_uiState.value !is BreathingExerciseUiState.SaveFailed) return
        _uiState.value = setupState()
    }

    private fun finishOrReset(
        startedAt: Instant,
        elapsedDuration: Duration,
    ) {
        stopTicker()
        if (elapsedDuration.isZero || elapsedDuration.isNegative) {
            _uiState.value = setupState()
            return
        }

        val timeline = calculateBreathingTimeline(config, elapsedDuration)
        val status = if (timeline is BreathingTimeline.Complete) {
            MindfulnessSessionStatus.COMPLETED
        } else {
            MindfulnessSessionStatus.INTERRUPTED
        }
        finish(startedAt, timeline, status)
    }

    private fun finish(
        startedAt: Instant,
        timeline: BreathingTimeline,
        status: MindfulnessSessionStatus,
    ) {
        if (_uiState.value !is BreathingExerciseUiState.Running &&
            _uiState.value !is BreathingExerciseUiState.Paused
        ) {
            return
        }
        stopTicker()
        val session = MindfulnessSession(
            id = UUID.randomUUID().toString(),
            type = MindfulnessSessionType.BREATHING_EXERCISE,
            status = status,
            sourcePathId = linkedContent?.pathId,
            sourceStepId = linkedContent?.stepId,
            startedAt = startedAt,
            activeDuration = timeline.activeDuration.coerceAtMost(config.plannedDuration),
            plannedDuration = config.plannedDuration,
        )
        _uiState.value = BreathingExerciseUiState.Saving(
            pendingSession = session,
            completedCycles = timeline.completedCycles,
        )
        save(session, timeline.completedCycles)
    }

    private fun save(session: MindfulnessSession, completedCycles: Int) {
        viewModelScope.launch {
            try {
                repository.addSession(session)
                val saving = _uiState.value as? BreathingExerciseUiState.Saving
                if (saving?.pendingSession?.id == session.id) {
                    _uiState.value = BreathingExerciseUiState.Finished(
                        savedSessionStatus = session.status,
                        activeDuration = session.activeDuration,
                        plannedDuration = requireNotNull(session.plannedDuration),
                        completedCycles = completedCycles,
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error

                val saving = _uiState.value as? BreathingExerciseUiState.Saving
                if (saving?.pendingSession?.id == session.id) {
                    _uiState.value = BreathingExerciseUiState.SaveFailed(
                        pendingSession = session,
                        completedCycles = completedCycles,
                    )
                }
            }
        }
    }

    private fun currentActiveDuration(
        running: BreathingExerciseUiState.Running,
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
            _uiState.value = BreathingExerciseUiState.Unavailable
            return
        }
        viewModelScope.launch {
            _uiState.value = try {
                val paths = contentRepository.getPaths()
                val located = paths.findStep(requestedStepId)
                    ?: decodeContentRouteId(requestedStepId)?.let(paths::findStep)
                val step = located?.step as? BreathingExerciseStep
                if (located == null || step == null) {
                    BreathingExerciseUiState.Unavailable
                } else {
                    val resolvedConfig = BreathingExerciseConfig(
                        inhaleDuration = Duration.ofSeconds(step.inhaleSeconds.toLong()),
                        holdAfterInhaleDuration =
                            Duration.ofSeconds(step.holdAfterInhaleSeconds.toLong()),
                        exhaleDuration = Duration.ofSeconds(step.exhaleSeconds.toLong()),
                        holdAfterExhaleDuration =
                            Duration.ofSeconds(step.holdAfterExhaleSeconds.toLong()),
                        cycles = step.cycles,
                    )
                    val details = LinkedBreathingExerciseDetails(
                        pathId = located.path.id,
                        stepId = step.id,
                        title = step.title,
                        description = step.description,
                        config = resolvedConfig,
                    )
                    config = resolvedConfig
                    linkedContent = details
                    setupState()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                BreathingExerciseUiState.Unavailable
            }
        }
    }

    private fun setupState(): BreathingExerciseUiState.Setup =
        BreathingExerciseUiState.Setup(
            config = config,
            linkedContent = linkedContent,
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
        const val TICK_INTERVAL_MILLIS = 100L
        const val STEP_ID_ARGUMENT = "stepId"
    }
}

private fun BreathingTimeline.Active.toRunning(
    config: BreathingExerciseConfig,
    startedAt: Instant,
    accumulatedActiveDuration: Duration,
    resumedAtElapsedRealtimeMillis: Long,
    confirmEnd: Boolean = false,
) = BreathingExerciseUiState.Running(
    startedAt = startedAt,
    accumulatedActiveDuration = accumulatedActiveDuration,
    resumedAtElapsedRealtimeMillis = resumedAtElapsedRealtimeMillis,
    currentPhase = phase,
    currentCycle = cycle,
    totalCycles = config.cycles,
    phaseRemainingDuration = phaseRemainingDuration,
    totalRemainingDuration = totalRemainingDuration,
    activeDuration = activeDuration,
    phaseProgress = phaseProgress,
    confirmEnd = confirmEnd,
)

private fun BreathingTimeline.Active.toPaused(
    config: BreathingExerciseConfig,
    startedAt: Instant,
) = BreathingExerciseUiState.Paused(
    startedAt = startedAt,
    currentPhase = phase,
    currentCycle = cycle,
    totalCycles = config.cycles,
    phaseRemainingDuration = phaseRemainingDuration,
    totalRemainingDuration = totalRemainingDuration,
    activeDuration = activeDuration,
    phaseProgress = phaseProgress,
)
