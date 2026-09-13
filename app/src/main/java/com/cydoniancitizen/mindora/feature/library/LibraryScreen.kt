package com.cydoniancitizen.mindora.feature.library

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.content.model.MeditationTerm
import com.cydoniancitizen.mindora.ui.MindoraTopAppBar

@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onMeditationClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        uiState = uiState,
        onBack = onNavigateBack,
        onMeditationClick = onMeditationClick,
        onCategorySelected = viewModel::selectCategory,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    onBack: () -> Unit,
    onMeditationClick: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onRetry: () -> Unit,
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
                LibraryUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                LibraryUiState.Error -> LibraryError(onRetry = onRetry)

                is LibraryUiState.Content -> LibraryContent(
                    uiState = uiState,
                    onMeditationClick = onMeditationClick,
                    onCategorySelected = onCategorySelected,
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    uiState: LibraryUiState.Content,
    onMeditationClick: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
) {
    Column(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
        Text(
            text = stringResource(R.string.library_subtitle),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Kept out of the list, so the selector stays put while the meditations scroll under it.
        CategorySelector(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelected = onCategorySelected,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.meditations, key = MeditationCardUiModel::id) { meditation ->
                MeditationCard(
                    meditation = meditation,
                    onClick = { onMeditationClick(meditation.id) },
                )
            }
        }
    }
}

/** Tapping the selected category again goes back to the whole library. */
@Composable
private fun CategorySelector(
    categories: List<MeditationTerm>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(
            label = stringResource(R.string.library_filter_all),
            selected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) },
        )
        categories.forEach { category ->
            val isSelected = category.id == selectedCategoryId
            CategoryChip(
                label = category.label,
                selected = isSelected,
                onClick = { onCategorySelected(if (isSelected) null else category.id) },
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun MeditationCard(
    meditation: MeditationCardUiModel,
    onClick: () -> Unit,
) {
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
                text = meditation.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = meditation.description,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = meditationSummary(meditation),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The category is the selector above, so the card spends its line on the other three. */
@Composable
private fun meditationSummary(meditation: MeditationCardUiModel): String = listOf(
    meditation.goal,
    pluralStringResource(
        R.plurals.duration_minutes,
        meditation.durationMinutes,
        meditation.durationMinutes,
    ),
    stringResource(meditation.level.labelResId),
).joinToString(separator = " · ")

@Composable
private fun LibraryError(onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text(
            text = stringResource(R.string.library_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(text = stringResource(R.string.retry))
        }
    }
}
