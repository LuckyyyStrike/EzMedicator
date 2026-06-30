package de.ingomohrmann.ezmedicator.ui.screens.alarms

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.ingomohrmann.ezmedicator.R
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledAlarmsScreen(
    onBack: () -> Unit,
    viewModel: ScheduledAlarmsViewModel = hiltViewModel(),
) {
    val alarms by viewModel.alarms.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scheduled_alarms)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.scheduled_alarms_empty),
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(alarms, key = { it.reminderId }) { info ->
                    AlarmInfoCard(info)
                }
            }
        }
    }
}

@Composable
private fun AlarmInfoCard(info: ScheduledAlarmInfo) {
    val context = LocalContext.current
    val timeFormat = remember { DateFormat.getTimeFormat(context) }
    val dateFormat = remember { DateFormat.getDateFormat(context) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (!info.isEnabled) {
                    Text(
                        text = stringResource(R.string.reminder_disabled),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = info.cronExpression,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            AlarmStatusRow(
                icon = if (info.regularScheduled) Icons.Filled.Alarm else Icons.Filled.AlarmOff,
                label = stringResource(R.string.alarm_regular),
                scheduled = info.regularScheduled,
            )

            val snoozeLabel = if (info.snoozeScheduled && info.snoozedUntil != null) {
                val date = Date(info.snoozedUntil)
                stringResource(
                    R.string.alarm_snooze_at,
                    dateFormat.format(date),
                    timeFormat.format(date),
                )
            } else {
                stringResource(R.string.alarm_snooze)
            }
            AlarmStatusRow(
                icon = Icons.Filled.Snooze,
                label = snoozeLabel,
                scheduled = info.snoozeScheduled,
            )
        }
    }
}

@Composable
private fun AlarmStatusRow(
    icon: ImageVector,
    label: String,
    scheduled: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (scheduled) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (scheduled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (scheduled) "✓" else "–",
            style = MaterialTheme.typography.bodyMedium,
            color = if (scheduled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
