package de.ingomohrmann.ezmedicator.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.ingomohrmann.ezmedicator.R
import de.ingomohrmann.ezmedicator.data.repository.formatDelayMinutes
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onActivityLog: () -> Unit,
    onScheduledAlarms: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val delaySteps by viewModel.delaySteps.collectAsState()
    val defaultTimeout by viewModel.defaultTimeoutSeconds.collectAsState()
    val defaultAutoDelay by viewModel.defaultAutoDelayMinutes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var timeoutInput by remember(defaultTimeout) { mutableStateOf(defaultTimeout.toString()) }
    var autoDelayInput by remember(defaultAutoDelay) { mutableStateOf(defaultAutoDelay.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.delay_steps_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(delaySteps, key = { it }) { minutes ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(
                            formatDelayMinutes(minutes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { viewModel.removeStep(minutes) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_delay_step))
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    stringResource(R.string.new_reminder_defaults),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = timeoutInput,
                    onValueChange = { v ->
                        timeoutInput = v.filter { it.isDigit() }
                        timeoutInput.toIntOrNull()?.takeIf { it > 0 }
                            ?.let { viewModel.saveDefaultTimeoutSeconds(it) }
                    },
                    label = { Text(stringResource(R.string.timeout_seconds)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = autoDelayInput,
                    onValueChange = { v ->
                        autoDelayInput = v.filter { it.isDigit() }
                        autoDelayInput.toIntOrNull()?.takeIf { it > 0 }
                            ?.let { viewModel.saveDefaultAutoDelayMinutes(it) }
                    },
                    label = { Text(stringResource(R.string.auto_delay_minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedButton(
                    onClick = onActivityLog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.activity_log))
                }
            }

            item {
                OutlinedButton(
                    onClick = onScheduledAlarms,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Alarm, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scheduled_alarms))
                }
            }
        }
    }

    if (showAddDialog) {
        AddDelayStepDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { minutes ->
                viewModel.addStep(minutes)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddDelayStepDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_delay_step)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.delay_minutes_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { input.toIntOrNull()?.takeIf { it > 0 }?.let { onConfirm(it) } },
                enabled = input.toIntOrNull()?.let { it > 0 } == true,
            ) { Text(stringResource(R.string.add_delay_step)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
