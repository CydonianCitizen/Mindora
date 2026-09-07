package com.cydoniancitizen.mindora.feature.freemeditation

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.cydoniancitizen.mindora.ui.session.LinkedStepLoading
import com.cydoniancitizen.mindora.ui.session.LinkedStepUnavailable
import com.cydoniancitizen.mindora.ui.session.SessionHaptics
import com.cydoniancitizen.mindora.ui.session.SessionHapticsViewModel
import com.cydoniancitizen.mindora.ui.session.SessionSaveFailed
import com.cydoniancitizen.mindora.ui.session.SessionSaving
import com.cydoniancitizen.mindora.ui.session.SessionStateColumn
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
                        .size(RING_SIZE)
                        .align(Alignment.Center),
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
                    primaryActionLabel = stringResource(R.string.pause),
                    onPrimaryAction = onPause,
                    onRequestEnd = onRequestEnd,
                )

                is FreeMeditationUiState.Paused -> SessionContent(
                    remainingDuration = uiState.remainingDuration,
                    status = stringResource(R.string.paused),
                    primaryActionLabel = stringResource(R.string.resume),
                    onPrimaryAction = onResume,
                    onRequestEnd = onRequestEnd,
                )

                is FreeMeditationUiState.Saving -> SessionSaving()
                is FreeMeditationUiState.Finished -> FinishedContent(
                    state = uiState,
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
            onDismiss = onDismissEnd,
            onConfirm = onConfirmEnd,
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
            .drawBehind {
                if (elapsed > 0L) {
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
                }
            },
    )
}

internal val RING_SIZE = 320.dp

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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val linkedContent = state.linkedContent
            val titleText = linkedContent?.title ?: stringResource(R.string.free_meditation)

            Text(
                text = titleText,
                modifier = Modifier
                    .semantics { heading() }
                    .padding(bottom = 8.dp),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (linkedContent != null) {
                Text(
                    text = linkedContent.description,
                    modifier = Modifier.padding(bottom = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            MeditationDurationDisplay(
                duration = state.selectedDuration,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            Text(
                text = stringResource(R.string.duration),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            if (linkedContent == null) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
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
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

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

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
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
        }
    }
}

@Composable
private fun SessionContent(
    remainingDuration: Duration,
    status: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onRequestEnd: () -> Unit,
) {
    val countdown = formatRemaining(remainingDuration)
    val countdownDescription = stringResource(
        R.string.countdown_accessibility,
        countdown,
    )
    SessionStateColumn {
        Text(
            text = status,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = countdown,
            modifier = Modifier
                .padding(vertical = 32.dp)
                .semantics { contentDescription = countdownDescription },
            style = MaterialTheme.typography.displayLarge.tabularNumerals(),
        )
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
    }
}

@Composable
private fun FinishedContent(
    state: FreeMeditationUiState.Finished,
    onDone: () -> Unit,
) {
    val result = when (state.savedSession.status) {
        MindfulnessSessionStatus.COMPLETED -> stringResource(R.string.session_completed_result)
        MindfulnessSessionStatus.INTERRUPTED -> stringResource(R.string.session_interrupted_result)
    }
    SessionStateColumn {
        Text(
            text = result,
            modifier = Modifier.semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(
                R.string.active_duration_summary,
                formatRemaining(state.savedSession.activeDuration),
            ),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.session_saved),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        ) {
            Text(stringResource(R.string.done))
        }
    }
}

@Composable
private fun EndSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.end_session_title)) },
        text = { Text(stringResource(R.string.end_session_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.end_session))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.continue_session))
            }
        },
    )
}
