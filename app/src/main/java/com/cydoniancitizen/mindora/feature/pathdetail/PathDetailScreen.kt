package com.cydoniancitizen.mindora.feature.pathdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import java.time.Duration
import java.util.Locale

@Composable
fun PathDetailScreen(
    onNavigateBack: () -> Unit,
    onStepClick: (PathStepType, String) -> Unit,
    viewModel: PathDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PathDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onStepClick = onStepClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PathDetailScreen(
    uiState: PathDetailUiState,
    onNavigateBack: () -> Unit,
    onStepClick: (PathStepType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.mindfulness_path),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            PathDetailUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            PathDetailUiState.Unavailable -> PathMessage(
                message = stringResource(R.string.path_unavailable),
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(innerPadding),
            )

            PathDetailUiState.Error -> PathMessage(
                message = stringResource(R.string.path_loading_error),
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(innerPadding),
            )

            is PathDetailUiState.Content -> PathContent(
                state = uiState,
                onStepClick = onStepClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun PathMessage(
    message: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(top = 20.dp),
        ) {
            Text(stringResource(R.string.back_to_practice))
        }
    }
}

@Composable
private fun PathContent(
    state: PathDetailUiState.Content,
    onStepClick: (PathStepType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val progressDescription = pluralStringResource(
                R.plurals.path_progress,
                state.totalSteps,
                state.completedSteps,
                state.totalSteps,
            )
            Text(
                state.title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                state.description,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                progressDescription,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(
                progress = { state.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .semantics { stateDescription = progressDescription },
            )
        }
        items(
            items = state.steps,
            key = PathStepUiModel::id,
        ) { step ->
            PathStepCard(
                step = step,
                onClick = { onStepClick(step.type, step.id) },
            )
        }
    }
}

@Composable
private fun PathStepCard(
    step: PathStepUiModel,
    onClick: () -> Unit,
) {
    val type = when (step.type) {
        PathStepType.GUIDED_MEDITATION -> stringResource(R.string.guided_meditation)
        PathStepType.FREE_MEDITATION -> stringResource(R.string.free_meditation)
        PathStepType.BREATHING_EXERCISE -> stringResource(R.string.breathing_exercise)
    }
    val completion = stringResource(
        if (step.completed) R.string.completed else R.string.not_completed,
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.path_step_number, step.ordinal, step.title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                step.description,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                type,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                stringResource(
                    R.string.practice_duration,
                    formatPathStepDuration(step.displayDuration),
                ),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                completion,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
            TextButton(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.open_step))
            }
        }
    }
}

internal fun formatPathStepDuration(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}
