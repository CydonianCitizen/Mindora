package com.cydoniancitizen.mindora.core.media

import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fires [onExpired] once, [duration] after [start] is called, unless [cancel] (or another [start])
 * happens first.
 *
 * A White Noise session loops forever, so the media player never reports an end; this is what stops
 * it. The logic is deliberately tiny and scope-injected so it runs under `runTest` virtual time in
 * the JVM suite rather than needing a device.
 */
internal class SessionCountdown(
    private val onExpired: () -> Unit,
) {
    private var job: Job? = null

    val isRunning: Boolean
        get() = job?.isActive == true

    fun start(scope: CoroutineScope, duration: Duration) {
        cancel()
        val millis = duration.toMillis()
        if (millis <= 0L) {
            onExpired()
            return
        }
        job = scope.launch {
            delay(millis)
            onExpired()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
