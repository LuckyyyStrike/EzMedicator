package de.ingomohrmann.ezmedicator.ui.screens.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.ingomohrmann.ezmedicator.R
import de.ingomohrmann.ezmedicator.data.database.entities.LogEntry
import de.ingomohrmann.ezmedicator.data.repository.formatDelayMinutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onBack: () -> Unit,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_log)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Filled.DeleteSweep, stringResource(R.string.log_clear))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Text(
                    stringResource(R.string.log_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    LogEntryCard(entry = entry, timestamp = viewModel.formatTimestamp(entry.timestamp))
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.log_clear)) },
            text = { Text(stringResource(R.string.log_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clear()
                    showClearConfirm = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun LogEntryCard(entry: LogEntry, timestamp: String) {
    val (icon, iconTint, typeLabel) = entryMeta(entry.type)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.medicationName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                val detail = if (entry.delayMinutes != null)
                    "$typeLabel · ${formatDelayMinutes(entry.delayMinutes)}"
                else
                    typeLabel
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun entryMeta(type: String): Triple<ImageVector, androidx.compose.ui.graphics.Color, String> {
    return when (type) {
        LogEntry.TYPE_TRIGGERED -> Triple(
            Icons.Filled.Alarm,
            MaterialTheme.colorScheme.primary,
            stringResource(R.string.log_triggered),
        )
        LogEntry.TYPE_DISMISSED -> Triple(
            Icons.Filled.CheckCircle,
            MaterialTheme.colorScheme.tertiary,
            stringResource(R.string.log_dismissed),
        )
        LogEntry.TYPE_DELAYED_MANUAL -> Triple(
            Icons.Filled.Schedule,
            MaterialTheme.colorScheme.secondary,
            stringResource(R.string.log_delayed_manual),
        )
        LogEntry.TYPE_DELAYED_AUTO -> Triple(
            Icons.Filled.Snooze,
            MaterialTheme.colorScheme.secondary,
            stringResource(R.string.log_delayed_auto),
        )
        LogEntry.TYPE_SKIPPED -> Triple(
            Icons.Filled.SkipNext,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.log_skipped),
        )
        LogEntry.TYPE_RESET -> Triple(
            Icons.Filled.Refresh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.log_reset),
        )
        else -> Triple(
            Icons.Filled.Alarm,
            MaterialTheme.colorScheme.onSurfaceVariant,
            type,
        )
    }
}
