package de.ingomohrmann.ezmedicator.ui.screens.medications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.ingomohrmann.ezmedicator.R
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import de.ingomohrmann.ezmedicator.data.repository.formatDelayMinutes
import de.ingomohrmann.ezmedicator.domain.CronHelper
import de.ingomohrmann.ezmedicator.ui.components.MedicationImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    medicationId: Long,
    onBack: () -> Unit,
    onEditMedication: () -> Unit,
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    viewModel: MedicationDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(medicationId) { viewModel.load(medicationId) }

    val medication by viewModel.medication.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val delaySteps by viewModel.delaySteps.collectAsState()
    var deleteTarget by remember { mutableStateOf<Reminder?>(null) }
    var delayTarget by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(medication?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEditMedication) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.edit_medication))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddReminder) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_reminder))
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Medication header
            medication?.let { med ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            MedicationImage(imagePath = med.imagePath, size = 72.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(med.title, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }

            // Section header
            item {
                Text(
                    stringResource(R.string.reminders),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (reminders.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_reminders),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        nextLabel = viewModel.nextTriggerLabel(reminder),
                        onEdit = { onEditReminder(reminder.id) },
                        onToggleEnabled = { viewModel.toggleEnabled(reminder) },
                        onToggleSkip = { viewModel.toggleSkipNext(reminder) },
                        onDelay = { delayTarget = reminder },
                        onReset = { viewModel.resetNext(reminder) },
                        onDelete = { deleteTarget = reminder },
                    )
                }
            }
        }
    }

    // Delete confirmation
    deleteTarget?.let { rem ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_reminder)) },
            text = { Text(stringResource(R.string.delete_reminder_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(rem)
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Delay picker
    delayTarget?.let { rem ->
        DelayDialog(
            presets = delaySteps,
            onDismiss = { delayTarget = null },
            onConfirm = { minutes ->
                viewModel.delayNext(rem, minutes)
                delayTarget = null
            },
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    nextLabel: String,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit,
    onToggleSkip: () -> Unit,
    onDelay: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.cronExpression,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    )
                    Text(
                        text = CronHelper.describe(reminder.cronExpression),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val isDelayed = reminder.delayedByMinutes != null &&
                        reminder.snoozedUntil != null &&
                        reminder.snoozedUntil > System.currentTimeMillis()
                    Text(
                        text = if (isDelayed)
                            stringResource(R.string.delayed_by, formatDelayMinutes(reminder.delayedByMinutes!!))
                        else
                            stringResource(R.string.next_trigger, nextLabel),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDelayed)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary,
                    )
                    if (isDelayed) {
                        Text(
                            text = stringResource(R.string.next_trigger, nextLabel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(checked = reminder.isEnabled, onCheckedChange = { onToggleEnabled() })
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, null)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            val isModified = reminder.skipNextOccurrence ||
                (reminder.snoozedUntil != null && reminder.snoozedUntil > System.currentTimeMillis())

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilterChip(
                    selected = reminder.skipNextOccurrence,
                    onClick = onToggleSkip,
                    label = { Text(stringResource(R.string.skip_next), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = onDelay,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.delay_next), style = MaterialTheme.typography.labelSmall)
                }
                if (isModified) {
                    IconButton(onClick = onReset) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.reset_next),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun DelayDialog(
    presets: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var customMinutes by remember { mutableStateOf("") }
    var showCustom by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delay_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                presets.forEach { minutes ->
                    TextButton(
                        onClick = { onConfirm(minutes) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(formatDelayMinutes(minutes)) }
                }
                TextButton(
                    onClick = { showCustom = !showCustom },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.delay_custom)) }
                if (showCustom) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.delay_minutes_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (showCustom) {
                TextButton(
                    onClick = {
                        customMinutes.toIntOrNull()?.takeIf { it > 0 }?.let { onConfirm(it) }
                    },
                    enabled = customMinutes.toIntOrNull()?.let { it > 0 } == true,
                ) { Text(stringResource(R.string.delay_confirm)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
