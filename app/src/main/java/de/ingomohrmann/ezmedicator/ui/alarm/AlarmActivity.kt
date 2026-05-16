package de.ingomohrmann.ezmedicator.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import de.ingomohrmann.ezmedicator.data.repository.AppSettingsRepository
import de.ingomohrmann.ezmedicator.data.repository.formatDelayMinutes
import javax.inject.Inject
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import de.ingomohrmann.ezmedicator.notification.NotificationHelper
import de.ingomohrmann.ezmedicator.receiver.NotificationActionReceiver
import de.ingomohrmann.ezmedicator.ui.theme.EzMedicatorTheme
import android.text.format.DateFormat
import java.util.Date

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: AppSettingsRepository

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_TIMEOUT_SECONDS = "timeout_seconds"
        const val EXTRA_SOUND_ENABLED = "sound_enabled"
        const val EXTRA_SOUND_URI = "sound_uri"
        const val EXTRA_VIBRATION_ENABLED = "vibration_enabled"
        const val ACTION_FINISH = "de.ingomohrmann.ezmedicator.ACTION_FINISH_ALARM"
    }

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish()
    }

    private var ringtone: Ringtone? = null
    @Suppress("DEPRECATION")
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn screen on and show over lock screen — same behaviour as a clock alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        enableEdgeToEdge()

        val filter = IntentFilter(ACTION_FINISH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(finishReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(finishReceiver, filter)
        }

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: ""
        val timeoutSeconds = intent.getIntExtra(EXTRA_TIMEOUT_SECONDS, 0)
        val soundEnabled = intent.getBooleanExtra(EXTRA_SOUND_ENABLED, true)
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI)
        val vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, true)
        val notifId = NotificationHelper.notificationId(reminderId)

        startAlarmSound(soundEnabled, soundUri)
        startVibration(vibrationEnabled)

        setContent {
            EzMedicatorTheme {
                val delaySteps by settingsRepository.delaySteps.collectAsState()
                AlarmScreen(
                    medicationName = medicationName,
                    timeoutSeconds = timeoutSeconds,
                    delaySteps = delaySteps,
                    onDismiss = {
                        dispatch(NotificationActionReceiver.ACTION_DISMISS, reminderId, notifId)
                        finish()
                    },
                    onDelay = { delayMs ->
                        dispatch(NotificationActionReceiver.ACTION_DELAY, reminderId, notifId, delayMs)
                        finish()
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        unregisterReceiver(finishReceiver)
    }

    private fun startAlarmSound(soundEnabled: Boolean, soundUri: String?) {
        if (!soundEnabled) return
        val uri = soundUri?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration(vibrationEnabled: Boolean) {
        if (!vibrationEnabled) return
        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500), 0)
        )
    }

    private fun stopAlarmSound() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun dispatch(
        action: String,
        reminderId: Long,
        notifId: Int,
        delayMs: Long = NotificationActionReceiver.DEFAULT_DELAY_MS,
    ) {
        sendBroadcast(
            Intent(this, NotificationActionReceiver::class.java).apply {
                this.action = action
                putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
                putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, notifId)
                putExtra("delay_ms", delayMs)
            }
        )
    }
}

@Composable
private fun AlarmScreen(
    medicationName: String,
    timeoutSeconds: Int,
    delaySteps: List<Int>,
    onDismiss: () -> Unit,
    onDelay: (Long) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val time = remember {
        DateFormat.getTimeFormat(context).format(Date())
    }
    var showDelayMenu by remember { mutableStateOf(false) }

    var remainingSeconds by remember { mutableIntStateOf(timeoutSeconds) }
    if (timeoutSeconds > 0) {
        LaunchedEffect(Unit) {
            while (remainingSeconds > 0) {
                delay(1_000L)
                remainingSeconds--
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .systemBarsPadding(),
        ) {
            // Clock
            Text(
                text = time,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Medication info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = "Time to take",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = medicationName,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }

            // Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.titleMedium)
                }

                Box {
                    OutlinedButton(
                        onClick = { showDelayMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text("Delay…", style = MaterialTheme.typography.titleMedium)
                    }
                    DropdownMenu(
                        expanded = showDelayMenu,
                        onDismissRequest = { showDelayMenu = false },
                    ) {
                        delaySteps.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text(formatDelayMinutes(minutes)) },
                                onClick = {
                                    showDelayMenu = false
                                    onDelay(minutes * 60_000L)
                                },
                            )
                        }
                    }
                }

                if (timeoutSeconds > 0) {
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    Text(
                        text = "Auto-delays in %d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
