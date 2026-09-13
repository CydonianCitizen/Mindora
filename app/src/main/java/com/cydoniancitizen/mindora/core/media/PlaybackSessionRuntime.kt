package com.cydoniancitizen.mindora.core.media

import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * What the saved [MindfulnessSession] records about where a playback session came from.
 *
 * A guided meditation fills all of it from its catalogue path step; a White Noise session has no
 * path, so [sourcePathId] is null and [sourceStepId] is the sound id.
 */
internal data class PlaybackSessionIdentity(
    val type: MindfulnessSessionType,
    val sourcePathId: String?,
    val sourceStepId: String,
    val plannedDuration: Duration,
)

/**
 * Owns the one session a playback produces: it tracks active listening time, then builds and saves
 * exactly one [MindfulnessSession], with retry and discard for a failed save.
 *
 * It is unaware of what is playing — guided meditation or White Noise — beyond the
 * [PlaybackSessionIdentity] it is handed.
 */
internal class PlaybackSessionRuntime(
    val identity: PlaybackSessionIdentity,
    val startedAt: Instant,
    timeSource: SessionTimeSource,
    private val repository: MindfulnessSessionRepository,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    private val activeTime = ActiveListeningTimeTracker(timeSource::elapsedRealtimeMillis)
    private var finalizationStarted = false
    private var saveInProgress = false

    var pendingSession: MindfulnessSession? = null
        private set

    val isFinalized: Boolean
        get() = finalizationStarted

    fun setPlaying(isPlaying: Boolean) {
        activeTime.setPlaying(isPlaying)
    }

    fun activeDuration(): Duration = activeTime.duration()

    suspend fun finalize(
        status: MindfulnessSessionStatus,
    ): PlaybackSaveResult {
        if (finalizationStarted) return PlaybackSaveResult.Ignored
        finalizationStarted = true
        val duration = activeTime.finish()
        if (duration <= Duration.ZERO) return PlaybackSaveResult.NothingToSave

        val session = MindfulnessSession(
            id = idFactory(),
            type = identity.type,
            status = status,
            sourcePathId = identity.sourcePathId,
            sourceStepId = identity.sourceStepId,
            startedAt = startedAt,
            activeDuration = duration,
            plannedDuration = identity.plannedDuration,
        )
        return save(session)
    }

    suspend fun retry(): PlaybackSaveResult {
        if (saveInProgress) return PlaybackSaveResult.Ignored
        val session = pendingSession ?: return PlaybackSaveResult.Ignored
        return save(session)
    }

    fun discard(): Boolean {
        if (saveInProgress || pendingSession == null) return false
        pendingSession = null
        return true
    }

    private suspend fun save(session: MindfulnessSession): PlaybackSaveResult {
        if (saveInProgress) return PlaybackSaveResult.Ignored
        saveInProgress = true
        pendingSession = session
        return try {
            repository.addSession(session)
            pendingSession = null
            PlaybackSaveResult.Saved(session)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            PlaybackSaveResult.Failed(session)
        } finally {
            saveInProgress = false
        }
    }
}

internal sealed interface PlaybackSaveResult {
    data class Saved(val session: MindfulnessSession) : PlaybackSaveResult
    data class Failed(val session: MindfulnessSession) : PlaybackSaveResult
    data object NothingToSave : PlaybackSaveResult
    data object Ignored : PlaybackSaveResult
}
