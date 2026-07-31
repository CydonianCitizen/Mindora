package com.cydoniancitizen.mindora.feature.freemeditation

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        FreeMeditationHeader(
            backEnabled = uiState !is FreeMeditationUiState.Saving &&
                uiState !is FreeMeditationUiState.SaveFailed,
            onBack = onBack,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
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
            .height(56.dp)
            .padding(horizontal = 4.dp),
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
            text = stringResource(R.string.app_name),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
internal fun MeditationAmbientBackground(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val animationsDisabled = remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    val animatedScale = if (animationsDisabled) {
        1f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "ambient_breathing")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "ambient_scale",
        )
        scale
    }

    val primaryColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    val tertiaryColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)

    Canvas(
        modifier = modifier
            .clearAndSetSemantics {}
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            },
    ) {
        val centerPoint = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 1.8f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor, tertiaryColor, Color.Transparent),
                center = centerPoint,
                radius = radius,
            ),
            center = centerPoint,
            radius = radius,
        )
    }
}

@Composable
internal fun MeditationDurationDisplay(
    duration: Duration,
    modifier: Modifier = Modifier,
) {
    val countdown = formatCountdown(duration)
    val minutes = duration.toMinutes().toInt()
    val durationText = pluralStringResource(
        R.plurals.duration_minutes,
        minutes,
        duration.toMinutes(),
    )
    val accessibleDescription = stringResource(
        R.string.selected_duration_accessibility,
        durationText,
    )
    Text(
        text = countdown,
        modifier = modifier.semantics {
            contentDescription = accessibleDescription
        },
        style = MaterialTheme.typography.displayLarge.copy(
            fontFeatureSettings = "tnum",
            fontWeight = FontWeight.Bold,
        ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun DurationChip(
    duration: Duration,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minutes = duration.toMinutes().toInt()
    val labelText = pluralStringResource(
        R.plurals.duration_minutes,
        minutes,
        duration.toMinutes(),
    )
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupContent(
    state: FreeMeditationUiState.Setup,
    onSelectDuration: (Duration) -> Unit,
    onStart: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        MeditationAmbientBackground(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.Center),
        )

        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val linkedContent = state.linkedContent
            val titleText = linkedContent?.title ?: stringResource(R.string.free_meditation)

            Text(
                text = titleText,
                modifier = Modifier
                    .semantics { heading() }
                    .padding(bottom = 8.dp),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (linkedContent != null) {
                Text(
                    text = linkedContent.description,
                    modifier = Modifier.padding(bottom = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            MeditationDurationDisplay(
                duration = state.selectedDuration,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            Text(
                text = stringResource(R.string.duration),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            if (linkedContent == null) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
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
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            val durationMinutes = state.selectedDuration.toMinutes().toInt()
            val formattedDurationText = pluralStringResource(
                R.plurals.duration_minutes,
                durationMinutes,
                state.selectedDuration.toMinutes(),
            )
            val startAccessibilityText = stringResource(
                R.string.start_session_accessibility,
                formattedDurationText,
            )

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = startAccessibilityText },
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.start_session),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
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
            .verticalScroll(rememberScrollState())
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
            style = MaterialTheme.typography.displayLarge.copy(
                fontFeatureSettings = "tnum",
            ),
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
