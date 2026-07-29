package com.cydoniancitizen.mindora.core.session

import android.os.SystemClock
import java.time.Instant
import javax.inject.Inject

interface SessionTimeSource {
    fun nowInstant(): Instant

    fun elapsedRealtimeMillis(): Long
}

class AndroidSessionTimeSource @Inject constructor() : SessionTimeSource {
    override fun nowInstant(): Instant = Instant.now()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}
