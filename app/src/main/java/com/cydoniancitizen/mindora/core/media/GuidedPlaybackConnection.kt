package com.cydoniancitizen.mindora.core.media

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface GuidedMeditationPlayback {
    val state: kotlinx.coroutines.flow.StateFlow<GuidedPlaybackState>

    fun start(stepId: String)
    fun play()
    fun pause()
    fun end()
    fun retrySave()
    fun discard()
    fun clear()
    fun release()
}

class GuidedPlaybackConnection @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : GuidedMeditationPlayback {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<GuidedPlaybackState>(
        GuidedPlaybackState.Connecting,
    )
    override val state: kotlinx.coroutines.flow.StateFlow<GuidedPlaybackState> =
        _state

    private var controller: MediaController? = null
    private var pendingStepId: String? = null
    private var progressJob: Job? = null
    private var released = false

    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            publish(controller, extras)
        }

        override fun onDisconnected(controller: MediaController) {
            this@GuidedPlaybackConnection.controller = null
            progressJob?.cancel()
            // A future that has already handed over its controller never hands over another one, so
            // it is dropped here: whatever asks to play next has to build a fresh connection.
            discardConnection()
            if (!released) _state.value = GuidedPlaybackState.Disconnected
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            controller?.let { publish(it, it.sessionExtras) }
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        connect()
    }

    /** Builds a connection, unless one is already connected or still on its way. */
    private fun connect() {
        if (released || controllerFuture != null) return
        _state.value = GuidedPlaybackState.Connecting
        val future = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, MindoraPlaybackService::class.java)),
        )
            .setListener(controllerListener)
            .buildAsync()
        controllerFuture = future
        future.addListener(
            {
                if (released) return@addListener
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(playerListener)
                    publish(connected, connected.sessionExtras)
                    pendingStepId?.let {
                        pendingStepId = null
                        send(GuidedPlaybackProtocol.LOAD, Bundle().apply {
                            putString(GuidedPlaybackProtocol.STEP_ID, it)
                        })
                    }
                } catch (_: ExecutionException) {
                    discardConnection()
                    _state.value = GuidedPlaybackState.Disconnected
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    discardConnection()
                    _state.value = GuidedPlaybackState.Disconnected
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun discardConnection() {
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
    }

    override fun start(stepId: String) {
        val connected = controller
        if (connected == null) {
            pendingStepId = stepId
            // Without this a retry after a disconnection only parks the request: the first future
            // was already spent, so nothing would ever pick the step up again.
            connect()
            return
        }
        send(GuidedPlaybackProtocol.LOAD, Bundle().apply {
            putString(GuidedPlaybackProtocol.STEP_ID, stepId)
        })
    }

    override fun play() {
        controller?.takeIf { it.isCommandAvailable(Player.COMMAND_PLAY_PAUSE) }?.play()
    }

    override fun pause() {
        controller?.takeIf { it.isCommandAvailable(Player.COMMAND_PLAY_PAUSE) }?.pause()
    }

    override fun end() = send(GuidedPlaybackProtocol.END)

    override fun retrySave() = send(GuidedPlaybackProtocol.RETRY_SAVE)

    override fun discard() = send(GuidedPlaybackProtocol.DISCARD)

    override fun clear() = send(GuidedPlaybackProtocol.CLEAR)

    override fun release() {
        if (released) return
        released = true
        progressJob?.cancel()
        controller?.removeListener(playerListener)
        controller = null
        discardConnection()
        scope.cancel()
    }

    private fun send(command: SessionCommand, arguments: Bundle = Bundle.EMPTY) {
        controller
            ?.takeIf { it.isSessionCommandAvailable(command) }
            ?.sendCustomCommand(command, arguments)
    }

    private fun publish(controller: MediaController, extras: Bundle) {
        val phase = extras.getString(GuidedPlaybackProtocol.PHASE)
            ?: GuidedPlaybackProtocol.PHASE_IDLE
        val stepId = extras.getString(GuidedPlaybackProtocol.STEP_ID)
        val activeDuration = Duration.ofMillis(
            extras.getLong(GuidedPlaybackProtocol.ACTIVE_DURATION_MILLIS, 0L),
        )
        val position = Duration.ofMillis(controller.currentPosition.coerceAtLeast(0L))
        val durationMillis = controller.duration
        val duration = durationMillis
            .takeUnless { it == C.TIME_UNSET || it < 0L }
            ?.let(Duration::ofMillis)

        _state.value = when (phase) {
            GuidedPlaybackProtocol.PHASE_PREPARING -> stepId?.let {
                GuidedPlaybackState.Preparing(it, position, duration)
            } ?: GuidedPlaybackState.Idle

            GuidedPlaybackProtocol.PHASE_PLAYING -> stepId?.let {
                GuidedPlaybackState.Playing(it, position, duration)
            } ?: GuidedPlaybackState.Idle

            GuidedPlaybackProtocol.PHASE_PAUSED -> stepId?.let {
                GuidedPlaybackState.Paused(it, position, duration)
            } ?: GuidedPlaybackState.Idle

            GuidedPlaybackProtocol.PHASE_SAVING -> stepId?.let {
                GuidedPlaybackState.Saving(it, activeDuration)
            } ?: GuidedPlaybackState.Idle

            GuidedPlaybackProtocol.PHASE_FINISHED -> stepId?.let {
                GuidedPlaybackState.Finished(
                    stepId = it,
                    status = extras.sessionStatus(),
                    activeDuration = activeDuration,
                )
            } ?: GuidedPlaybackState.Idle

            GuidedPlaybackProtocol.PHASE_SAVE_FAILED -> stepId?.let {
                GuidedPlaybackState.SaveFailed(it, activeDuration)
            } ?: GuidedPlaybackState.Idle

            GuidedPlaybackProtocol.PHASE_PLAYBACK_FAILED -> GuidedPlaybackState.PlaybackFailed(
                stepId = stepId,
                result = if (extras.getBoolean(GuidedPlaybackProtocol.INTERRUPTED_SAVED)) {
                    PlaybackFailureResult.INTERRUPTED_SAVED
                } else {
                    PlaybackFailureResult.NOTHING_SAVED
                },
                activeDuration = activeDuration,
            )

            else -> GuidedPlaybackState.Idle
        }
        updateProgressTicker()
    }

    private fun updateProgressTicker() {
        if (_state.value is GuidedPlaybackState.Playing) {
            if (progressJob?.isActive == true) return
            progressJob = scope.launch {
                while (isActive && _state.value is GuidedPlaybackState.Playing) {
                    delay(PROGRESS_INTERVAL_MILLIS)
                    controller?.let { publish(it, it.sessionExtras) }
                }
            }
        } else {
            progressJob?.cancel()
            progressJob = null
        }
    }

    private fun Bundle.sessionStatus(): MindfulnessSessionStatus =
        if (getString(GuidedPlaybackProtocol.SESSION_STATUS) ==
            MindfulnessSessionStatus.COMPLETED.name
        ) {
            MindfulnessSessionStatus.COMPLETED
        } else {
            MindfulnessSessionStatus.INTERRUPTED
        }

    private companion object {
        const val PROGRESS_INTERVAL_MILLIS = 500L
    }
}

internal object GuidedPlaybackProtocol {
    val LOAD = SessionCommand("mindora.guided.LOAD", Bundle.EMPTY)
    val END = SessionCommand("mindora.guided.END", Bundle.EMPTY)
    val RETRY_SAVE = SessionCommand("mindora.guided.RETRY_SAVE", Bundle.EMPTY)
    val DISCARD = SessionCommand("mindora.guided.DISCARD", Bundle.EMPTY)
    val CLEAR = SessionCommand("mindora.guided.CLEAR", Bundle.EMPTY)
    val applicationCommands = listOf(LOAD, END, RETRY_SAVE, DISCARD, CLEAR)

    const val STEP_ID = "step_id"
    const val PHASE = "phase"
    const val ACTIVE_DURATION_MILLIS = "active_duration_millis"
    const val SESSION_STATUS = "session_status"
    const val INTERRUPTED_SAVED = "interrupted_saved"

    const val PHASE_IDLE = "idle"
    const val PHASE_PREPARING = "preparing"
    const val PHASE_PLAYING = "playing"
    const val PHASE_PAUSED = "paused"
    const val PHASE_SAVING = "saving"
    const val PHASE_FINISHED = "finished"
    const val PHASE_SAVE_FAILED = "save_failed"
    const val PHASE_PLAYBACK_FAILED = "playback_failed"
}
