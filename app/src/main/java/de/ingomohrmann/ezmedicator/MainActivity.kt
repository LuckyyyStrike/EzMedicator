package de.ingomohrmann.ezmedicator

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import de.ingomohrmann.ezmedicator.ui.navigation.AppNavGraph
import de.ingomohrmann.ezmedicator.ui.theme.EzMedicatorTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EzMedicatorTheme {
                PermissionGate {
                    AppNavGraph()
                }
            }
        }
    }
}

@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    // POST_NOTIFICATIONS (Android 13+)
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // SCHEDULE_EXACT_ALARM (Android 12 / API 31-32)
    val needsExactAlarmPrompt = remember {
        Build.VERSION.SDK_INT in Build.VERSION_CODES.S..Build.VERSION_CODES.S_V2 &&
                !(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                    .canScheduleExactAlarms()
    }
    var exactAlarmDismissed by remember { mutableStateOf(false) }

    if (needsExactAlarmPrompt && !exactAlarmDismissed) {
        AlertDialog(
            onDismissRequest = { exactAlarmDismissed = true },
            title = { Text(stringResource(R.string.permission_exact_alarm_title)) },
            text = { Text(stringResource(R.string.permission_exact_alarm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    exactAlarmDismissed = true
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }) { Text(stringResource(R.string.permission_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { exactAlarmDismissed = true }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // USE_FULL_SCREEN_INTENT (Android 14+ / API 34): required for the alarm screen
    // to show over the lock screen. Without it reminders still work but won't pop up
    // when the screen is off.
    val needsFullScreenPrompt = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !context.getSystemService(NotificationManager::class.java)
                    .canUseFullScreenIntent()
    }
    var fullScreenDismissed by remember { mutableStateOf(false) }

    if (needsFullScreenPrompt && !fullScreenDismissed) {
        AlertDialog(
            onDismissRequest = { fullScreenDismissed = true },
            title = { Text(stringResource(R.string.permission_full_screen_title)) },
            text = { Text(stringResource(R.string.permission_full_screen_message)) },
            confirmButton = {
                TextButton(onClick = {
                    fullScreenDismissed = true
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    })
                }) { Text(stringResource(R.string.permission_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { fullScreenDismissed = true }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    content()
}
