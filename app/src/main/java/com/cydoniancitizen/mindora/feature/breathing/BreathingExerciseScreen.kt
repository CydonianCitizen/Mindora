package com.cydoniancitizen.mindora.feature.breathing

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.format.formatRemaining
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.ui.BreathEasing
import com.cydoniancitizen.mindora.ui.MindoraTopAppBar
import com.cydoniancitizen.mindora.ui.endlessMotionAllowed
import com.cydoniancitizen.mindora.ui.session.LinkedStepLoading
import com.cydoniancitizen.mindora.ui.session.LinkedStepUnavailable
import com.cydoniancitizen.mindora.ui.session.SessionSaveFailed
import com.cydoniancitizen.mindora.ui.session.SessionSaving
import com.cydoniancitizen.mindora.ui.session.SessionStateColumn
import com.cydoniancitizen.mindora.ui.softGlow
import com.cydoniancitizen.mindora.ui.systemAnimationsEnabled
import com.cydoniancitizen.mindora.ui.theme.tabularNumerals
import java.time.Duration

@Composable
fun BreathingExerciseScreen(
    onNavigateBack: () -> Unit,
    viewModel: BreathingExerciseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onBack = {
        when (uiState) {
            BreathingExerciseUiState.LoadingContent,
            BreathingExerciseUiState.Unavailable,
            is BreathingExerciseUiState.Setup,
            is BreathingExerciseUiState.Finished,
            -> onNavigateBack()

            is BreathingExerciseUiState.Running,
            is BreathingExerciseUiState.Paused,
            -> viewModel.requestEnd()

            is BreathingExerciseUiState.Saving,
            is BreathingExerciseUiState.SaveFailed,
            -> Unit
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshTime()
    }
    BackHandler(onBack = onBack)
    BreathingExerciseScreen(
        uiState = uiState,
        onBack = onBack,
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onRequestEnd = viewModel::requestEnd,
        onDismissEnd = viewModel::dismissEndRequest,
        onConfirmEnd = viewModel::confirmEnd,
        onRetrySave = viewModel::retrySave,
        onDiscard = viewModel::discard,
        onDone = onNavigateBack,
    )
}

@Composable
internal fun BreathingExerciseScreen(
    uiState: BreathingExerciseUiState,
    onBack: () -> Unit,
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
) {
    Column(modifier = modifier.fillMaxSize()) {
        MindoraTopAppBar(
            title = stringResource(R.string.breathing_exercise),
            onBack = onBack,
            backEnabled = uiState !is BreathingExerciseUiState.Saving &&
                uiState !is BreathingExerciseUiState.SaveFailed,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (uiState) {
                BreathingExerciseUiState.LoadingContent -> LinkedStepLoading()
                BreathingExerciseUiState.Unavailable -> LinkedStepUnavailable(onBack)
                is BreathingExerciseUiState.Setup -> SetupContent(
                    state = uiState,
                    onStart = onStart,
                )

                is BreathingExerciseUiState.Running -> SessionContent(
                    phase = uiState.currentPhase,
                    cycle = uiState.currentCycle,
                    totalCycles = uiState.totalCycles,
                    phaseRemainingDuration = uiState.phaseRemainingDuration,
                    totalRemainingDuration = uiState.totalRemainingDuration,
                    phaseProgress = uiState.phaseProgress,
                    status = stringResource(R.string.running),
                    primaryActionLabel = stringResource(R.string.pause),
                    onPrimaryAction = onPause,
                    onRequestEnd = onRequestEnd,
                )

                is BreathingExerciseUiState.Paused -> SessionContent(
                    phase = uiState.currentPhase,
                    cycle = uiState.currentCycle,
                    totalCycles = uiState.totalCycles,
                    phaseRemainingDuration = uiState.phaseRemainingDuration,
                    totalRemainingDuration = uiState.totalRemainingDuration,
                    phaseProgress = uiState.phaseProgress,
                    status = stringResource(R.string.paused),
                    primaryActionLabel = stringResource(R.string.resume),
                    onPrimaryAction = onResume,
                    onRequestEnd = onRequestEnd,
                )

                is BreathingExerciseUiState.Saving -> SessionSaving()
                is BreathingExerciseUiState.Finished -> FinishedContent(
                    state = uiState,
                    onDone = onDone,
                )

                is BreathingExerciseUiState.SaveFailed -> SessionSaveFailed(
                    onRetrySave = onRetrySave,
                    onDiscard = onDiscard,
                )
            }
        }
    }

    if (
        (uiState is BreathingExerciseUiState.Running && uiState.confirmEnd) ||
        (uiState is BreathingExerciseUiState.Paused && uiState.confirmEnd)
    ) {
        EndExerciseDialog(
            onDismiss = onDismissEnd,
            onConfirm = onConfirmEnd,
        )
    }
}

@Composable
private fun SetupContent(
    state: BreathingExerciseUiState.Setup,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val linkedContent = state.linkedContent
        Text(
            text = linkedContent?.title ?: stringResource(R.string.breathing_exercise),
            modifier = Modifier.semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = linkedContent?.description
                ?: stringResource(R.string.breathing_exercise_description),
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = pluralStringResource(
                R.plurals.breathing_cycle_count,
                state.totalCycles,
                state.totalCycles,
            ),
            modifier = Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = pluralStringResource(
                R.plurals.breathing_approximate_duration,
                state.plannedDuration.seconds.toInt(),
                state.plannedDuration.seconds,
            ),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.start))
        }
    }
}

@Composable
private fun SessionContent(
    phase: BreathingPhase,
    cycle: Int,
    totalCycles: Int,
    phaseRemainingDuration: Duration,
    totalRemainingDuration: Duration,
    phaseProgress: Float,
    status: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onRequestEnd: () -> Unit,
) {
    val phaseLabel = phase.label()
    val phaseCountdown = formatRemaining(phaseRemainingDuration)
    val totalCountdown = formatRemaining(totalRemainingDuration)
    val cycleLabel = stringResource(R.string.breathing_cycle_format, cycle, totalCycles)
    val phaseRemainingLabel = stringResource(
        R.string.breathing_phase_remaining,
        phaseCountdown,
    )
    val totalRemainingLabel = stringResource(
        R.string.breathing_exercise_remaining,
        totalCountdown,
    )
    val guideDescription = stringResource(
        R.string.breathing_guide_accessibility,
        phaseLabel,
        cycleLabel,
        phaseRemainingLabel,
    )

    SessionStateColumn {
        Text(
            text = status,
            modifier = Modifier.testTag(BreathingTestTags.STATUS_TEXT),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Column(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = guideDescription
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = cycleLabel,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(BreathingTestTags.CYCLE_TEXT),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            BreathingGuide(
                phase = phase,
                phaseProgress = phaseProgress,
            )
            Text(
                text = phaseLabel,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .testTag(BreathingTestTags.PHASE_TEXT),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = phaseCountdown,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(BreathingTestTags.COUNTDOWN_TEXT),
                style = MaterialTheme.typography.displayMedium.tabularNumerals(),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        Text(
            text = totalRemainingLabel,
            modifier = Modifier
                .padding(top = 12.dp)
                .testTag(BreathingTestTags.TOTAL_REMAINING_TEXT),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
                .testTag(BreathingTestTags.PRIMARY_BUTTON),
        ) {
            Text(primaryActionLabel)
        }
        OutlinedButton(
            onClick = onRequestEnd,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .testTag(BreathingTestTags.END_BUTTON),
        ) {
            Text(stringResource(R.string.end))
        }
    }
}

internal object BreathingTestTags {
    const val CONTAINER = "breathing_container"
    const val CIRCLE = "breathing_circle"
    const val STATUS_TEXT = "breathing_status_text"
    const val CYCLE_TEXT = "breathing_cycle_text"
    const val PHASE_TEXT = "breathing_phase_text"
    const val COUNTDOWN_TEXT = "breathing_countdown_text"
    const val TOTAL_REMAINING_TEXT = "breathing_total_remaining_text"
    const val PRIMARY_BUTTON = "breathing_primary_button"
    const val END_BUTTON = "breathing_end_button"
}

internal val VisualScaleSemanticsKey = SemanticsPropertyKey<Float>("VisualScale")
internal var SemanticsPropertyReceiver.visualScale by VisualScaleSemanticsKey

@Composable
private fun BreathingGuide(
    phase: BreathingPhase,
    phaseProgress: Float,
    modifier: Modifier = Modifier,
) {
    val openness = if (!systemAnimationsEnabled()) {
        HELD_OPENNESS
    } else {
        BreathEasing.transform(
            when (phase) {
                BreathingPhase.INHALE -> phaseProgress
                BreathingPhase.HOLD_AFTER_INHALE -> 1f
                BreathingPhase.EXHALE -> 1f - phaseProgress
                BreathingPhase.HOLD_AFTER_EXHALE -> 0f
            }.coerceIn(0f, 1f),
        )
    }

    // The view model reports progress ten times a second. Interpolating between those reports over
    // exactly one tick turns a visible ten-step staircase into motion at display rate, without
    // moving the breathing clock out of the view model that owns it.
    //
    // Because a report always arrives as the previous interpolation ends, this animates without a
    // pause for as long as the exercise runs — so it asks first, and snaps when never-ending motion
    // is not allowed. Snapping also leaves the value a test reads exactly on the reported progress.
    val animatedOpenness = remember { Animatable(openness) }
    LaunchedEffect(openness) {
        if (endlessMotionAllowed()) {
            animatedOpenness.animateTo(
                targetValue = openness,
                animationSpec = tween(BREATHING_TICK_MILLIS, easing = LinearEasing),
            )
        } else {
            animatedOpenness.snapTo(openness)
        }
    }
    val minScale = MIN_CIRCLE.value / MAX_CIRCLE.value
    // No hard rim: the disc is solid at the core and dissolves into the surface behind it, so the
    // breath reads as something expanding rather than as a shape being resized.
    val core = MaterialTheme.colorScheme.primaryContainer
    val glow = remember(core) {
        softGlow(
            color = core,
            0.00f to 1f,
            0.45f to 1f,
            0.70f to 0.50f,
            0.88f to 0.12f,
            1.00f to 0f,
        )
    }

    Box(
        modifier = modifier
            .padding(top = 24.dp)
            .size(MAX_CIRCLE)
            .testTag(BreathingTestTags.CONTAINER),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Read inside the lambda so a frame re-runs the layer, never the composable.
                .graphicsLayer {
                    val scale = minScale + (1f - minScale) * animatedOpenness.value
                    scaleX = scale
                    scaleY = scale
                }
                .background(glow)
                // The tag goes inside the cleared block, not after it: clearAndSetSemantics
                // replaces the whole configuration, so a testTag chained afterwards is thrown
                // away and the circle becomes unreachable from a test in either tree.
                .clearAndSetSemantics {
                    testTag = BreathingTestTags.CIRCLE
                    visualScale = minScale + (1f - minScale) * animatedOpenness.value
                },
        )
    }
}

private val MAX_CIRCLE = 220.dp
private val MIN_CIRCLE = 128.dp

/** Matches BreathingExerciseViewModel's tick, so one report interpolates into the next. */
private const val BREATHING_TICK_MILLIS = 100

/** Half open, so a circle held still by "Remove animations" does not imply a phase. */
private const val HELD_OPENNESS = 0.5f


@Composable
private fun FinishedContent(
    state: BreathingExerciseUiState.Finished,
    onDone: () -> Unit,
) {
    val result = when (state.savedSessionStatus) {
        MindfulnessSessionStatus.COMPLETED ->
            stringResource(R.string.breathing_completed_result)

        MindfulnessSessionStatus.INTERRUPTED ->
            stringResource(R.string.breathing_interrupted_result)
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
                formatRemaining(state.activeDuration),
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
private fun EndExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.end_breathing_exercise_title)) },
        text = { Text(stringResource(R.string.end_breathing_exercise_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.end_exercise))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.continue_exercise))
            }
        },
    )
}

@Composable
private fun BreathingPhase.label(): String = when (this) {
    BreathingPhase.INHALE -> stringResource(R.string.inhale)
    BreathingPhase.HOLD_AFTER_INHALE,
    BreathingPhase.HOLD_AFTER_EXHALE,
    -> stringResource(R.string.hold)

    BreathingPhase.EXHALE -> stringResource(R.string.exhale)
}

