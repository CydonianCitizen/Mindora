package com.cydoniancitizen.mindora.core.format

import java.time.Duration
import java.util.Locale

private const val PATTERN = "%02d:%02d"

/**
 * Formats time still to come, rounding up.
 *
 * A countdown must read 05:00 for the whole of its first second: rounding down would show 04:59
 * the instant a five minute session starts, and 00:00 for a full second before it ends.
 */
internal fun formatRemaining(duration: Duration): String {
    val totalSeconds = if (duration.isZero || duration.isNegative) {
        0L
    } else {
        (duration.toMillis() + 999) / 1_000
    }
    return format(totalSeconds)
}

/**
 * Formats time that has passed or a fixed length, rounding down.
 *
 * Elapsed time and planned durations report what has actually elapsed, so 4.9 seconds is 00:04.
 */
internal fun formatElapsed(duration: Duration): String = format(duration.seconds.coerceAtLeast(0L))

private fun format(totalSeconds: Long): String =
    String.format(Locale.ROOT, PATTERN, totalSeconds / 60, totalSeconds % 60)
