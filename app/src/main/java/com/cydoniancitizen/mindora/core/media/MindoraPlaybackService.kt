package com.cydoniancitizen.mindora.core.media

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.cydoniancitizen.mindora.MainActivity
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.ResolvedGuidedMeditation
import com.cydoniancitizen.mindora.core.content.findGuidedMeditation
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.SessionTimeSource
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MindoraPlaybackService : MediaSessionService() {
    @Inject lateinit var contentRepository: MindfulnessContentRepository
    @Inject lateinit var sessionRepository: MindfulnessSessionRepository
    @Inject lateinit var timeSource: SessionTimeSource

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private var runtime: PlaybackSessionRuntime? = null
    private var loadJob: Job? = null
    private val whiteNoiseCountdown = SessionCountdown {
        finalizeSession(
            status = MindfulnessSessionStatus.COMPLETED,
            cause = FinalizationCause.NORMAL,
        )
    }
    private var phase = PlaybackPhase.IDLE
    private var finalizationCause = FinalizationCause.NORMAL
    private var terminalSession: MindfulnessSession? = null
    private var playbackFailureStepId: String? = null
    private var interruptedSessionSaved = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            runtime?.setPlaying(isPlaying)
            publishPlayerPhase()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                finalizeSession(
                    status = MindfulnessSessionStatus.COMPLETED,
                    cause = FinalizationCause.NORMAL,
                )
            } else {
                publishPlayerPhase()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlaybackError()
        }
    }

    private val sessionCallback = object : MediaSession.Callback {
        @UnstableApi
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val ownApplication = controller.packageName == packageName
            val trustedSystemController = controller.isTrusted ||
                session.isMediaNotificationController(controller)
            if (!ownApplication && !trustedSystemController) {
                return MediaSession.ConnectionResult.reject()
            }

            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .apply {
                    if (ownApplication) {
                        GuidedPlaybackProtocol.applicationCommands.forEach(::add)
                    }
                }
                .build()
            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_GET_TIMELINE,
                        Player.COMMAND_GET_METADATA,
                        Player.COMMAND_GET_AUDIO_ATTRIBUTES,
                        Player.COMMAND_GET_VOLUME,
                    )
                    .build(),
            )
        }

        @UnstableApi
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (controller.packageName != packageName) {
                return Futures.immediateFuture(
                    SessionResult(SessionError.ERROR_PERMISSION_DENIED),
                )
            }
            val accepted = when (customCommand) {
                GuidedPlaybackProtocol.LOAD -> {
                    val contentId = args.getString(GuidedPlaybackProtocol.STEP_ID)
                    val kind = args.getString(GuidedPlaybackProtocol.CONTENT_KIND)
                        ?: GuidedPlaybackProtocol.KIND_GUIDED
                    val durationMillis = args.getLong(GuidedPlaybackProtocol.DURATION_MILLIS, 0L)
                    contentId != null && load(contentId, kind, durationMillis)
                }
                GuidedPlaybackProtocol.END -> requestEarlyEnd()
                GuidedPlaybackProtocol.RETRY_SAVE -> retrySave()
                GuidedPlaybackProtocol.DISCARD -> discardPendingSave()
                GuidedPlaybackProtocol.CLEAR -> clearTerminalState()
                else -> false
            }
            return Futures.immediateFuture(
                SessionResult(
                    if (accepted) SessionResult.RESULT_SUCCESS
                    else SessionError.ERROR_INVALID_STATE,
                ),
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(playerListener) }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(sessionCallback)
            .setSessionExtras(runtimeExtras())
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession = mediaSession

    override fun onDestroy() {
        loadJob?.cancel()
        serviceScope.cancel()
        player.removeListener(playerListener)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun load(contentId: String, kind: String, durationMillis: Long): Boolean {
        if (phase == PlaybackPhase.PLAYBACK_FAILED && !interruptedSessionSaved) {
            resetPlayback()
        }
        if (phase != PlaybackPhase.IDLE || loadJob?.isActive == true) return false

        phase = PlaybackPhase.PREPARING
        playbackFailureStepId = contentId
        publishState()
        val startedAt = timeSource.nowInstant()
        loadJob = serviceScope.launch {
            try {
                val prepared = when (kind) {
                    GuidedPlaybackProtocol.KIND_WHITE_NOISE ->
                        prepareWhiteNoise(contentId, durationMillis)
                    else -> prepareGuided(contentId)
                } ?: return@launch showPlaybackFailure(contentId)

                verifyAsset(prepared.assetPath)
                runtime = PlaybackSessionRuntime(
                    identity = prepared.identity,
                    startedAt = startedAt,
                    timeSource = timeSource,
                    repository = sessionRepository,
                )
                playbackFailureStepId = null
                interruptedSessionSaved = false
                terminalSession = null
                // Set explicitly for every load: a White Noise session leaves the player on
                // REPEAT_MODE_ONE, and the next guided meditation must not inherit it.
                player.repeatMode = prepared.repeatMode
                player.setMediaItem(prepared.mediaItem)
                player.prepare()
                player.play()
                prepared.loopDuration?.let { whiteNoiseCountdown.start(serviceScope, it) }
                publishPlayerPhase()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showPlaybackFailure(contentId)
            }
        }
        return true
    }

    private suspend fun prepareGuided(stepId: String): PreparedContent? {
        val meditation = contentRepository.findGuidedMeditation(stepId) ?: return null
        return PreparedContent(
            identity = PlaybackSessionIdentity(
                type = MindfulnessSessionType.GUIDED_MEDITATION,
                sourcePathId = meditation.pathId,
                sourceStepId = meditation.step.id,
                plannedDuration = Duration.ofSeconds(meditation.step.durationSeconds.toLong()),
            ),
            assetPath = meditation.step.audioAsset,
            mediaItem = meditation.toMediaItem(),
            repeatMode = Player.REPEAT_MODE_OFF,
            loopDuration = null,
        )
    }

    private fun prepareWhiteNoise(soundId: String, durationMillis: Long): PreparedContent? {
        if (durationMillis <= 0L) return null
        val sound = WhiteNoiseCatalog.find(soundId) ?: return null
        val plannedDuration = Duration.ofMillis(durationMillis)
        return PreparedContent(
            identity = PlaybackSessionIdentity(
                type = MindfulnessSessionType.WHITE_NOISE,
                sourcePathId = null,
                sourceStepId = sound.id,
                plannedDuration = plannedDuration,
            ),
            assetPath = sound.audioAsset,
            mediaItem = sound.toMediaItem(),
            repeatMode = Player.REPEAT_MODE_ONE,
            loopDuration = plannedDuration,
        )
    }

    private suspend fun verifyAsset(assetPath: String) {
        withContext(Dispatchers.IO) {
            assets.open(assetPath).use { stream ->
                stream.read()
            }
        }
    }

    private fun assetUri(assetPath: String): Uri = Uri.Builder()
        .scheme(ASSET_SCHEME)
        .authority("")
        .apply { assetPath.split('/').forEach(::appendPath) }
        .build()

    private fun ResolvedGuidedMeditation.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(step.title)
            .setSubtitle(getString(R.string.session_type_guided_meditation))
            .setArtist(getString(R.string.app_name))
            .setDescription(step.description)
            .setDurationMs(Duration.ofSeconds(step.durationSeconds.toLong()).toMillis())
            .setIsPlayable(true)
            .build()
        return MediaItem.Builder()
            .setMediaId(step.id)
            .setUri(assetUri(step.audioAsset))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun WhiteNoiseSound.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(getString(titleResId))
            .setSubtitle(getString(R.string.session_type_white_noise))
            .setArtist(getString(R.string.app_name))
            .setIsPlayable(true)
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(assetUri(audioAsset))
            .setMediaMetadata(metadata)
            .build()
    }

    private class PreparedContent(
        val identity: PlaybackSessionIdentity,
        val assetPath: String,
        val mediaItem: MediaItem,
        val repeatMode: Int,
        /** Non-null only for a looping sound: how long before the session finalizes itself. */
        val loopDuration: Duration?,
    )

    private fun requestEarlyEnd(): Boolean {
        val activeRuntime = runtime ?: return false
        if (phase !in ACTIVE_PHASES || activeRuntime.isFinalized) return false
        player.pause()
        activeRuntime.setPlaying(false)
        finalizeSession(
            status = MindfulnessSessionStatus.INTERRUPTED,
            cause = FinalizationCause.NORMAL,
        )
        return true
    }

    private fun handlePlaybackError() {
        val activeRuntime = runtime
        if (activeRuntime == null) {
            showPlaybackFailure(playbackFailureStepId)
            return
        }
        if (activeRuntime.isFinalized) return
        activeRuntime.setPlaying(false)
        finalizeSession(
            status = MindfulnessSessionStatus.INTERRUPTED,
            cause = FinalizationCause.PLAYBACK_ERROR,
        )
    }

    private fun finalizeSession(
        status: MindfulnessSessionStatus,
        cause: FinalizationCause,
    ) {
        val activeRuntime = runtime ?: return
        if (activeRuntime.isFinalized || phase == PlaybackPhase.SAVING) return
        whiteNoiseCountdown.cancel()
        finalizationCause = cause
        phase = PlaybackPhase.SAVING
        publishState()
        serviceScope.launch {
            handleSaveResult(activeRuntime.finalize(status))
        }
    }

    private fun retrySave(): Boolean {
        val activeRuntime = runtime ?: return false
        if (phase != PlaybackPhase.SAVE_FAILED || activeRuntime.pendingSession == null) return false
        phase = PlaybackPhase.SAVING
        publishState()
        serviceScope.launch {
            handleSaveResult(activeRuntime.retry())
        }
        return true
    }

    private fun discardPendingSave(): Boolean {
        val activeRuntime = runtime ?: return false
        if (phase != PlaybackPhase.SAVE_FAILED || !activeRuntime.discard()) return false
        resetPlayback()
        publishState()
        return true
    }

    private fun clearTerminalState(): Boolean {
        if (phase !in TERMINAL_PHASES) return false
        resetPlayback()
        publishState()
        stopSelf()
        return true
    }

    private fun handleSaveResult(result: PlaybackSaveResult) {
        when (result) {
            is PlaybackSaveResult.Saved -> {
                terminalSession = result.session
                if (finalizationCause == FinalizationCause.PLAYBACK_ERROR) {
                    interruptedSessionSaved = true
                    phase = PlaybackPhase.PLAYBACK_FAILED
                } else {
                    phase = PlaybackPhase.FINISHED
                }
            }
            is PlaybackSaveResult.Failed -> {
                terminalSession = result.session
                phase = PlaybackPhase.SAVE_FAILED
            }
            PlaybackSaveResult.NothingToSave -> {
                if (finalizationCause == FinalizationCause.PLAYBACK_ERROR) {
                    interruptedSessionSaved = false
                    phase = PlaybackPhase.PLAYBACK_FAILED
                } else {
                    resetPlayback()
                }
            }
            PlaybackSaveResult.Ignored -> return
        }
        publishState()
    }

    private fun showPlaybackFailure(stepId: String?) {
        player.pause()
        phase = PlaybackPhase.PLAYBACK_FAILED
        playbackFailureStepId = stepId
        interruptedSessionSaved = false
        publishState()
    }

    private fun resetPlayback() {
        loadJob?.cancel()
        loadJob = null
        whiteNoiseCountdown.cancel()
        phase = PlaybackPhase.IDLE
        runtime = null
        terminalSession = null
        playbackFailureStepId = null
        interruptedSessionSaved = false
        finalizationCause = FinalizationCause.NORMAL
        player.pause()
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.clearMediaItems()
    }

    private fun publishPlayerPhase() {
        if (phase !in ACTIVE_PHASES) return
        phase = when {
            player.isPlaying -> PlaybackPhase.PLAYING
            player.playbackState == Player.STATE_BUFFERING -> PlaybackPhase.PREPARING
            player.playbackState == Player.STATE_READY -> PlaybackPhase.PAUSED
            else -> phase
        }
        publishState()
    }

    private fun publishState() {
        if (::mediaSession.isInitialized) mediaSession.setSessionExtras(runtimeExtras())
    }

    private fun runtimeExtras(): Bundle = Bundle().apply {
        putString(GuidedPlaybackProtocol.PHASE, phase.name)
        putString(
            GuidedPlaybackProtocol.STEP_ID,
            runtime?.identity?.sourceStepId ?: playbackFailureStepId,
        )
        putLong(
            GuidedPlaybackProtocol.ACTIVE_DURATION_MILLIS,
            terminalSession?.activeDuration?.toMillis()
                ?: runtime?.activeDuration()?.toMillis()
                ?: 0L,
        )
        terminalSession?.status?.let {
            putString(GuidedPlaybackProtocol.SESSION_STATUS, it.name)
        }
        putBoolean(GuidedPlaybackProtocol.INTERRUPTED_SAVED, interruptedSessionSaved)
    }

    private enum class FinalizationCause {
        NORMAL,
        PLAYBACK_ERROR,
    }

    private companion object {
        const val ASSET_SCHEME = "asset"

        val ACTIVE_PHASES = setOf(
            PlaybackPhase.PREPARING,
            PlaybackPhase.PLAYING,
            PlaybackPhase.PAUSED,
        )
        val TERMINAL_PHASES = setOf(
            PlaybackPhase.FINISHED,
            PlaybackPhase.PLAYBACK_FAILED,
        )

    }
}
