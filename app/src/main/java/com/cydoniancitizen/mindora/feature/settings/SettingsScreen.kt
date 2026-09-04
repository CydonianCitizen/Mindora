package com.cydoniancitizen.mindora.feature.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cydoniancitizen.mindora.BuildConfig
import com.cydoniancitizen.mindora.R
import com.cydoniancitizen.mindora.core.export.defaultExportFileName
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
    var language by remember { mutableStateOf(currentAppLanguage(context)) }
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
                // Both can be changed from system settings while the app is in the background.
                language = currentAppLanguage(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE),
    ) { destination -> destination?.let(viewModel::exportData) }

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
        language = language,
        onLanguageSelected = { chosen ->
            language = chosen
            setAppLanguage(context, chosen)
        },
        onExportClick = { exportLauncher.launch(defaultExportFileName()) },
        onDismissExportStatus = viewModel::clearExportStatus,
    )
}

private const val EXPORT_MIME_TYPE = "application/json"

@OptIn(ExperimentalMaterial3Api::class)
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
    language: AppLanguage = AppLanguage.SYSTEM,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    onExportClick: () -> Unit = {},
    onDismissExportStatus: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
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
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            SettingsBody(
                uiState = uiState,
                notificationsAllowed = notificationsAllowed,
                permissionDenied = permissionDenied,
                onWeeklyGoalSelected = onWeeklyGoalSelected,
                onReminderToggle = onReminderToggle,
                onReminderTimeSelected = onReminderTimeSelected,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onDismissError = onDismissError,
                language = language,
                onLanguageSelected = onLanguageSelected,
                onExportClick = onExportClick,
                onDismissExportStatus = onDismissExportStatus,
            )
        }
    }
}

@Composable
private fun SettingsBody(
    uiState: SettingsUiState,
    notificationsAllowed: Boolean,
    permissionDenied: Boolean,
    onWeeklyGoalSelected: (Int?) -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onReminderTimeSelected: (LocalTime) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onDismissError: () -> Unit,
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onExportClick: () -> Unit,
    onDismissExportStatus: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.settings),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (uiState) {
            SettingsUiState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            SettingsUiState.Error -> item {
                SettingsMessage(text = stringResource(R.string.settings_load_error))
            }
            is SettingsUiState.Content -> {
                item {
                    SettingsSection(
                        title = stringResource(R.string.settings_practice_goals_section),
                        icon = Icons.Filled.Favorite,
                    ) {
                        WeeklyGoalSetting(
                            weeklyGoalMinutes = uiState.preferences.weeklyGoalMinutes,
                            onWeeklyGoalSelected = onWeeklyGoalSelected,
                        )
                    }
                }
                item {
                    val showNotificationBlock = permissionDenied ||
                        (uiState.preferences.dailyReminderEnabled && !notificationsAllowed)
                    SettingsSection(
                        title = stringResource(R.string.settings_reminders_section),
                        icon = Icons.Filled.Notifications,
                    ) {
                        DailyReminderSetting(
                            enabled = uiState.preferences.dailyReminderEnabled,
                            time = uiState.preferences.dailyReminderTime,
                            onToggle = onReminderToggle,
                            onTimeSelected = onReminderTimeSelected,
                        )
                        if (showNotificationBlock) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            NotificationPermissionMessage(
                                reminderEnabled = uiState.preferences.dailyReminderEnabled,
                                onOpenNotificationSettings = onOpenNotificationSettings,
                            )
                        }
                    }
                }
                if (uiState.hasOperationError) {
                    item {
                        SettingsMessage(
                            text = stringResource(R.string.settings_save_error),
                            actionLabel = stringResource(R.string.dismiss),
                            onAction = onDismissError,
                        )
                    }
                }
                item {
                    SettingsSection(
                        title = stringResource(R.string.settings_language_section),
                        icon = ImageVector.vectorResource(R.drawable.ic_language),
                    ) {
                        LanguageSetting(
                            selected = language,
                            onSelect = onLanguageSelected,
                        )
                    }
                }
                item {
                    SettingsSection(
                        title = stringResource(R.string.settings_data_section),
                        icon = ImageVector.vectorResource(R.drawable.ic_export),
                    ) {
                        DataExportSetting(
                            status = uiState.exportStatus,
                            onExportClick = onExportClick,
                            onDismissStatus = onDismissExportStatus,
                        )
                    }
                }
                item {
                    SettingsSection(
                        title = stringResource(R.string.settings_about_section),
                        icon = Icons.Filled.Info,
                    ) {
                        AboutSetting()
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            content()
        }
    }
}

@Composable
internal fun WeeklyGoalSetting(
    weeklyGoalMinutes: Int?,
    onWeeklyGoalSelected: (Int?) -> Unit,
) {
    val currentIndex = weeklyGoalMinutes?.let(supportedWeeklyGoalMinutes::indexOf) ?: -1
    val canDecrease = currentIndex > 0
    val canIncrease = weeklyGoalMinutes == null ||
        currentIndex in 0 until supportedWeeklyGoalMinutes.lastIndex
    val decreaseTarget = supportedWeeklyGoalMinutes.getOrNull(currentIndex - 1)
    val increaseTarget = if (weeklyGoalMinutes == null) {
        supportedWeeklyGoalMinutes.first()
    } else {
        supportedWeeklyGoalMinutes.getOrNull(currentIndex + 1)
    }
    val currentValueLabel = weeklyGoalMinutes?.let { minutes ->
        pluralStringResource(R.plurals.duration_minutes, minutes, minutes)
    } ?: stringResource(R.string.off)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WeeklyGoalDescription()
                WeeklyGoalStepper(
                    currentValueLabel = currentValueLabel,
                    canDecrease = canDecrease,
                    canIncrease = canIncrease,
                    onDecrease = { decreaseTarget?.let(onWeeklyGoalSelected) },
                    onIncrease = { increaseTarget?.let(onWeeklyGoalSelected) },
                    modifier = Modifier.align(Alignment.End),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WeeklyGoalDescription(modifier = Modifier.weight(1f))
                WeeklyGoalStepper(
                    currentValueLabel = currentValueLabel,
                    canDecrease = canDecrease,
                    canIncrease = canIncrease,
                    onDecrease = { decreaseTarget?.let(onWeeklyGoalSelected) },
                    onIncrease = { increaseTarget?.let(onWeeklyGoalSelected) },
                )
            }
        }
    }
    if (weeklyGoalMinutes != null) {
        TextButton(
            onClick = { onWeeklyGoalSelected(null) },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(text = stringResource(R.string.turn_off_weekly_goal))
        }
    }
}

@Composable
private fun WeeklyGoalDescription(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.weekly_goal),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.weekly_goal_supporting_text),
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeeklyGoalStepper(
    currentValueLabel: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val decreaseDescription = stringResource(
        R.string.decrease_weekly_goal_accessibility,
        currentValueLabel,
    )
    val increaseDescription = stringResource(
        R.string.increase_weekly_goal_accessibility,
        currentValueLabel,
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalIconButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.size(48.dp).semantics {
                contentDescription = decreaseDescription
                if (!canDecrease) disabled()
            },
        ) {
            Text(
                text = "−",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Column(
            modifier = Modifier.widthIn(min = 76.dp).semantics(mergeDescendants = true) {
                contentDescription = currentValueLabel
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = currentValueLabel,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.per_week),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalIconButton(
            onClick = onIncrease,
            enabled = canIncrease,
            modifier = Modifier.size(48.dp).semantics {
                contentDescription = increaseDescription
                if (!canIncrease) disabled()
            },
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
}

@Composable
internal fun DailyReminderSetting(
    enabled: Boolean,
    time: LocalTime,
    onToggle: (Boolean) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
) {
    val reminderLabel = stringResource(R.string.daily_reminder)
    val reminderState = stringResource(if (enabled) R.string.enabled else R.string.disabled)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminderLabel,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.reminder_delivery_approximation),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics {
                contentDescription = reminderLabel
                stateDescription = reminderState
            },
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    ReminderTimeRow(
        time = time,
        enabled = enabled,
        onTimeSelected = onTimeSelected,
    )
}

@Composable
private fun ReminderTimeRow(
    time: LocalTime,
    enabled: Boolean,
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
    val timeDescription = stringResource(R.string.reminder_time_accessibility, displayTime)
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onTimeSelected(LocalTime.of(hour, minute)) },
                        time.hour,
                        time.minute,
                        DateFormat.is24HourFormat(context),
                    ).show()
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = timeDescription
                if (!enabled) disabled()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.reminder_time),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            Text(
                text = displayTime,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        }
        Text(
            text = stringResource(R.string.change_reminder_time),
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun NotificationPermissionMessage(
    reminderEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(
                if (reminderEnabled) R.string.notifications_blocked
                else R.string.notification_permission_denied,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(
            onClick = onOpenNotificationSettings,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(text = stringResource(R.string.open_notification_settings))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageSetting(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Read-only and not editable: the field shows the choice and opens the list, it is not a
        // place to type. No label either — the section heading directly above already says
        // Language, and repeating it inside the field would announce it twice.
        TextField(
            value = stringResource(selected.labelResId),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(language.labelResId)) },
                    onClick = {
                        expanded = false
                        onSelect(language)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
internal fun DataExportSetting(
    status: DataExportStatus,
    onExportClick: () -> Unit,
    onDismissStatus: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.export_data_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onExportClick,
            enabled = status != DataExportStatus.RUNNING,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(text = stringResource(R.string.export_data))
        }

        val message = when (status) {
            DataExportStatus.IDLE -> null
            DataExportStatus.RUNNING -> stringResource(R.string.export_data_running)
            DataExportStatus.DONE -> stringResource(R.string.export_data_done)
            DataExportStatus.FAILED -> stringResource(R.string.export_data_failed)
        }
        if (message != null) {
            Text(
                text = message,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (status == DataExportStatus.FAILED) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (status != DataExportStatus.RUNNING) {
                TextButton(onClick = onDismissStatus) {
                    Text(text = stringResource(R.string.dismiss))
                }
            }
        }
    }
}

@Composable
internal fun AboutSetting() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.privacy_first),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.privacy_first_description),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    Text(
        text = stringResource(
            R.string.app_version_value,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsMessage(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}
