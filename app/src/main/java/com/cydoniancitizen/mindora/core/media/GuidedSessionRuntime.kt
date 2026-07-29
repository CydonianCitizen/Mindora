package com.cydoniancitizen.mindora.core.media

import com.cydoniancitizen.mindora.core.content.ResolvedGuidedMeditation
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException

internal class GuidedSessionRuntime(
    val meditation: ResolvedGuidedMeditation,
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
    ): GuidedSaveResult {
        if (finalizationStarted) return GuidedSaveResult.Ignored
        finalizationStarted = true
        val duration = activeTime.finish()
        if (duration <= Duration.ZERO) return GuidedSaveResult.NothingToSave

        val session = MindfulnessSession(
            id = idFactory(),
            type = MindfulnessSessionType.GUIDED_MEDITATION,
            status = status,
            sourcePathId = meditation.pathId,
            sourceStepId = meditation.step.id,
            startedAt = startedAt,
            activeDuration = duration,
            plannedDuration = Duration.ofSeconds(meditation.step.durationSeconds.toLong()),
        )
        return save(session)
    }

    suspend fun retry(): GuidedSaveResult {
        if (saveInProgress) return GuidedSaveResult.Ignored
        val session = pendingSession ?: return GuidedSaveResult.Ignored
        return save(session)
    }

    fun discard(): Boolean {
        if (saveInProgress || pendingSession == null) return false
        pendingSession = null
        return true
    }

    private suspend fun save(session: MindfulnessSession): GuidedSaveResult {
        if (saveInProgress) return GuidedSaveResult.Ignored
        saveInProgress = true
        pendingSession = session
        return try {
            repository.addSession(session)
            pendingSession = null
            GuidedSaveResult.Saved(session)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            GuidedSaveResult.Failed(session)
        } finally {
            saveInProgress = false
        }
    }
}

internal sealed interface GuidedSaveResult {
    data class Saved(val session: MindfulnessSession) : GuidedSaveResult
    data class Failed(val session: MindfulnessSession) : GuidedSaveResult
    data object NothingToSave : GuidedSaveResult
    data object Ignored : GuidedSaveResult
}
