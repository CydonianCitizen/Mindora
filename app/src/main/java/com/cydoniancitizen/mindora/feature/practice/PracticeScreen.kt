package com.cydoniancitizen.mindora.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R

@Composable
fun PracticeScreen(
    onFreeMeditationClick: () -> Unit,
    onBreathingExerciseClick: () -> Unit,
    onPathClick: (String) -> Unit,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PracticeScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onFreeMeditationClick = onFreeMeditationClick,
        onBreathingExerciseClick = onBreathingExerciseClick,
        onPathClick = onPathClick,
    )
}

@Composable
internal fun PracticeScreen(
    uiState: PracticeUiState,
    onRetry: () -> Unit,
    onFreeMeditationClick: () -> Unit,
    onBreathingExerciseClick: () -> Unit,
    onPathClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.practice),
                modifier = Modifier
                    .padding(
                        start = 24.dp,
                        top = 24.dp,
                        end = 24.dp,
                        bottom = 4.dp,
                    )
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            PracticeStandaloneAction(
                title = stringResource(R.string.free_meditation),
                description = stringResource(R.string.free_meditation_description),
                onClick = onFreeMeditationClick,
            )
        }
        item {
            PracticeStandaloneAction(
                title = stringResource(R.string.breathing_exercise),
                description = stringResource(R.string.breathing_exercise_description),
                onClick = onBreathingExerciseClick,
            )
        }
        item {
            Text(
                text = stringResource(R.string.bundled_mindfulness_paths),
                modifier = Modifier
                    .padding(
                        start = 24.dp,
                        top = 12.dp,
                        end = 24.dp,
                    )
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        when (uiState) {
            PracticeUiState.Loading -> item { PracticeLoading() }
            PracticeUiState.Empty -> item {
                PracticeMessage(message = stringResource(R.string.practice_empty_message))
            }
            PracticeUiState.Error -> item { PracticeError(onRetry = onRetry) }
            is PracticeUiState.Content -> items(
                items = uiState.paths,
                key = MindfulnessPathSummary::id,
            ) { path ->
                PracticePathCard(
                    path = path,
                    onClick = { onPathClick(path.id) },
                )
            }
        }
    }
}

@Composable
private fun PracticeStandaloneAction(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PracticeLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PracticeMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun PracticeError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.practice_error_message),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
private fun PracticePathCard(
    path: MindfulnessPathSummary,
    onClick: () -> Unit,
) {
    val progressDescription = pluralStringResource(
        R.plurals.path_progress,
        path.totalSteps,
        path.completedSteps,
        path.totalSteps,
    )
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = path.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = path.description,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = progressDescription,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(
                progress = { path.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .semantics { stateDescription = progressDescription },
            )
        }
    }
}
