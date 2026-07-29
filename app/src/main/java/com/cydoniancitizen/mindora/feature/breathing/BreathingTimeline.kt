package com.cydoniancitizen.mindora.feature.breathing

import java.time.Duration

data class BreathingExerciseConfig(
    val inhaleDuration: Duration,
    val holdAfterInhaleDuration: Duration,
    val exhaleDuration: Duration,
    val holdAfterExhaleDuration: Duration,
    val cycles: Int,
) {
    init {
        require(cycles > 0)
        require(
            listOf(
                inhaleDuration,
                holdAfterInhaleDuration,
                exhaleDuration,
                holdAfterExhaleDuration,
            ).all { !it.isNegative },
        )
        require(cycleDuration > Duration.ZERO)
    }

    val cycleDuration: Duration
        get() = inhaleDuration
            .plus(holdAfterInhaleDuration)
            .plus(exhaleDuration)
            .plus(holdAfterExhaleDuration)

    val plannedDuration: Duration
        get() = cycleDuration.multipliedBy(cycles.toLong())
}

enum class BreathingPhase {
    INHALE,
    HOLD_AFTER_INHALE,
    EXHALE,
    HOLD_AFTER_EXHALE,
}

internal val ProductionBreathingExerciseConfig = BreathingExerciseConfig(
    inhaleDuration = Duration.ofSeconds(4),
    holdAfterInhaleDuration = Duration.ZERO,
    exhaleDuration = Duration.ofSeconds(6),
    holdAfterExhaleDuration = Duration.ZERO,
    cycles = 5,
)

internal sealed interface BreathingTimeline {
    val activeDuration: Duration
    val totalRemainingDuration: Duration
    val completedCycles: Int

    data class Active(
        override val activeDuration: Duration,
        override val totalRemainingDuration: Duration,
        override val completedCycles: Int,
        val phase: BreathingPhase,
        val cycle: Int,
        val phaseRemainingDuration: Duration,
        val phaseProgress: Float,
    ) : BreathingTimeline

    data class Complete(
        override val activeDuration: Duration,
        override val totalRemainingDuration: Duration = Duration.ZERO,
        override val completedCycles: Int,
    ) : BreathingTimeline
}

internal fun calculateBreathingTimeline(
    config: BreathingExerciseConfig,
    elapsedDuration: Duration,
): BreathingTimeline {
    val elapsedMillis = elapsedDuration.toMillis().coerceAtLeast(0)
    val plannedMillis = config.plannedDuration.toMillis()
    if (elapsedMillis >= plannedMillis) {
        return BreathingTimeline.Complete(
            activeDuration = config.plannedDuration,
            completedCycles = config.cycles,
        )
    }

    val cycleMillis = config.cycleDuration.toMillis()
    val cycleIndex = (elapsedMillis / cycleMillis).toInt().coerceIn(0, config.cycles - 1)
    val elapsedInCycle = elapsedMillis % cycleMillis
    val phases = listOf(
        BreathingPhase.INHALE to config.inhaleDuration.toMillis(),
        BreathingPhase.HOLD_AFTER_INHALE to config.holdAfterInhaleDuration.toMillis(),
        BreathingPhase.EXHALE to config.exhaleDuration.toMillis(),
        BreathingPhase.HOLD_AFTER_EXHALE to config.holdAfterExhaleDuration.toMillis(),
    )

    var phaseStartMillis = 0L
    val (phase, phaseDurationMillis) = phases.first { (_, durationMillis) ->
        val containsElapsed = durationMillis > 0 &&
            elapsedInCycle < phaseStartMillis + durationMillis
        if (!containsElapsed) {
            phaseStartMillis += durationMillis
        }
        containsElapsed
    }
    val elapsedInPhase = elapsedInCycle - phaseStartMillis

    return BreathingTimeline.Active(
        activeDuration = Duration.ofMillis(elapsedMillis),
        totalRemainingDuration = Duration.ofMillis(plannedMillis - elapsedMillis),
        completedCycles = cycleIndex,
        phase = phase,
        cycle = cycleIndex + 1,
        phaseRemainingDuration = Duration.ofMillis(phaseDurationMillis - elapsedInPhase),
        phaseProgress = (elapsedInPhase.toFloat() / phaseDurationMillis)
            .coerceIn(0f, 1f),
    )
}
