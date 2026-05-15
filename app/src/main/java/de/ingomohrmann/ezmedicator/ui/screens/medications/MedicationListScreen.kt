package de.ingomohrmann.ezmedicator.ui.screens.medications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.ingomohrmann.ezmedicator.R
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.ui.components.MedicationImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onAddReminder: (Long) -> Unit,
    viewModel: MedicationListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    var deleteTarget by remember { mutableStateOf<Medication?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.medications)) })
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
}

@Composable
private fun MedicationCard(
    item: MedicationListItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageReminders: () -> Unit,
    onAddReminder: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // ── Medication header ─────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 8.dp),
            ) {
                MedicationImage(imagePath = item.medication.imagePath)
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

            // ── Reminders row ─────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                TextButton(
                    onClick = onManageReminders,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(
                        text = when (item.reminderCount) {
                            0 -> stringResource(R.string.no_reminders_short)
                            1 -> stringResource(R.string.one_reminder)
                            else -> stringResource(R.string.n_reminders, item.reminderCount)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
