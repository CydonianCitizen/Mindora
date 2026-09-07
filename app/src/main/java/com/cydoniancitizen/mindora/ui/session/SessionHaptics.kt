package com.cydoniancitizen.mindora.ui.session

import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.cydoniancitizen.mindora.feature.breathing.BreathingPhase
import com.cydoniancitizen.mindora.ui.BreathEasing
import com.cydoniancitizen.mindora.ui.endlessMotionAllowed
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val MEDITATION_CYCLE_MILLIS = 10_000L

internal data class HapticWaveform(val timings: LongArray, val amplitudes: IntArray)

/** Bounded native waveform: no per-frame vibrator calls or uncancelled native repeats. */
private fun hapticWaveform(
    durationMillis: Long,
    intensity: Float,
    amplitudeAt: (Long) -> Float,
): HapticWaveform? {
    require(intensity.isFinite() && intensity in 0f..1f)
    if (durationMillis <= 0 || intensity == 0f) return null
    // Stream long phases in bounded chunks.
    val duration = durationMillis.coerceAtMost(MEDITATION_CYCLE_MILLIS)
    // ponytail: 50 ms amplitude steps; use API 36 envelopes if hardware tuning exposes stepping.
    val count = ((duration + 49) / 50).toInt()
    val timings = LongArray(count) { minOf(50L, duration - it * 50L) }
    val amplitudes = IntArray(count) { index ->
        val time = index * 50L
        // Short fades avoid a hard actuator start/stop, including a mid-phase resume.
        val fade = minOf(time / 100f, (duration - time - timings[index]) / 100f, 1f)
        (255 * intensity * amplitudeAt(time).coerceIn(0f, 1f) * fade)
            .roundToInt().coerceIn(0, 255)
    }
    return HapticWaveform(timings, amplitudes)
}

internal fun breathingHapticWaveform(
    phase: BreathingPhase,
    progress: Float,
    remainingMillis: Long,
    intensity: Float,
): HapticWaveform? {
    if (phase != BreathingPhase.INHALE && phase != BreathingPhase.EXHALE) return null
    val start = progress.coerceIn(0f, 1f)
    return hapticWaveform(remainingMillis, intensity) { time ->
        val position = start + (1f - start) * time.toFloat() / remainingMillis
        BreathEasing.transform(if (phase == BreathingPhase.INHALE) position else 1f - position)
    }
}

internal fun meditationCycleProgress(elapsedMillis: Long): Float =
    (elapsedMillis.coerceAtLeast(0) % MEDITATION_CYCLE_MILLIS).toFloat() / MEDITATION_CYCLE_MILLIS

internal fun meditationHapticWaveform(
    elapsedMillis: Long,
    remainingMillis: Long,
    intensity: Float,
): HapticWaveform? {
    val cycleRemaining = MEDITATION_CYCLE_MILLIS - elapsedMillis.coerceAtLeast(0) % MEDITATION_CYCLE_MILLIS
    return hapticWaveform(minOf(cycleRemaining, remainingMillis), intensity) { time ->
        val orbit = meditationCycleProgress(elapsedMillis + time)
        val beat = (orbit * 8f) % 1f
        if (beat >= 0.24f) 0f else {
            val pulse = sin(PI * beat / 0.24).toFloat()
            pulse * pulse * (0.7f + 0.3f * cos(2 * PI * orbit).toFloat())
        }
    }
}

/** Null when the device cannot render the amplitude ramps these waveforms are built from. */
@Composable
internal fun rememberSessionVibrator() = LocalContext.current.let { context ->
    remember(context) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?.takeIf { it.hasVibrator() && it.hasAmplitudeControl() }
    }
}

/** Runs only on the resumed screen. The latest factory reads the session's monotonic clock. */
@Composable
internal fun SessionHaptics(
    running: Boolean,
    intensity: Float,
    patternKey: Any?,
    waveform: () -> HapticWaveform?,
) {
    val vibrator = rememberSessionVibrator()
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestWaveform by rememberUpdatedState(waveform)
    LaunchedEffect(running, intensity, patternKey, vibrator, lifecycleOwner) {
        if (!running || intensity == 0f || vibrator == null || !endlessMotionAllowed()) {
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                while (isActive) {
                    val next = latestWaveform() ?: awaitCancellation()
                    if (next.amplitudes.any { it > 0 }) {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(next.timings, next.amplitudes, -1),
                            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH),
                        )
                    }
                    delay(next.timings.sum())
                }
            } finally {
                vibrator.cancel()
            }
        }
    }
}
