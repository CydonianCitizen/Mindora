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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.format.formatElapsed
import com.cydoniancitizen.mindora.core.media.PlaybackFailureResult
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.ui.MindoraTopAppBar
import com.cydoniancitizen.mindora.ui.session.EndSessionDialog
import com.cydoniancitizen.mindora.ui.session.SessionAction
import com.cydoniancitizen.mindora.ui.session.SessionFinished
import com.cydoniancitizen.mindora.ui.session.SessionMessage
import com.cydoniancitizen.mindora.ui.session.SessionProgress
import com.cydoniancitizen.mindora.ui.session.SessionSaveFailed
import com.cydoniancitizen.mindora.ui.session.SessionStateColumn
import com.cydoniancitizen.mindora.ui.theme.tabularNumerals
import java.time.Duration

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
            MindoraTopAppBar(
                title = stringResource(R.string.guided_meditation),
                onBack = { if (active) confirmEnd = true else onNavigateBack() },
                backEnabled = !exitBlocked,
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
        EndSessionDialog(
            title = stringResource(R.string.end_guided_meditation_title),
            message = stringResource(R.string.end_guided_meditation_message),
            confirmLabel = stringResource(R.string.end_meditation),
            dismissLabel = stringResource(R.string.continue_meditation),
            onConfirm = {
                confirmEnd = false
                onEnd()
            },
            onDismiss = { confirmEnd = false },
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
        GuidedMeditationUiState.LoadingContent -> SessionProgress(
            stringResource(R.string.loading_guided_meditation),
        )
        GuidedMeditationUiState.Unavailable -> SessionMessage(
            title = stringResource(R.string.guided_meditation_unavailable),
            primaryAction = SessionAction(stringResource(R.string.done), onNavigateBack),
        )
        GuidedMeditationUiState.ContentFailed -> SessionMessage(
            title = stringResource(R.string.guided_content_error),
            primaryAction = SessionAction(stringResource(R.string.done), onNavigateBack),
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
        is GuidedMeditationUiState.Saving -> SessionProgress(
            stringResource(R.string.saving_session),
        )
        is GuidedMeditationUiState.Finished -> SessionFinished(
            title = stringResource(
                if (uiState.status == MindfulnessSessionStatus.COMPLETED) {
                    R.string.guided_completed_result
                } else {
                    R.string.guided_interrupted_result
                },
            ),
            activeDuration = formatElapsed(uiState.activeDuration),
            onDone = onDone,
        )
        is GuidedMeditationUiState.SaveFailed -> SessionSaveFailed(
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
        Text(
            state.meditation.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            state.meditation.description,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            stringResource(
                R.string.practice_duration,
                formatElapsed(state.meditation.plannedDuration),
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
    SessionStateColumn {
        Text(
            state.meditation.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
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
        formatElapsed(position),
        formatElapsed(totalDuration),
    )
    SessionStateColumn {
        Text(
            meditation.title,
            modifier = Modifier.semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(status, modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .semantics { stateDescription = progressDescription },
        )
        Text(
            stringResource(
                R.string.guided_elapsed_total,
                formatElapsed(position),
                formatElapsed(totalDuration),
            ),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge.tabularNumerals(),
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
private fun PlaybackFailedContent(
    state: GuidedMeditationUiState.PlaybackFailed,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    val message = if (state.result == PlaybackFailureResult.INTERRUPTED_SAVED) {
        stringResource(
            R.string.guided_playback_failed_saved,
            formatElapsed(state.activeDuration),
        )
    } else {
        stringResource(R.string.guided_playback_failed)
    }
    SessionMessage(
        title = message,
        primaryAction = if (state.result == PlaybackFailureResult.NOTHING_SAVED) {
            SessionAction(stringResource(R.string.retry_playback), onRetry)
        } else {
            null
        },
        secondaryAction = SessionAction(stringResource(R.string.done), onDone),
    )
}
