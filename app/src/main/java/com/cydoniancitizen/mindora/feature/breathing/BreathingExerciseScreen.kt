package com.cydoniancitizen.mindora.feature.breathing

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import java.time.Duration
import java.util.Locale

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
        BreathingHeader(
            backEnabled = uiState !is BreathingExerciseUiState.Saving &&
                uiState !is BreathingExerciseUiState.SaveFailed,
            onBack = onBack,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (uiState) {
                BreathingExerciseUiState.LoadingContent -> LinkedContentLoading()
                BreathingExerciseUiState.Unavailable -> LinkedContentUnavailable(onBack)
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

                is BreathingExerciseUiState.Saving -> SavingContent()
                is BreathingExerciseUiState.Finished -> FinishedContent(
                    state = uiState,
                    onDone = onDone,
                )

                is BreathingExerciseUiState.SaveFailed -> SaveFailedContent(
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
private fun BreathingHeader(
    backEnabled: Boolean,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            enabled = backEnabled,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back),
            )
        }
        Text(
            text = stringResource(R.string.breathing_exercise),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
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
private fun LinkedContentLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.loading_practice_step),
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun LinkedContentUnavailable(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.practice_step_unavailable),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 20.dp),
        ) {
            Text(stringResource(R.string.back_to_path))
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
    val phaseCountdown = formatBreathingCountdown(phaseRemainingDuration)
    val totalCountdown = formatBreathingCountdown(totalRemainingDuration)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.titleMedium,
        )
        Column(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = guideDescription
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = cycleLabel,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            BreathingGuide(
                phase = phase,
                phaseProgress = phaseProgress,
            )
            Text(
                text = phaseLabel,
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = phaseCountdown,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.displayMedium,
            )
        }
        Text(
            text = totalRemainingLabel,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
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
private fun BreathingGuide(
    phase: BreathingPhase,
    phaseProgress: Float,
) {
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) >= 1f
    }
    val visualProgress = if (!motionEnabled) {
        0.5f
    } else {
        when (phase) {
            BreathingPhase.INHALE -> phaseProgress
            BreathingPhase.HOLD_AFTER_INHALE -> 1f
            BreathingPhase.EXHALE -> 1f - phaseProgress
            BreathingPhase.HOLD_AFTER_EXHALE -> 0f
        }
    }.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .padding(top = 24.dp)
            .size((128f + 92f * visualProgress).dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            )
            .clearAndSetSemantics { },
    )
}

@Composable
private fun SavingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.saving_session),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = result,
            modifier = Modifier.semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(
                R.string.active_duration_summary,
                formatBreathingCountdown(state.activeDuration),
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
private fun SaveFailedContent(
    onRetrySave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.session_save_failed),
            modifier = Modifier.semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.session_discard_explanation),
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onRetrySave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Text(stringResource(R.string.retry))
        }
        OutlinedButton(
            onClick = onDiscard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text(stringResource(R.string.discard))
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

internal fun formatBreathingCountdown(duration: Duration): String {
    val totalSeconds = if (duration.isZero || duration.isNegative) {
        0
    } else {
        (duration.toMillis() + 999) / 1_000
    }
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}
