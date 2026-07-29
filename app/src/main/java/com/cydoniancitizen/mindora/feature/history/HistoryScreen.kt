package com.cydoniancitizen.mindora.feature.history

import android.text.format.DateFormat
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionStatus
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSessionType
import java.time.Duration
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(uiState = uiState)
}

@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.history),
            modifier = Modifier.padding(
                start = 24.dp,
                top = 24.dp,
                end = 24.dp,
                bottom = 16.dp,
            ),
            style = MaterialTheme.typography.headlineMedium,
        )

        when (uiState) {
            HistoryUiState.Loading -> HistoryLoading()
            HistoryUiState.Empty -> HistoryMessage(
                message = stringResource(R.string.history_empty_message),
            )

            HistoryUiState.Error -> HistoryMessage(
                message = stringResource(R.string.history_error_message),
            )

            is HistoryUiState.Content -> HistorySessionList(uiState.sessions)
        }
    }
}

@Composable
private fun ColumnScope.HistoryLoading() {
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
private fun ColumnScope.HistoryMessage(message: String) {
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
private fun ColumnScope.HistorySessionList(sessions: List<MindfulnessSession>) {
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
            items = sessions,
            key = MindfulnessSession::id,
        ) { session ->
            HistorySessionCard(session)
        }
    }
}

@Composable
private fun HistorySessionCard(session: MindfulnessSession) {
    val context = LocalContext.current
    val startedAt = Date.from(session.startedAt)
    val type = when (session.type) {
        MindfulnessSessionType.GUIDED_MEDITATION ->
            stringResource(R.string.session_type_guided_meditation)

        MindfulnessSessionType.FREE_MEDITATION ->
            stringResource(R.string.session_type_free_meditation)

        MindfulnessSessionType.BREATHING_EXERCISE ->
            stringResource(R.string.session_type_breathing_exercise)
    }
    val status = when (session.status) {
        MindfulnessSessionStatus.COMPLETED -> stringResource(R.string.session_status_completed)
        MindfulnessSessionStatus.INTERRUPTED -> stringResource(R.string.session_status_interrupted)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = type,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.history_session_started_at,
                    DateFormat.getMediumDateFormat(context).format(startedAt),
                    DateFormat.getTimeFormat(context).format(startedAt),
                ),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.history_session_active_duration,
                    formatDuration(session.activeDuration),
                ),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = status,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val totalSeconds = duration.seconds
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
