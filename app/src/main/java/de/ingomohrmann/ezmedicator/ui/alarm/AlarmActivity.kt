package de.ingomohrmann.ezmedicator.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.ingomohrmann.ezmedicator.notification.NotificationHelper
import de.ingomohrmann.ezmedicator.receiver.NotificationActionReceiver
import de.ingomohrmann.ezmedicator.ui.theme.EzMedicatorTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val ACTION_FINISH = "de.ingomohrmann.ezmedicator.ACTION_FINISH_ALARM"
    }

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish()
    }

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
        val notifId = NotificationHelper.notificationId(reminderId)

        setContent {
            EzMedicatorTheme {
                AlarmScreen(
                    medicationName = medicationName,
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
        unregisterReceiver(finishReceiver)
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
    onDismiss: () -> Unit,
    onDelay: (Long) -> Unit,
) {
    val time = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    var showDelayMenu by remember { mutableStateOf(false) }

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
                        listOf(
                            "5 minutes" to 5L * 60_000,
                            "15 minutes" to 15L * 60_000,
                            "30 minutes" to 30L * 60_000,
                            "1 hour" to 60L * 60_000,
                        ).forEach { (label, ms) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    showDelayMenu = false
                                    onDelay(ms)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
