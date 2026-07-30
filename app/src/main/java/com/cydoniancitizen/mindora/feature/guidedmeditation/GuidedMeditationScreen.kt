package com.cydoniancitizen.mindora.feature.guidedmeditation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.media.PlaybackFailureResult
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import java.time.Duration
import java.util.Locale

@Composable
fun GuidedMeditationScreen(
    onNavigateBack: () -> Unit,
    viewModel: GuidedMeditationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GuidedMeditationScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onStart = viewModel::start,
        onPlay = viewModel::play,
        onPause = viewModel::pause,
        onEnd = viewModel::end,
        onRetrySave = viewModel::retrySave,
        onDiscard = viewModel::discard,
        onClear = viewModel::clear,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GuidedMeditationScreen(
    uiState: GuidedMeditationUiState,
    onNavigateBack: () -> Unit,
    onStart: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit,
    onRetrySave: () -> Unit,
    onDiscard: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    val active = uiState is GuidedMeditationUiState.Preparing ||
        uiState is GuidedMeditationUiState.Playing ||
        uiState is GuidedMeditationUiState.Paused
    val exitBlocked = uiState is GuidedMeditationUiState.Saving ||
        uiState is GuidedMeditationUiState.SaveFailed

    BackHandler(enabled = active) { confirmEnd = true }
    BackHandler(enabled = exitBlocked) { }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guided_meditation)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                active -> confirmEnd = true
                                exitBlocked -> Unit
                                else -> onNavigateBack()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            GuidedMeditationContent(
                uiState = uiState,
                onStart = onStart,
                onPlay = onPlay,
                onPause = onPause,
                onRequestEnd = { confirmEnd = true },
                onRetrySave = onRetrySave,
                onDiscard = onDiscard,
                onDone = {
                    onClear()
                    onNavigateBack()
                },
                onNavigateBack = onNavigateBack,
            )
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text(stringResource(R.string.end_guided_meditation_title)) },
            text = { Text(stringResource(R.string.end_guided_meditation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmEnd = false
                        onEnd()
                    },
                ) {
                    Text(stringResource(R.string.end_meditation))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) {
                    Text(stringResource(R.string.continue_meditation))
                }
            },
        )
    }
}

@Composable
private fun GuidedMeditationContent(
    uiState: GuidedMeditationUiState,
    onStart: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onRequestEnd: () -> Unit,
    onRetrySave: () -> Unit,
    onDiscard: () -> Unit,
    onDone: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    when (uiState) {
        GuidedMeditationUiState.LoadingContent -> CenteredProgress(
            stringResource(R.string.loading_guided_meditation),
        )
        GuidedMeditationUiState.Unavailable -> MessageContent(
            message = stringResource(R.string.guided_meditation_unavailable),
            actionLabel = stringResource(R.string.done),
            onAction = onNavigateBack,
        )
        GuidedMeditationUiState.ContentFailed -> MessageContent(
            message = stringResource(R.string.guided_content_error),
            actionLabel = stringResource(R.string.done),
            onAction = onNavigateBack,
        )
        is GuidedMeditationUiState.Ready -> ReadyContent(uiState, onStart)
        is GuidedMeditationUiState.Preparing -> PreparingContent(uiState)
        is GuidedMeditationUiState.Playing -> PlaybackContent(
            meditation = uiState.meditation,
            status = stringResource(R.string.playing),
            position = uiState.position,
            totalDuration = uiState.totalDuration,
            primaryLabel = stringResource(R.string.pause),
            onPrimary = onPause,
            onRequestEnd = onRequestEnd,
        )
        is GuidedMeditationUiState.Paused -> PlaybackContent(
            meditation = uiState.meditation,
            status = stringResource(R.string.paused),
            position = uiState.position,
            totalDuration = uiState.totalDuration,
            primaryLabel = stringResource(R.string.resume),
            onPrimary = onPlay,
            onRequestEnd = onRequestEnd,
        )
        is GuidedMeditationUiState.Saving -> CenteredProgress(
            stringResource(R.string.saving_session),
        )
        is GuidedMeditationUiState.Finished -> FinishedContent(uiState, onDone)
        is GuidedMeditationUiState.SaveFailed -> SaveFailedContent(
            onRetrySave = onRetrySave,
            onDiscard = onDiscard,
        )
        is GuidedMeditationUiState.PlaybackFailed -> PlaybackFailedContent(
            state = uiState,
            onRetry = onStart,
            onDone = onDone,
        )
    }
}

@Composable
private fun ReadyContent(
    state: GuidedMeditationUiState.Ready,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.meditation.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            state.meditation.description,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            stringResource(
                R.string.practice_duration,
                formatGuidedDuration(state.meditation.plannedDuration),
            ),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.labelLarge,
        )
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        ) {
            Text(stringResource(R.string.start))
        }
    }
}

@Composable
private fun PreparingContent(state: GuidedMeditationUiState.Preparing) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.meditation.title, style = MaterialTheme.typography.headlineSmall)
        CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
        Text(
            stringResource(R.string.preparing_audio),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun PlaybackContent(
    meditation: GuidedMeditationDetails,
    status: String,
    position: Duration,
    totalDuration: Duration,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onRequestEnd: () -> Unit,
) {
    val safeTotalMillis = totalDuration.toMillis().coerceAtLeast(1L)
    val progress = (position.toMillis().toFloat() / safeTotalMillis).coerceIn(0f, 1f)
    val progressDescription = stringResource(
        R.string.guided_progress_accessibility,
        status,
        formatGuidedDuration(position),
        formatGuidedDuration(totalDuration),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(meditation.title, textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium)
        Text(status, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .semantics { contentDescription = progressDescription },
        )
        Text(
            stringResource(
                R.string.guided_elapsed_total,
                formatGuidedDuration(position),
                formatGuidedDuration(totalDuration),
            ),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        ) {
            Text(primaryLabel)
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
    state: GuidedMeditationUiState.Finished,
    onDone: () -> Unit,
) {
    val result = if (state.status == MindfulnessSessionStatus.COMPLETED) {
        stringResource(R.string.guided_completed_result)
    } else {
        stringResource(R.string.guided_interrupted_result)
    }
    MessageContent(
        message = "$result\n${stringResource(
            R.string.active_duration_summary,
            formatGuidedDuration(state.activeDuration),
        )}\n${stringResource(R.string.session_saved)}",
        actionLabel = stringResource(R.string.done),
        onAction = onDone,
    )
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
            stringResource(R.string.session_save_failed),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            stringResource(R.string.session_discard_explanation),
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetrySave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) { Text(stringResource(R.string.retry)) }
        OutlinedButton(
            onClick = onDiscard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) { Text(stringResource(R.string.discard)) }
    }
}

@Composable
private fun PlaybackFailedContent(
    state: GuidedMeditationUiState.PlaybackFailed,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    val message = if (state.result == PlaybackFailureResult.INTERRUPTED_SAVED) {
        stringResource(
            R.string.guided_playback_failed_saved,
            formatGuidedDuration(state.activeDuration),
        )
    } else {
        stringResource(R.string.guided_playback_failed)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
        if (state.result == PlaybackFailureResult.NOTHING_SAVED) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) { Text(stringResource(R.string.retry_playback)) }
        }
        OutlinedButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) { Text(stringResource(R.string.done)) }
    }
}

@Composable
private fun CenteredProgress(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(message, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun MessageContent(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) { Text(actionLabel) }
    }
}

internal fun formatGuidedDuration(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}
