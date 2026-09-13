package com.cydoniancitizen.mindora.feature.whitenoise

import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import java.time.Duration

/** The few durations a White Noise session can run for, mirroring the free meditation choices. */
internal object WhiteNoiseDurations {
    val options: List<Duration> = listOf(5L, 10L, 20L, 30L).map(Duration::ofMinutes)
    val default: Duration = Duration.ofMinutes(10)
}

sealed interface WhiteNoiseUiState {
    data class Setup(
        val selectedDuration: Duration = WhiteNoiseDurations.default,
        val availableDurations: List<Duration> = WhiteNoiseDurations.options,
    ) : WhiteNoiseUiState

    /**
     * The sound is looping. [total] is the chosen length; the countdown itself is drawn by the
     * screen from a local clock, while the service owns the real timer that ends the session.
     */
    data class Running(
        val total: Duration,
        val preparing: Boolean = false,
    ) : WhiteNoiseUiState

    data object Saving : WhiteNoiseUiState

    data class Finished(
        val status: MindfulnessSessionStatus,
        val activeDuration: Duration,
    ) : WhiteNoiseUiState

    data object SaveFailed : WhiteNoiseUiState
}
