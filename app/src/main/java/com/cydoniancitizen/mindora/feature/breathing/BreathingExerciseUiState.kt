package com.cydoniancitizen.mindora.feature.breathing

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import java.time.Duration
import java.time.Instant

sealed interface BreathingExerciseUiState {
    data class Setup(
        val config: BreathingExerciseConfig = ProductionBreathingExerciseConfig,
        val totalCycles: Int = config.cycles,
        val plannedDuration: Duration = config.plannedDuration,
    ) : BreathingExerciseUiState

    data class Running(
        val startedAt: Instant,
        val accumulatedActiveDuration: Duration,
        val resumedAtElapsedRealtimeMillis: Long,
        val currentPhase: BreathingPhase,
        val currentCycle: Int,
        val totalCycles: Int,
        val phaseRemainingDuration: Duration,
        val totalRemainingDuration: Duration,
        val activeDuration: Duration,
        val phaseProgress: Float,
        val confirmEnd: Boolean = false,
    ) : BreathingExerciseUiState

    data class Paused(
        val startedAt: Instant,
        val currentPhase: BreathingPhase,
        val currentCycle: Int,
        val totalCycles: Int,
        val phaseRemainingDuration: Duration,
        val totalRemainingDuration: Duration,
        val activeDuration: Duration,
        val phaseProgress: Float,
        val confirmEnd: Boolean = false,
    ) : BreathingExerciseUiState

    data class Saving(
        val pendingSession: MindfulnessSession,
        val completedCycles: Int,
    ) : BreathingExerciseUiState

    data class Finished(
        val savedSessionStatus: MindfulnessSessionStatus,
        val activeDuration: Duration,
        val plannedDuration: Duration,
        val completedCycles: Int,
    ) : BreathingExerciseUiState

    data class SaveFailed(
        val pendingSession: MindfulnessSession,
        val completedCycles: Int,
    ) : BreathingExerciseUiState
}
