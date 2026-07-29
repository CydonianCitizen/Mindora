package com.cydoniancitizen.mindora.core.media

import java.time.Duration

internal class ActiveListeningTimeTracker(
    private val elapsedRealtimeMillis: () -> Long,
) {
    private var accumulatedMillis = 0L
    private var playingSinceMillis: Long? = null
    private var finalized = false

    fun setPlaying(isPlaying: Boolean) {
        if (finalized || isPlaying == (playingSinceMillis != null)) return
        val now = elapsedRealtimeMillis()
        if (isPlaying) {
            playingSinceMillis = now
        } else {
            accumulateUntil(now)
        }
    }

    fun duration(): Duration {
        val runningMillis = playingSinceMillis?.let { baseline ->
            (elapsedRealtimeMillis() - baseline).coerceAtLeast(0L)
        } ?: 0L
        return Duration.ofMillis(accumulatedMillis + runningMillis)
    }

    fun finish(): Duration {
        if (!finalized) {
            playingSinceMillis?.let { accumulateUntil(elapsedRealtimeMillis()) }
            finalized = true
        }
        return Duration.ofMillis(accumulatedMillis)
    }

    private fun accumulateUntil(now: Long) {
        val baseline = playingSinceMillis ?: return
        accumulatedMillis += (now - baseline).coerceAtLeast(0L)
        playingSinceMillis = null
    }
}
