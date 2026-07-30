package com.cydoniancitizen.mindora.feature.freemeditation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
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
) {
    Column(modifier = modifier.fillMaxSize()) {
        FreeMeditationHeader(
            backEnabled = uiState !is FreeMeditationUiState.Saving &&
                uiState !is FreeMeditationUiState.SaveFailed,
            onBack = onBack,
        )

        when (uiState) {
            FreeMeditationUiState.LoadingContent -> LinkedContentLoading()
            FreeMeditationUiState.Unavailable -> LinkedContentUnavailable(onBack)
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

            is FreeMeditationUiState.Saving -> SavingContent()
            is FreeMeditationUiState.Finished -> FinishedContent(
                state = uiState,
                onDone = onDone,
            )

            is FreeMeditationUiState.SaveFailed -> SaveFailedContent(
                onRetrySave = onRetrySave,
                onDiscard = onDiscard,
            )
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

@Composable
private fun FreeMeditationHeader(
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
            text = stringResource(R.string.free_meditation),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun SetupContent(
    state: FreeMeditationUiState.Setup,
    onSelectDuration: (Duration) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val linkedContent = state.linkedContent
        if (linkedContent == null) {
            Text(
                text = stringResource(R.string.select_duration),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.availableDurations.forEach { duration ->
                    FilterChip(
                        selected = duration == state.selectedDuration,
                        onClick = { onSelectDuration(duration) },
                        label = {
                            Text(
                                stringResource(
                                    R.string.duration_minutes,
                                    duration.toMinutes(),
                                ),
                            )
                        },
                    )
                }
            }
        } else {
            Text(
                text = linkedContent.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = linkedContent.description,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    R.string.practice_duration,
                    formatCountdown(linkedContent.plannedDuration),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
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
    remainingDuration: Duration,
    status: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onRequestEnd: () -> Unit,
) {
    val countdown = formatCountdown(remainingDuration)
    val countdownDescription = stringResource(
        R.string.countdown_accessibility,
        countdown,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = countdown,
            modifier = Modifier
                .padding(vertical = 32.dp)
                .semantics { contentDescription = countdownDescription },
            style = MaterialTheme.typography.displayLarge,
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
private fun SavingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
    state: FreeMeditationUiState.Finished,
    onDone: () -> Unit,
) {
    val result = when (state.savedSession.status) {
        MindfulnessSessionStatus.COMPLETED -> stringResource(R.string.session_completed_result)
        MindfulnessSessionStatus.INTERRUPTED -> stringResource(R.string.session_interrupted_result)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = result,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(
                R.string.active_duration_summary,
                formatCountdown(state.savedSession.activeDuration),
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.session_save_failed),
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
