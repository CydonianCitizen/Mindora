package com.cydoniancitizen.mindora.feature.freemeditation

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.format.formatRemaining
import com.cydoniancitizen.mindora.core.preferences.model.DEFAULT_HAPTIC_INTENSITY
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.ui.BreathEasing
import com.cydoniancitizen.mindora.ui.MindoraTopAppBar
import com.cydoniancitizen.mindora.ui.endlessMotionAllowed
import com.cydoniancitizen.mindora.ui.session.EndSessionDialog
import com.cydoniancitizen.mindora.ui.session.LinkedStepLoading
import com.cydoniancitizen.mindora.ui.session.LinkedStepUnavailable
import com.cydoniancitizen.mindora.ui.session.SessionHaptics
import com.cydoniancitizen.mindora.ui.session.SessionHapticsViewModel
import com.cydoniancitizen.mindora.ui.session.SessionSaveFailed
import com.cydoniancitizen.mindora.ui.session.SessionSaving
import com.cydoniancitizen.mindora.ui.session.SessionStage
import com.cydoniancitizen.mindora.ui.session.SessionStageMetrics
import com.cydoniancitizen.mindora.ui.session.SessionFinished
import com.cydoniancitizen.mindora.ui.session.meditationCycleProgress
import com.cydoniancitizen.mindora.ui.session.meditationHapticWaveform
import com.cydoniancitizen.mindora.ui.softGlow
import com.cydoniancitizen.mindora.ui.systemAnimationsEnabled
import com.cydoniancitizen.mindora.ui.theme.tabularNumerals
import java.time.Duration
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.isActive

@Composable
fun FreeMeditationScreen(
    onNavigateBack: () -> Unit,
    viewModel: FreeMeditationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onBack = {
        when (uiState) {
            FreeMeditationUiState.LoadingContent,
            FreeMeditationUiState.Unavailable,
            is FreeMeditationUiState.Setup,
            is FreeMeditationUiState.Finished,
            -> onNavigateBack()

            is FreeMeditationUiState.Running,
            is FreeMeditationUiState.Paused,
            -> viewModel.requestEnd()

            is FreeMeditationUiState.Saving,
            is FreeMeditationUiState.SaveFailed,
            -> Unit
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshTime()
    }
    BackHandler(onBack = onBack)
    val hapticIntensity by hiltViewModel<SessionHapticsViewModel>()
        .intensity.collectAsStateWithLifecycle()
    FreeMeditationScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectDuration = viewModel::selectDuration,
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onRequestEnd = viewModel::requestEnd,
        onDismissEnd = viewModel::dismissEndRequest,
        onConfirmEnd = viewModel::confirmEnd,
        onRetrySave = viewModel::retrySave,
        onDiscard = viewModel::discard,
        onDone = onNavigateBack,
        hapticIntensity = hapticIntensity,
    )
}

@Composable
internal fun FreeMeditationScreen(
    uiState: FreeMeditationUiState,
    onBack: () -> Unit,
    onSelectDuration: (Duration) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRequestEnd: () -> Unit,
    onDismissEnd: () -> Unit,
    onConfirmEnd: () -> Unit,
    onRetrySave: () -> Unit,
    onDiscard: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    hapticIntensity: Float = DEFAULT_HAPTIC_INTENSITY,
) {
    val running = uiState as? FreeMeditationUiState.Running
    val elapsedMillis = {
        when (uiState) {
            is FreeMeditationUiState.Running -> (
                uiState.accumulatedActiveDuration.toMillis() +
                    (SystemClock.elapsedRealtime() - uiState.resumedAtElapsedRealtimeMillis).coerceAtLeast(0)
                ).coerceAtMost(uiState.plannedDuration.toMillis())
            is FreeMeditationUiState.Paused -> uiState.activeDuration.toMillis()
            else -> 0L
        }
    }
    SessionHaptics(
        running = running != null && !running.confirmEnd,
        intensity = hapticIntensity,
        patternKey = running?.startedAt,
    ) {
        running?.let { state ->
            val elapsed = elapsedMillis()
            meditationHapticWaveform(elapsed, state.plannedDuration.toMillis() - elapsed, hapticIntensity)
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        MindoraTopAppBar(
            title = stringResource(R.string.app_name),
            onBack = onBack,
            backEnabled = uiState !is FreeMeditationUiState.Saving &&
                uiState !is FreeMeditationUiState.SaveFailed,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val ringBreathing = when (uiState) {
                is FreeMeditationUiState.Setup -> false
                is FreeMeditationUiState.Running -> true
                is FreeMeditationUiState.Paused -> false
                else -> null
            }
            if (ringBreathing != null) {
                MeditationRing(
                    breathing = ringBreathing,
                    elapsedMillis = elapsedMillis,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = SessionStageMetrics.CircleTop)
                        .size(SessionStageMetrics.MeditationRing),
                )
            }

            when (uiState) {
                FreeMeditationUiState.LoadingContent -> LinkedStepLoading()
                FreeMeditationUiState.Unavailable -> LinkedStepUnavailable(onBack)
                is FreeMeditationUiState.Setup -> SetupContent(
                    state = uiState,
                    onSelectDuration = onSelectDuration,
                    onStart = onStart,
                )

                is FreeMeditationUiState.Running -> SessionContent(
                    remainingDuration = uiState.remainingDuration,
                    status = stringResource(R.string.running),
                    stepCue = uiState.steps.stepCue(
                        uiState.remainingDuration,
                        uiState.plannedDuration,
                    ),
                    primaryActionLabel = stringResource(R.string.pause),
                    onPrimaryAction = onPause,
                    onRequestEnd = onRequestEnd,
                )

                is FreeMeditationUiState.Paused -> SessionContent(
                    remainingDuration = uiState.remainingDuration,
                    status = stringResource(R.string.paused),
                    stepCue = uiState.steps.stepCue(
                        uiState.remainingDuration,
                        uiState.plannedDuration,
                    ),
                    primaryActionLabel = stringResource(R.string.resume),
                    onPrimaryAction = onResume,
                    onRequestEnd = onRequestEnd,
                )

                is FreeMeditationUiState.Saving -> SessionSaving()
                is FreeMeditationUiState.Finished -> SessionFinished(
                    title = stringResource(
                        if (uiState.savedSession.status == MindfulnessSessionStatus.COMPLETED) {
                            R.string.session_completed_result
                        } else {
                            R.string.session_interrupted_result
                        },
                    ),
                    activeDuration = formatRemaining(uiState.savedSession.activeDuration),
                    onDone = onDone,
                )

                is FreeMeditationUiState.SaveFailed -> SessionSaveFailed(
                    onRetrySave = onRetrySave,
                    onDiscard = onDiscard,
                )
            }
        }
    }

    if (
        (uiState is FreeMeditationUiState.Running && uiState.confirmEnd) ||
        (uiState is FreeMeditationUiState.Paused && uiState.confirmEnd)
    ) {
        EndSessionDialog(
            title = stringResource(R.string.end_session_title),
            message = stringResource(R.string.end_session_message),
            confirmLabel = stringResource(R.string.end_session),
            dismissLabel = stringResource(R.string.continue_session),
            onConfirm = onConfirmEnd,
            onDismiss = onDismissEnd,
        )
    }
}

/**
 * The ring behind a free meditation: still while the duration is being chosen, breathing once the
 * session runs.
 *
 * Scale, orbit and haptics share the session clock, including pause/resume and background returns.
 */
@Composable
internal fun MeditationRing(
    breathing: Boolean,
    elapsedMillis: () -> Long,
    modifier: Modifier = Modifier,
) {
    val latestElapsedMillis by rememberUpdatedState(elapsedMillis)
    var elapsed by remember { mutableLongStateOf(elapsedMillis()) }
    val animate = breathing && systemAnimationsEnabled()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(animate, lifecycleOwner) {
        if (!breathing) elapsed = latestElapsedMillis()
        if (!animate || !endlessMotionAllowed()) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                withFrameNanos { elapsed = latestElapsedMillis() }
            }
        }
    }

    val band = MaterialTheme.colorScheme.primaryContainer
    val colors = MaterialTheme.colorScheme
    val light = if (colors.surface.luminance() > 0.5f) colors.inversePrimary else colors.primary
    val ring = remember(band) {
        softGlow(
            color = band,
            0.00f to 0f,
            0.42f to 0f,
            0.60f to RING_ALPHA,
            0.74f to RING_ALPHA,
            0.92f to 0f,
            1.00f to 0f,
        )
    }

    Box(
        modifier = modifier
            .clearAndSetSemantics {}
            .graphicsLayer {
                val progress = meditationCycleProgress(elapsed)
                val openness = if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
                val scale = RING_REST + (RING_SWELL - RING_REST) * BreathEasing.transform(openness)
                scaleX = scale
                scaleY = scale
            }
            .background(ring)
            // Drawn from the first frame, including while the duration is being chosen: starting
            // the session sets the orbit moving from where it already sits, instead of popping it
            // into existence at the top of the ring.
            .drawBehind {
                val angle = meditationCycleProgress(elapsed) * 2 * PI - PI / 2
                val radius = size.minDimension * 0.335f
                val position = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius
                val glowRadius = size.minDimension * 0.09f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(light.copy(alpha = 0.45f), light.copy(alpha = 0f)),
                        center = position,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = position,
                )
            },
    )
}

/**
 * Rest is exactly the size the still ring is drawn at, which is what makes the start invisible:
 * the pulse has nowhere to jump from.
 */
private const val RING_REST = 1f

/**
 * A soft-edged ring hides small movement — at a tenth of its size the blurred edge shifts by a
 * finger's width over five seconds and reads as static. Growing by a third is what makes the
 * breath legible while staying slow enough to be calm.
 */
private const val RING_SWELL = 1.32f
private const val RING_ALPHA = 0.5f

/** Two lines of body text: most cues fit, and the rest grow downwards from a steady baseline. */
private val STEP_CUE_MIN_HEIGHT = 56.dp

@Composable
internal fun MeditationDurationDisplay(
    duration: Duration,
    modifier: Modifier = Modifier,
) {
    val countdown = formatRemaining(duration)
    val minutes = duration.toMinutes().toInt()
    val durationText = pluralStringResource(
        R.plurals.duration_minutes,
        minutes,
        duration.toMinutes(),
    )
    val accessibleDescription = stringResource(
        R.string.selected_duration_accessibility,
        durationText,
    )
    Text(
        text = countdown,
        modifier = modifier.semantics {
            contentDescription = accessibleDescription
        },
        style = MaterialTheme.typography.displayLarge.tabularNumerals(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun DurationChip(
    duration: Duration,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minutes = duration.toMinutes().toInt()
    val labelText = pluralStringResource(
        R.plurals.duration_minutes,
        minutes,
        duration.toMinutes(),
    )
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupContent(
    state: FreeMeditationUiState.Setup,
    onSelectDuration: (Duration) -> Unit,
    onStart: () -> Unit,
) {
    val linkedContent = state.linkedContent
    val durationMinutes = state.selectedDuration.toMinutes().toInt()
    val formattedDurationText = pluralStringResource(
        R.plurals.duration_minutes,
        durationMinutes,
        state.selectedDuration.toMinutes(),
    )
    val startAccessibilityText = stringResource(
        R.string.start_session_accessibility,
        formattedDurationText,
    )

    SessionStage(
        // The countdown sits inside the ring, which is painted behind this slot: the duration being
        // chosen and the time left occupy the same place, so starting changes the number without
        // moving it.
        circle = { RingSlot { MeditationDurationDisplay(duration = state.selectedDuration) } },
        above = {
            Text(
                text = linkedContent?.title ?: stringResource(R.string.free_meditation),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (linkedContent != null) {
                Text(
                    text = linkedContent.description,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        below = {
            Text(
                text = stringResource(R.string.duration),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (linkedContent == null) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.availableDurations.forEach { duration ->
                        DurationChip(
                            duration = duration,
                            isSelected = duration == state.selectedDuration,
                            onClick = { onSelectDuration(duration) },
                        )
                    }
                }
            }
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(56.dp)
                    .semantics { contentDescription = startAccessibilityText },
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.start_session),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
    )
}

/** The stage's circle slot here: the ring is painted behind it, the number stands inside it. */
@Composable
private fun RingSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(SessionStageMetrics.MeditationRing),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SessionContent(
    remainingDuration: Duration,
    status: String,
    stepCue: String?,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onRequestEnd: () -> Unit,
) {
    val countdown = formatRemaining(remainingDuration)
    val countdownDescription = stringResource(
        R.string.countdown_accessibility,
        countdown,
    )
    SessionStage(
        circle = {
            RingSlot {
                Text(
                    text = countdown,
                    modifier = Modifier.semantics { contentDescription = countdownDescription },
                    style = MaterialTheme.typography.displayLarge.tabularNumerals(),
                )
            }
        },
        above = {
            Text(
                text = status,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        below = {
            if (stepCue != null) {
                // One instruction at a time, faded rather than swapped, so the change is noticed
                // without pulling attention. The slot keeps its height so the buttons below never
                // move as a shorter or longer cue takes over.
                Crossfade(
                    targetState = stepCue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = STEP_CUE_MIN_HEIGHT)
                        .padding(bottom = 24.dp),
                    label = "step cue",
                ) { cue ->
                    Text(
                        text = cue,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(primaryActionLabel)
            }
            OutlinedButton(
                onClick = onRequestEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.end))
            }
        },
    )
}
