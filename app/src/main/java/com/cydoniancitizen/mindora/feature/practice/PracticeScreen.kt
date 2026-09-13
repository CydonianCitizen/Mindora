package com.cydoniancitizen.mindora.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.feature.breathing.ProductionBreathingExerciseConfig
import com.cydoniancitizen.mindora.navigation.ContainerKeys
import com.cydoniancitizen.mindora.navigation.sharedContainer
import com.cydoniancitizen.mindora.ui.MindoraBrandTopAppBar
import java.time.Duration
import java.time.LocalTime

@Composable
fun PracticeScreen(
    onFreeMeditationClick: () -> Unit,
    onWhiteNoiseClick: () -> Unit,
    onBreathingExerciseClick: () -> Unit,
    onPathClick: (String) -> Unit,
    onLibraryClick: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshWeeklyGoal()
    }
    LaunchedEffect(viewModel) {
        while (true) {
            viewModel.refreshAtNextWeekBoundary()
        }
    }
    PracticeScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onFreeMeditationClick = onFreeMeditationClick,
        onWhiteNoiseClick = onWhiteNoiseClick,
        onBreathingExerciseClick = onBreathingExerciseClick,
        onPathClick = onPathClick,
        onLibraryClick = onLibraryClick,
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
    onWhiteNoiseClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isWideScreen = screenWidthDp >= 600
    val breathingDurationSeconds = ProductionBreathingExerciseConfig.plannedDuration.seconds.toInt()
    val breathingDurationLabel = pluralStringResource(
        R.plurals.breathing_approximate_duration,
        breathingDurationSeconds,
        breathingDurationSeconds,
    )
    val showPathsSection = when (uiState) {
        is PracticeUiState.Empty -> false
        is PracticeUiState.Content -> uiState.paths.isNotEmpty()
        else -> true
    }

    Column(modifier = modifier.fillMaxSize()) {
        MindoraBrandTopAppBar()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                // Width cap first: fillMaxSize hands down exact constraints, so a widthIn placed
                // after it is ignored and the content stretches across the whole window.
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    val greetingRes = rememberGreetingStringRes()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(greetingRes),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.practice),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .semantics { heading() },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item {
                    val freeMeditationCard: @Composable (Modifier) -> Unit = { cardModifier ->
                        PracticeActionCard(
                            title = stringResource(R.string.free_meditation),
                            description = stringResource(R.string.free_meditation_description),
                            icon = Icons.Filled.PlayArrow,
                            onClick = onFreeMeditationClick,
                            modifier = cardModifier.sharedContainer(ContainerKeys.FREE_MEDITATION),
                        )
                    }
                    val breathingCard: @Composable (Modifier) -> Unit = { cardModifier ->
                        PracticeActionCard(
                            title = stringResource(R.string.breathing_exercise),
                            description = stringResource(R.string.breathing_exercise_description),
                            chipLabel = breathingDurationLabel,
                            icon = Icons.Filled.Favorite,
                            onClick = onBreathingExerciseClick,
                            modifier = cardModifier.sharedContainer(ContainerKeys.BREATHING_EXERCISE),
                        )
                    }
                    if (isWideScreen) {
                        // Side by side at the taller one's height.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            freeMeditationCard(Modifier.weight(1f).fillMaxHeight())
                            breathingCard(Modifier.weight(1f).fillMaxHeight())
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            freeMeditationCard(Modifier)
                            breathingCard(Modifier)
                        }
                    }
                }

                item {
                    PracticeActionCard(
                        title = stringResource(R.string.white_noise),
                        description = stringResource(R.string.white_noise_description),
                        icon = Icons.Filled.PlayArrow,
                        onClick = onWhiteNoiseClick,
                        modifier = Modifier.sharedContainer(ContainerKeys.WHITE_NOISE),
                    )
                }

                // Full width in both layouts: the library opens a screen of its own rather
                // than starting a session, so it does not sit in the pair above.
                item {
                    PracticeActionCard(
                        title = stringResource(R.string.library),
                        description = stringResource(R.string.library_description),
                        icon = ImageVector.vectorResource(R.drawable.ic_library),
                        onClick = onLibraryClick,
                        modifier = Modifier.sharedContainer(ContainerKeys.LIBRARY),
                    )
                }

                item {
                    WeeklyGoalCard(weeklyGoal = uiState.weeklyGoal)
                }

                if (showPathsSection) {
                    item {
                        Text(
                            text = stringResource(R.string.bundled_mindfulness_paths),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .semantics { heading() },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    when (uiState) {
                        PracticeUiState.Loading -> item { PracticeLoading() }
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
                        is PracticeUiState.Empty -> Unit
                    }
                }
            }
        }
    }

}

@Composable
fun PracticeActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chipLabel: String? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // The chip sits under the text rather than beside the title: alongside it, a
                // longer translation or a larger font scale wraps it into a two-line lump.
                if (chipLabel != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = chipLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircularGoalProgress(
    progressFraction: Float,
    centerText: String?,
    modifier: Modifier = Modifier,
) {
    // Material's own track, cap and gap: the component already draws the whole ring.
    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier.fillMaxSize(),
        )
        if (centerText != null) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun WeeklyGoalCard(
    weeklyGoal: WeeklyGoalUiModel,
    modifier: Modifier = Modifier,
) {
    val progressDescription = if (weeklyGoal.targetMinutes != null) {
        weeklyProgressText(weeklyGoal.practicedDuration, weeklyGoal.targetMinutes)
    } else {
        stringResource(
            R.string.practiced_this_week,
            practicedDurationText(weeklyGoal.practicedDuration),
        )
    }
    val goalRatio = weeklyGoal.targetMinutes?.let { targetMinutes ->
        stringResource(
            R.string.weekly_goal_ratio,
            weeklyGoal.practicedDuration.toMinutes(),
            targetMinutes,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = progressDescription
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularGoalProgress(
                progressFraction = weeklyGoal.progressFraction,
                centerText = goalRatio,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.weekly_goal),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )

                if (weeklyGoal.targetMinutes == null) {
                    Text(
                        text = stringResource(R.string.no_weekly_goal_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = progressDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = progressDescription,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            if (weeklyGoal.isReached) {
                                R.string.weekly_goal_reached
                            } else {
                                R.string.weekly_goal_in_progress
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (weeklyGoal.isReached) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberGreetingStringRes(
    time: LocalTime = LocalTime.now(),
): Int = when (time.hour) {
    in 5..11 -> R.string.greeting_morning
    in 12..16 -> R.string.greeting_afternoon
    else -> R.string.greeting_evening
}

@Composable
internal fun weeklyProgressText(duration: Duration, targetMinutes: Int): String = when {
    duration.isZero -> pluralStringResource(
        R.plurals.weekly_progress_minutes,
        targetMinutes,
        0,
        targetMinutes,
    )
    duration < Duration.ofMinutes(1) -> pluralStringResource(
        R.plurals.weekly_progress_less_than_minute,
        targetMinutes,
        targetMinutes,
    )
    else -> pluralStringResource(
        R.plurals.weekly_progress_minutes,
        targetMinutes,
        duration.toMinutes().toInt(),
        targetMinutes,
    )
}

@Composable
internal fun practicedDurationText(duration: Duration): String = when {
    duration.isZero -> stringResource(R.string.zero_minutes)
    duration < Duration.ofMinutes(1) -> stringResource(R.string.less_than_one_minute)
    else -> pluralStringResource(
        R.plurals.duration_minutes,
        duration.toMinutes().toInt(),
        duration.toMinutes(),
    )
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = path.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = path.description,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = progressDescription,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
