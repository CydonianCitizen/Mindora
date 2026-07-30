package com.cydoniancitizen.mindora.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.res.stringResource
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
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.practice),
            modifier = Modifier.padding(
                start = 24.dp,
                top = 24.dp,
                end = 24.dp,
                bottom = 16.dp,
            ),
            style = MaterialTheme.typography.headlineMedium,
        )

        PracticeStandaloneAction(
            title = stringResource(R.string.free_meditation),
            description = stringResource(R.string.free_meditation_description),
            onClick = onFreeMeditationClick,
        )
        PracticeStandaloneAction(
            title = stringResource(R.string.breathing_exercise),
            description = stringResource(R.string.breathing_exercise_description),
            onClick = onBreathingExerciseClick,
            modifier = Modifier.padding(top = 12.dp),
        )

        Text(
            text = stringResource(R.string.bundled_mindfulness_paths),
            modifier = Modifier.padding(
                start = 24.dp,
                top = 24.dp,
                end = 24.dp,
                bottom = 12.dp,
            ),
            style = MaterialTheme.typography.titleMedium,
        )

        when (uiState) {
            PracticeUiState.Loading -> PracticeLoading()
            PracticeUiState.Empty -> PracticeMessage(
                message = stringResource(R.string.practice_empty_message),
            )

            PracticeUiState.Error -> PracticeError(onRetry = onRetry)
            is PracticeUiState.Content -> PracticePathList(
                paths = uiState.paths,
                onPathClick = onPathClick,
            )
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
private fun ColumnScope.PracticeLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ColumnScope.PracticeMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
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
private fun ColumnScope.PracticeError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
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
private fun ColumnScope.PracticePathList(
    paths: List<MindfulnessPathSummary>,
    onPathClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = paths,
            key = MindfulnessPathSummary::id,
        ) { path ->
            Card(
                onClick = { onPathClick(path.id) },
                modifier = Modifier.fillMaxWidth(),
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
                        text = stringResource(
                            R.string.path_progress,
                            path.completedSteps,
                            path.totalSteps,
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LinearProgressIndicator(
                        progress = { path.progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
