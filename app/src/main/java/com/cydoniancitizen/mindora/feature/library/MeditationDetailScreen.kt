package com.cydoniancitizen.mindora.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.ui.MindoraTopAppBar

@Composable
fun MeditationDetailScreen(
    onNavigateBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    viewModel: MeditationDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MeditationDetailScreen(
        uiState = uiState,
        onBack = onNavigateBack,
        onStartPractice = onStartPractice,
    )
}

@Composable
internal fun MeditationDetailScreen(
    uiState: MeditationDetailUiState,
    onBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MindoraTopAppBar(
            title = stringResource(R.string.library),
            onBack = onBack,
        )
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (uiState) {
                MeditationDetailUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                MeditationDetailUiState.Unavailable -> DetailMessage(
                    text = stringResource(R.string.meditation_unavailable),
                )

                MeditationDetailUiState.Error -> DetailMessage(
                    text = stringResource(R.string.library_error),
                )

                is MeditationDetailUiState.Content -> MeditationDetailContent(
                    content = uiState,
                    onStartPractice = { onStartPractice(uiState.id) },
                )
            }
        }
    }
}

@Composable
private fun MeditationDetailContent(
    content: MeditationDetailUiState.Content,
    onStartPractice: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    text = content.title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = content.description,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DetailRow(
                    label = stringResource(R.string.meditation_technique),
                    value = content.technique,
                )
                DetailRow(
                    label = stringResource(R.string.meditation_category),
                    value = content.category,
                )
                DetailRow(
                    label = stringResource(R.string.meditation_goal),
                    value = content.goal,
                )
                DetailRow(
                    label = stringResource(R.string.meditation_duration),
                    value = pluralStringResource(
                        R.plurals.duration_minutes,
                        content.durationMinutes,
                        content.durationMinutes,
                    ),
                )
                DetailRow(
                    label = stringResource(R.string.meditation_level),
                    value = stringResource(content.level.labelResId),
                )
            }
        }
        item {
            Button(
                onClick = onStartPractice,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(text = stringResource(R.string.meditation_start))
            }
        }
        if (content.safetyNotes != null) {
            item {
                SafetyNote(text = content.safetyNotes)
            }
        }
        item {
            Text(
                text = stringResource(R.string.meditation_how_to_practise),
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        itemsIndexed(content.steps) { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (content.tags.isNotEmpty()) {
            item {
                DetailRow(
                    label = stringResource(R.string.meditation_tags),
                    value = content.tags.joinToString(separator = " · "),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Shown on the practices that ask more of the user. It reads as an ordinary note, not a warning:
 * the point is to say the practice can be left at any moment, not to make it sound risky.
 */
@Composable
private fun SafetyNote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.meditation_safety_note),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = text,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
