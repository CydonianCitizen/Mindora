package com.cydoniancitizen.mindora.feature.history

import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import com.cydoniancitizen.mindora.ui.theme.tabularNumerals
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onNavigateToPractice: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onFilterSelected = viewModel::selectFilter,
        onStartPracticeClick = onNavigateToPractice,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    onFilterSelected: (HistoryFilter) -> Unit,
    onStartPracticeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mindfulness_reminder),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                ) {
                    HistoryHeaderSection()

                    when (uiState) {
                        HistoryUiState.Loading -> HistoryLoading()

                        HistoryUiState.Error -> HistoryErrorMessage()

                        HistoryUiState.Empty -> HistoryEmptyState(
                            onStartPracticeClick = onStartPracticeClick,
                        )

                        is HistoryUiState.Content -> {
                            HistoryFilterBar(
                                selectedFilter = uiState.selectedFilter,
                                availableFilters = uiState.availableFilters,
                                onFilterSelected = onFilterSelected,
                            )

                            if (uiState.monthGroups.isEmpty()) {
                                HistoryFilteredEmptyState(
                                    onClearFilterClick = { onFilterSelected(HistoryFilter.ALL) },
                                )
                            } else {
                                HistoryGroupedSessionList(
                                    monthGroups = uiState.monthGroups,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.history),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.history_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFilterBar(
    selectedFilter: HistoryFilter,
    availableFilters: List<HistoryFilter>,
    onFilterSelected: (HistoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        availableFilters.forEach { filter ->
            val isSelected = filter == selectedFilter
            val label = when (filter) {
                HistoryFilter.ALL -> stringResource(R.string.history_filter_all)
                HistoryFilter.GUIDED_MEDITATION -> stringResource(R.string.history_filter_guided)
                HistoryFilter.FREE_MEDITATION -> stringResource(R.string.history_filter_free)
                HistoryFilter.BREATHING_EXERCISE -> stringResource(R.string.history_filter_breathing)
            }
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun HistoryGroupedSessionList(
    monthGroups: List<HistoryMonthGroup>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 8.dp,
            bottom = 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(
            items = monthGroups,
            key = { _, group -> group.yearMonth.toString() },
        ) { _, monthGroup ->
            HistoryMonthGroupCard(monthGroup = monthGroup)
        }
    }
}

@Composable
private fun HistoryMonthGroupCard(
    monthGroup: HistoryMonthGroup,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val monthYearLabel = monthGroup.yearMonth.atDay(1)
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    val uppercaseMonthLabel = monthYearLabel.uppercase(locale)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = uppercaseMonthLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .semantics {
                    heading()
                    contentDescription = monthYearLabel
                },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                monthGroup.sessions.forEachIndexed { index, item ->
                    HistorySessionRow(item = item)
                    if (index < monthGroup.sessions.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySessionRow(
    item: HistorySessionDisplayItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val session = item.session
    val startedAtDate = Date.from(session.startedAt)

    val locale = LocalConfiguration.current.locales[0]
    val conciseDateFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
    val visibleDate = session.startedAt.atZone(ZoneId.systemDefault()).format(conciseDateFormatter)
    val visibleTime = DateFormat.getTimeFormat(context).format(startedAtDate)
    val fullDate = DateFormat.getMediumDateFormat(context).format(startedAtDate)
    val visibleDuration = formatDuration(session.activeDuration)
    val accessibilityDuration = formatDurationAccessibility(session.activeDuration, context)

    val (icon, containerColor, iconTint, typeNameRes) = when (session.type) {
        MindfulnessSessionType.GUIDED_MEDITATION -> Quadruple(
            Icons.AutoMirrored.Filled.List,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            R.string.session_type_guided_meditation,
        )

        MindfulnessSessionType.FREE_MEDITATION -> Quadruple(
            Icons.Default.PlayArrow,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            R.string.session_type_free_meditation,
        )

        MindfulnessSessionType.BREATHING_EXERCISE -> Quadruple(
            Icons.Default.Favorite,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            R.string.session_type_breathing_exercise,
        )
    }

    val typeName = stringResource(typeNameRes)
    val title = item.catalogueTitle ?: typeName
    val isInterrupted = session.status == MindfulnessSessionStatus.INTERRUPTED
    val statusText = if (isInterrupted) stringResource(R.string.session_status_interrupted) else null

    val isTitleIdenticalToType = title.equals(typeName, ignoreCase = true)
    val accessibilityDesc = when {
        isTitleIdenticalToType && isInterrupted -> stringResource(
            R.string.history_session_accessibility_interrupted,
            title,
            accessibilityDuration,
            fullDate,
            visibleTime,
        )

        isTitleIdenticalToType -> stringResource(
            R.string.history_session_accessibility,
            title,
            accessibilityDuration,
            fullDate,
            visibleTime,
        )

        isInterrupted -> stringResource(
            R.string.history_session_accessibility_with_type_interrupted,
            title,
            typeName,
            accessibilityDuration,
            fullDate,
            visibleTime,
        )

        else -> stringResource(
            R.string.history_session_accessibility_with_type,
            title,
            typeName,
            accessibilityDuration,
            fullDate,
            visibleTime,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDesc
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$typeName • $visibleDuration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isInterrupted && statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            // Equal-width figures so the dates and times stay in a column down the list instead of
            // going ragged wherever a 1 meets a 0.
            Text(
                text = visibleDate,
                style = MaterialTheme.typography.bodySmall.tabularNumerals(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = visibleTime,
                style = MaterialTheme.typography.labelSmall.tabularNumerals(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(
    onStartPracticeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accessibilityActionLabel = stringResource(R.string.history_start_practice_accessibility)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mindfulness_reminder),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.history_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onStartPracticeClick,
                modifier = Modifier.semantics {
                    contentDescription = accessibilityActionLabel
                },
            ) {
                Text(text = stringResource(R.string.history_start_practice))
            }
        }
    }
}

@Composable
private fun HistoryFilteredEmptyState(
    onClearFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.history_filtered_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_filtered_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(onClick = onClearFilterClick) {
                Text(text = stringResource(R.string.history_clear_filter))
            }
        }
    }
}

@Composable
private fun HistoryLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HistoryErrorMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.history_error_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

private fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

private fun formatDurationAccessibility(duration: Duration, context: android.content.Context): String {
    val totalSeconds = duration.seconds
    val minutes = (totalSeconds / 60).toInt()
    return if (minutes > 0) {
        context.resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
    } else {
        context.getString(R.string.less_than_one_minute)
    }
}

