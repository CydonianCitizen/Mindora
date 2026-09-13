package com.cydoniancitizen.mindora.feature.whitenoise

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.format.formatRemaining
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.feature.freemeditation.DurationChip
import com.cydoniancitizen.mindora.ui.MindoraTopAppBar
import com.cydoniancitizen.mindora.ui.session.SessionFinished
import com.cydoniancitizen.mindora.ui.session.SessionSaveFailed
import com.cydoniancitizen.mindora.ui.session.SessionSaving
import com.cydoniancitizen.mindora.ui.session.SessionStateColumn
import java.time.Duration
import kotlinx.coroutines.delay

@Composable
fun WhiteNoiseScreen(
    onNavigateBack: () -> Unit,
    viewModel: WhiteNoiseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onBack = {
        when (uiState) {
            is WhiteNoiseUiState.Running -> viewModel.stop()
            is WhiteNoiseUiState.Setup, is WhiteNoiseUiState.Finished -> onNavigateBack()
            else -> Unit
        }
    }
    BackHandler(onBack = onBack)
    WhiteNoiseScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectDuration = viewModel::selectDuration,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onRetrySave = viewModel::retrySave,
        onDiscard = viewModel::discard,
        onDone = {
            viewModel.done()
            onNavigateBack()
        },
    )
}

@Composable
internal fun WhiteNoiseScreen(
    uiState: WhiteNoiseUiState,
    onBack: () -> Unit,
    onSelectDuration: (Duration) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRetrySave: () -> Unit,
    onDiscard: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MindoraTopAppBar(
            title = stringResource(R.string.white_noise),
            onBack = onBack,
            backEnabled = uiState !is WhiteNoiseUiState.Saving &&
                uiState !is WhiteNoiseUiState.SaveFailed,
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (uiState) {
                is WhiteNoiseUiState.Setup -> SetupContent(uiState, onSelectDuration, onStart)
                is WhiteNoiseUiState.Running -> RunningContent(uiState, onStop)
                WhiteNoiseUiState.Saving -> SessionSaving()
                is WhiteNoiseUiState.Finished -> SessionFinished(
                    title = stringResource(
                        if (uiState.status == MindfulnessSessionStatus.COMPLETED) {
                            R.string.white_noise_finished
                        } else {
                            R.string.session_interrupted_result
                        },
                    ),
                    activeDuration = formatRemaining(uiState.activeDuration),
                    onDone = onDone,
                )
                WhiteNoiseUiState.SaveFailed -> SessionSaveFailed(onRetrySave, onDiscard)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupContent(
    state: WhiteNoiseUiState.Setup,
    onSelectDuration: (Duration) -> Unit,
    onStart: () -> Unit,
) {
    SessionStateColumn {
        Text(
            text = stringResource(R.string.white_noise),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.duration),
            modifier = Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            shape = CircleShape,
        ) {
            Text(stringResource(R.string.start_session))
        }
    }
}

@Composable
private fun RunningContent(
    state: WhiteNoiseUiState.Running,
    onStop: () -> Unit,
) {
    // View-only countdown from a local clock; the service owns the timer that actually ends the
    // session, so a drifting or backgrounded screen cannot cut it short or run it long.
    val startedAt = remember(state.total) { SystemClock.elapsedRealtime() }
    val remaining by produceState(initialValue = state.total, state.total) {
        while (true) {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            value = state.total.minusMillis(elapsed).coerceAtLeast(Duration.ZERO)
            delay(1_000L)
        }
    }
    SessionStateColumn {
        Text(
            text = if (state.preparing) {
                stringResource(R.string.preparing_audio)
            } else {
                stringResource(R.string.running)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatRemaining(remaining),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.displayMedium,
        )
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        ) {
            Text(stringResource(R.string.white_noise_stop))
        }
    }
}
