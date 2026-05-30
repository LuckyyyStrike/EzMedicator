package de.ingomohrmann.ezmedicator.ui.screens.medications

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.ingomohrmann.ezmedicator.R
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import de.ingomohrmann.ezmedicator.data.repository.formatDelayMinutes
import de.ingomohrmann.ezmedicator.ui.components.MedicationImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onAddReminder: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: MedicationListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val delaySteps by viewModel.delaySteps.collectAsState()
    var deleteTarget by remember { mutableStateOf<Medication?>(null) }
    var delayTarget by remember { mutableStateOf<Pair<MedicationListItem, Reminder>?>(null) }

    val context = LocalContext.current
    val todayLabel = remember {
        val date = Date()
        val dow = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        val datePart = DateFormat.getDateFormat(context).format(date)
        "$dow $datePart"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.medications))
                        Text(
                            text = todayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_medication))
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text(
                    text = stringResource(R.string.no_medications),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.medication.id }) { item ->
                    MedicationCard(
                        item = item,
                        onEdit = { onEdit(item.medication.id) },
                        onDelete = { deleteTarget = item.medication },
                        onManageReminders = { onOpen(item.medication.id) },
                        onAddReminder = { onAddReminder(item.medication.id) },
                        onToggleSkip = { reminder ->
                            viewModel.toggleSkipNext(reminder, item.medication.title)
                        },
                        onDelay = { reminder -> delayTarget = item to reminder },
                        onReset = { reminder ->
                            viewModel.resetNext(reminder, item.medication.title)
                        },
                        nextTriggerLabel = { reminder -> viewModel.nextTriggerLabel(reminder) },
                    )
                }
            }
        }
    }

    deleteTarget?.let { med ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_medication)) },
            text = { Text(stringResource(R.string.delete_medication_confirm, med.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(med)
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

    delayTarget?.let { (item, reminder) ->
        DelayDialog(
            presets = delaySteps,
            onDismiss = { delayTarget = null },
            onConfirm = { minutes ->
                viewModel.delayNext(reminder, item.medication.title, minutes)
                delayTarget = null
            },
        )
    }
}

@Composable
private fun MedicationCard(
    item: MedicationListItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageReminders: () -> Unit,
    onAddReminder: () -> Unit,
    onToggleSkip: (Reminder) -> Unit,
    onDelay: (Reminder) -> Unit,
    onReset: (Reminder) -> Unit,
    nextTriggerLabel: (Reminder) -> String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // ── Medication header ──────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 8.dp),
            ) {
                MedicationImage(
                    imagePath = item.medication.imagePath,
                    iconName = item.medication.iconName,
                    iconColor = item.medication.iconColor,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.medication.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_medication))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_medication),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            HorizontalDivider()

            // ── Mini reminder list ─────────────────────────────────────────────
            if (item.reminders.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_reminders_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            } else {
                item.reminders.forEach { reminder ->
                    ReminderListRow(
                        reminder = reminder,
                        nextLabel = nextTriggerLabel(reminder),
                        onToggleSkip = { onToggleSkip(reminder) },
                        onDelay = { onDelay(reminder) },
                        onReset = { onReset(reminder) },
                    )
                }
            }

            HorizontalDivider()

            // ── Bottom actions ─────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                TextButton(
                    onClick = onManageReminders,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(stringResource(R.string.details))
                }
                Spacer(Modifier.weight(1f))
                FilledTonalButton(
                    onClick = onAddReminder,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add_reminder))
                }
            }
        }
    }
}

@Composable
private fun ReminderListRow(
    reminder: Reminder,
    nextLabel: String,
    onToggleSkip: () -> Unit,
    onDelay: () -> Unit,
    onReset: () -> Unit,
) {
    val isDelayed = reminder.delayedByMinutes != null &&
        reminder.snoozedUntil != null &&
        reminder.snoozedUntil > System.currentTimeMillis()
    val isModified = reminder.skipNextOccurrence || isDelayed

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (isDelayed) {
                Text(
                    text = stringResource(R.string.delayed_by, formatDelayMinutes(reminder.delayedByMinutes!!)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Text(
                text = stringResource(R.string.next_trigger, nextLabel),
                style = MaterialTheme.typography.bodySmall,
                color = if (isDelayed)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
            )
        }
        FilterChip(
            selected = reminder.skipNextOccurrence,
            onClick = onToggleSkip,
            label = { Text(stringResource(R.string.skip), style = MaterialTheme.typography.labelSmall) },
        )
        Spacer(Modifier.width(4.dp))
        OutlinedButton(
            onClick = onDelay,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(stringResource(R.string.delay), style = MaterialTheme.typography.labelSmall)
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
    }
}
