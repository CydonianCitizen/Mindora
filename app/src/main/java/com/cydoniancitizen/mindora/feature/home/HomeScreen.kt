package com.cydoniancitizen.mindora.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import java.time.Duration

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.home),
            modifier = Modifier.padding(
                start = 24.dp,
                top = 24.dp,
                end = 24.dp,
                bottom = 8.dp,
            ),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.this_week),
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleLarge,
        )

        when (uiState) {
            HomeUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            HomeUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_goal_error),
                    textAlign = TextAlign.Center,
                )
            }

            is HomeUiState.Content -> HomeGoalSummary(
                state = uiState,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeGoalSummary(
    state: HomeUiState.Content,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.weekly_goal),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (state.targetMinutes == null) {
                        Text(text = stringResource(R.string.no_weekly_goal_configured))
                        Text(
                            text = stringResource(
                                R.string.practiced_this_week,
                                practicedDurationText(state.practicedDuration),
                            ),
                        )
                        Button(onClick = onOpenSettings) {
                            Text(text = stringResource(R.string.set_a_goal))
                        }
                    } else {
                        val progressText = weeklyProgressText(
                            state.practicedDuration,
                            state.targetMinutes,
                        )
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        LinearProgressIndicator(
                            progress = { state.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = progressText },
                        )
                        Text(
                            text = stringResource(
                                if (state.isReached) {
                                    R.string.weekly_goal_reached
                                } else {
                                    R.string.weekly_goal_in_progress
                                },
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun weeklyProgressText(duration: Duration, targetMinutes: Int): String = when {
    duration.isZero -> stringResource(R.string.weekly_progress_minutes, 0, targetMinutes)
    duration < Duration.ofMinutes(1) ->
        stringResource(R.string.weekly_progress_less_than_minute, targetMinutes)

    else -> stringResource(
        R.string.weekly_progress_minutes,
        duration.toMinutes(),
        targetMinutes,
    )
}

@Composable
private fun practicedDurationText(duration: Duration): String = when {
    duration.isZero -> stringResource(R.string.zero_minutes)
    duration < Duration.ofMinutes(1) -> stringResource(R.string.less_than_one_minute)
    else -> stringResource(R.string.duration_minutes, duration.toMinutes())
}
