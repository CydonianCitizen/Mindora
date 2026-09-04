package com.cydoniancitizen.mindora.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cydoniancitizen.mindora.R

/**
 * The container the full-screen states of the three session screens share: scrollable so nothing
 * is lost at large font scales, centred, inset by 24 dp.
 */
@Composable
internal fun SessionStateColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

/** Spinner and a line of text: loading a linked step, preparing audio, saving a session. */
@Composable
internal fun SessionProgress(message: String, modifier: Modifier = Modifier) {
    SessionStateColumn(modifier) {
        CircularProgressIndicator()
        Text(
            text = message,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** A label and click handler for one of [SessionMessage]'s buttons. */
@Immutable
internal data class SessionAction(val label: String, val onClick: () -> Unit)

/**
 * A state the session cannot continue from: a heading, optional supporting text, and up to two
 * full-width actions. The heading is announced as one, so a screen reader lands on it when the
 * state appears.
 */
@Composable
internal fun SessionMessage(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    primaryAction: SessionAction? = null,
    secondaryAction: SessionAction? = null,
) {
    SessionStateColumn(modifier) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (primaryAction != null) {
            Button(
                onClick = primaryAction.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text(primaryAction.label)
            }
        }
        if (secondaryAction != null) {
            OutlinedButton(
                onClick = secondaryAction.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (primaryAction != null) 12.dp else 24.dp),
            ) {
                Text(secondaryAction.label)
            }
        }
    }
}

/** Waiting for the catalogue entry a path step points at. */
@Composable
internal fun LinkedStepLoading() {
    SessionProgress(stringResource(R.string.loading_practice_step))
}

/** The path step could not be resolved, so the only way on is back to the path. */
@Composable
internal fun LinkedStepUnavailable(onBack: () -> Unit) {
    SessionMessage(
        title = stringResource(R.string.practice_step_unavailable),
        secondaryAction = SessionAction(stringResource(R.string.back_to_path), onBack),
    )
}

/** Writing the finished session to the database. */
@Composable
internal fun SessionSaving() {
    SessionProgress(stringResource(R.string.saving_session))
}

/** The session finished but could not be stored; retrying and discarding are the only ways out. */
@Composable
internal fun SessionSaveFailed(onRetrySave: () -> Unit, onDiscard: () -> Unit) {
    SessionMessage(
        title = stringResource(R.string.session_save_failed),
        supportingText = stringResource(R.string.session_discard_explanation),
        primaryAction = SessionAction(stringResource(R.string.retry), onRetrySave),
        secondaryAction = SessionAction(stringResource(R.string.discard), onDiscard),
    )
}
