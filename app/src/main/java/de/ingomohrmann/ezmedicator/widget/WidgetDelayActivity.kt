package de.ingomohrmann.ezmedicator.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.ingomohrmann.ezmedicator.data.database.entities.LogEntry
import de.ingomohrmann.ezmedicator.data.repository.AppSettingsRepository
import de.ingomohrmann.ezmedicator.data.repository.LogRepository
import de.ingomohrmann.ezmedicator.data.repository.MedicationRepository
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import de.ingomohrmann.ezmedicator.domain.CronHelper
import de.ingomohrmann.ezmedicator.domain.ReminderScheduler
import de.ingomohrmann.ezmedicator.ui.screens.medications.DelayDialog
import de.ingomohrmann.ezmedicator.ui.theme.EzMedicatorTheme
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@AndroidEntryPoint
class WidgetDelayActivity : ComponentActivity() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var logRepository: LogRepository
    @Inject lateinit var settingsRepository: AppSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) { finish(); return }

        setContent {
            EzMedicatorTheme {
                val delaySteps by settingsRepository.delaySteps.collectAsState()
                DelayDialog(
                    presets = delaySteps,
                    onDismiss = ::finish,
                    onConfirm = { minutes ->
                        lifecycleScope.launch { applyDelay(reminderId, minutes) }
                    },
                )
            }
        }
    }

    private suspend fun applyDelay(reminderId: Long, delayMinutes: Int) {
        val reminder = reminderRepository.getById(reminderId) ?: run { finish(); return }
        val medName = medicationRepository.getById(reminder.medicationId)?.title ?: ""
        val baseTime = CronHelper.nextExecution(reminder.cronExpression) ?: ZonedDateTime.now()
        val snoozedUntil = baseTime.toInstant().toEpochMilli() + delayMinutes * 60_000L
        val updated = reminder.copy(
            snoozedUntil = snoozedUntil,
            skipNextOccurrence = false,
            delayedByMinutes = delayMinutes,
        )
        reminderRepository.save(updated)
        reminderScheduler.schedule(updated)
        logRepository.log(
            LogEntry(
                timestamp = System.currentTimeMillis(),
                type = LogEntry.TYPE_DELAYED_MANUAL,
                medicationName = medName,
                reminderId = reminderId,
                delayMinutes = delayMinutes,
            )
        )
        MedicationWidget().updateAll(this@WidgetDelayActivity)
        finish()
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
