package de.ingomohrmann.ezmedicator.ui.screens.reminders

import android.media.RingtoneManager
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.ingomohrmann.ezmedicator.BuildConfig
import de.ingomohrmann.ezmedicator.R
import de.ingomohrmann.ezmedicator.ui.components.CronTextField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditScreen(
    medicationId: Long,
    reminderId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ReminderEditViewModel = hiltViewModel(),
) {
    LaunchedEffect(medicationId, reminderId) { viewModel.load(medicationId, reminderId) }

    val cron by viewModel.cronExpression.collectAsState()
    val isEnabled by viewModel.isEnabled.collectAsState()
    val timeoutSeconds by viewModel.timeoutSeconds.collectAsState()
    val autoDelayMinutes by viewModel.autoDelayMinutes.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val soundUri by viewModel.soundUri.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(saved) { if (saved) onSaved() }

    var timeoutInput by remember(timeoutSeconds) { mutableStateOf(timeoutSeconds.toString()) }
    var autoDelayInput by remember(autoDelayMinutes) { mutableStateOf(autoDelayMinutes.toString()) }

    val context = LocalContext.current
    val todayLabel = remember {
        val date = Date()
        val dow = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        val datePart = DateFormat.getDateFormat(context).format(date)
        "$dow $datePart"
    }
    val defaultSoundLabel = stringResource(R.string.default_sound)
    val soundLabel = remember(soundUri, context) {
        soundUri?.let { uri ->
            RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context)
                ?: defaultSoundLabel
        } ?: defaultSoundLabel
    }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        viewModel.setSoundUri(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (reminderId == null) stringResource(R.string.add_reminder)
                        else stringResource(R.string.edit_reminder)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }, enabled = isValid) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Enabled toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.reminder_enabled),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = isEnabled, onCheckedChange = { viewModel.setEnabled(it) })
            }

            HorizontalDivider()

            // Cron expression
            CronTextField(
                value = cron,
                onValueChange = { viewModel.setCron(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.today, todayLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (BuildConfig.DEBUG) {
                val now = java.time.LocalTime.now().plusMinutes(1)
                OutlinedButton(
                    onClick = { viewModel.setCron("${now.minute} ${now.hour} * * *") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("DEBUG: set to now +1 min (${now.hour}:${"%02d".format(now.minute)})")
                }
            }

            HorizontalDivider()

            // Notification settings
            Text(
                stringResource(R.string.notification_settings),
                style = MaterialTheme.typography.titleMedium,
            )

            // Timeout
            OutlinedTextField(
                value = timeoutInput,
                onValueChange = { v ->
                    timeoutInput = v.filter { it.isDigit() }
                    timeoutInput.toIntOrNull()?.let { viewModel.setTimeout(it) }
                },
                label = { Text(stringResource(R.string.timeout_seconds)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            // Auto-delay duration
            OutlinedTextField(
                value = autoDelayInput,
                onValueChange = { v ->
                    autoDelayInput = v.filter { it.isDigit() }
                    autoDelayInput.toIntOrNull()?.let { viewModel.setAutoDelayMinutes(it) }
                },
                label = { Text(stringResource(R.string.auto_delay_minutes)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            // Vibration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.vibration),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = vibrationEnabled, onCheckedChange = { viewModel.setVibration(it) })
            }

            // Sound
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.sound),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = soundEnabled, onCheckedChange = { viewModel.setSoundEnabled(it) })
            }

            if (soundEnabled) {
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select notification sound")
                            soundUri?.let {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                            }
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        }
                        ringtonePicker.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "${stringResource(R.string.select_sound)}: $soundLabel")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
