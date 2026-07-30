package com.cydoniancitizen.mindora.feature.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.preferences.model.supportedWeeklyGoalMinutes
import com.cydoniancitizen.mindora.core.reminder.notificationsAllowed
import java.time.LocalTime
import java.util.Calendar

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var systemNotificationsAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        systemNotificationsAllowed = notificationsAllowed(context)
        if (granted && systemNotificationsAllowed) {
            permissionDenied = false
            viewModel.enableDailyReminder()
        } else {
            permissionDenied = true
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemNotificationsAllowed = notificationsAllowed(context)
                if (systemNotificationsAllowed) permissionDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsScreen(
        uiState = uiState,
        notificationsAllowed = systemNotificationsAllowed,
        permissionDenied = permissionDenied,
        onWeeklyGoalSelected = viewModel::setWeeklyGoal,
        onReminderToggle = { enabled ->
            if (!enabled) {
                viewModel.disableDailyReminder()
            } else if (systemNotificationsAllowed) {
                permissionDenied = false
                viewModel.enableDailyReminder()
            } else if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                permissionDenied = true
            }
        },
        onReminderTimeSelected = viewModel::setDailyReminderTime,
        onOpenNotificationSettings = { openNotificationSettings(context) },
        onDismissError = viewModel::clearOperationError,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    notificationsAllowed: Boolean,
    permissionDenied: Boolean,
    onWeeklyGoalSelected: (Int?) -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onReminderTimeSelected: (LocalTime) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.settings),
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        when (uiState) {
            SettingsUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            SettingsUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.settings_load_error))
            }

            is SettingsUiState.Content -> SettingsContent(
                state = uiState,
                notificationsAllowed = notificationsAllowed,
                permissionDenied = permissionDenied,
                onWeeklyGoalSelected = onWeeklyGoalSelected,
                onReminderToggle = onReminderToggle,
                onReminderTimeSelected = onReminderTimeSelected,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onDismissError = onDismissError,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Content,
    notificationsAllowed: Boolean,
    permissionDenied: Boolean,
    onWeeklyGoalSelected: (Int?) -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onReminderTimeSelected: (LocalTime) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = state.preferences
    val showNotificationBlock = permissionDenied ||
        (preferences.dailyReminderEnabled && !notificationsAllowed)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.weekly_goal),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            Column(modifier = Modifier.semantics { selectableGroup() }) {
                GoalChoice(
                    minutes = null,
                    selected = preferences.weeklyGoalMinutes == null,
                    onSelected = onWeeklyGoalSelected,
                )
                supportedWeeklyGoalMinutes.forEach { minutes ->
                    GoalChoice(
                        minutes = minutes,
                        selected = preferences.weeklyGoalMinutes == minutes,
                        onSelected = onWeeklyGoalSelected,
                    )
                }
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        item {
            Text(
                text = stringResource(R.string.daily_reminder),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            val toggleState = stringResource(
                if (preferences.dailyReminderEnabled) R.string.enabled else R.string.disabled,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.enable_daily_reminder),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(text = stringResource(R.string.reminder_delivery_approximation))
                }
                Switch(
                    checked = preferences.dailyReminderEnabled,
                    onCheckedChange = onReminderToggle,
                    modifier = Modifier.semantics { stateDescription = toggleState },
                )
            }
        }
        item {
            ReminderTimeRow(
                time = preferences.dailyReminderTime,
                onTimeSelected = onReminderTimeSelected,
            )
        }
        if (showNotificationBlock) {
            item {
                Text(
                    text = stringResource(
                        if (preferences.dailyReminderEnabled) {
                            R.string.notifications_blocked
                        } else {
                            R.string.notification_permission_denied
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onOpenNotificationSettings) {
                    Text(text = stringResource(R.string.open_notification_settings))
                }
            }
        }
        if (state.hasOperationError) {
            item {
                Text(text = stringResource(R.string.settings_save_error))
                TextButton(onClick = onDismissError) {
                    Text(text = stringResource(R.string.dismiss))
                }
            }
        }
    }
}

@Composable
private fun GoalChoice(
    minutes: Int?,
    selected: Boolean,
    onSelected: (Int?) -> Unit,
) {
    val label = if (minutes == null) {
        stringResource(R.string.off)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = { onSelected(minutes) },
                role = Role.RadioButton,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun ReminderTimeRow(
    time: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val calendar = remember(time) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
        }
    }
    val displayTime = DateFormat.getTimeFormat(context).format(calendar.time)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.reminder_time),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = displayTime)
        }
        Button(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
                    time.hour,
                    time.minute,
                    DateFormat.is24HourFormat(context),
                ).show()
            },
        ) {
            Text(text = stringResource(R.string.change_reminder_time))
        }
    }
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}
