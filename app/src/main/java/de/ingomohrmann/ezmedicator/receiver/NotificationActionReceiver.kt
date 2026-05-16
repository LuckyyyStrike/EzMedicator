package de.ingomohrmann.ezmedicator.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import de.ingomohrmann.ezmedicator.data.database.entities.LogEntry
import de.ingomohrmann.ezmedicator.data.repository.LogRepository
import de.ingomohrmann.ezmedicator.data.repository.MedicationRepository
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import de.ingomohrmann.ezmedicator.domain.ReminderScheduler
import de.ingomohrmann.ezmedicator.notification.NotificationHelper
import de.ingomohrmann.ezmedicator.ui.alarm.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "de.ingomohrmann.ezmedicator.ACTION_DISMISS"
        const val ACTION_DELAY = "de.ingomohrmann.ezmedicator.ACTION_DELAY"
        const val ACTION_TIMEOUT = "de.ingomohrmann.ezmedicator.ACTION_TIMEOUT"

        const val DEFAULT_DELAY_MS = 30 * 60 * 1_000L
    }

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var logRepository: LogRepository

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        val delayMs = intent.getLongExtra("delay_ms", DEFAULT_DELAY_MS)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DISMISS -> handleDismiss(reminderId, notifId, context)
                    ACTION_DELAY -> handleDelay(reminderId, notifId, delayMs, context, isAutoDelay = false)
                    ACTION_TIMEOUT -> handleDelay(reminderId, notifId, delayMs, context, isAutoDelay = true)
                }
            } finally {
                result.finish()
            }
        }
    }

    private suspend fun handleDismiss(reminderId: Long, notifId: Int, context: Context) {
        cancelTimeoutAlarm(context, reminderId)
        notificationHelper.dismiss(notifId)
        sendFinishAlarm(context)

        val reminder = reminderRepository.getById(reminderId) ?: return
        val medication = medicationRepository.getById(reminder.medicationId) ?: return
        logRepository.log(LogEntry(
            timestamp = System.currentTimeMillis(),
            type = LogEntry.TYPE_DISMISSED,
            medicationName = medication.title,
            reminderId = reminderId,
        ))
    }

    private suspend fun handleDelay(reminderId: Long, notifId: Int, delayMs: Long, context: Context, isAutoDelay: Boolean) {
        cancelTimeoutAlarm(context, reminderId)
        notificationHelper.dismiss(notifId)
        sendFinishAlarm(context)

        val snoozeAt = System.currentTimeMillis() + delayMs
        val delayMinutes = (delayMs / 60_000L).toInt()
        reminderRepository.setSnoozeState(reminderId, snoozeAt, delayMinutes)
        reminderScheduler.scheduleSnooze(reminderId, snoozeAt)

        val reminder = reminderRepository.getById(reminderId) ?: return
        val medication = medicationRepository.getById(reminder.medicationId) ?: return
        logRepository.log(LogEntry(
            timestamp = System.currentTimeMillis(),
            type = if (isAutoDelay) LogEntry.TYPE_DELAYED_AUTO else LogEntry.TYPE_DELAYED_MANUAL,
            medicationName = medication.title,
            reminderId = reminderId,
            delayMinutes = delayMinutes,
        ))

        if (isAutoDelay) {
            notificationHelper.showAutoDelayed(medication.title, delayMinutes, reminderId)
        }
    }

    private fun sendFinishAlarm(context: Context) {
        context.sendBroadcast(
            Intent(AlarmActivity.ACTION_FINISH).apply { setPackage(context.packageName) }
        )
    }

    private fun cancelTimeoutAlarm(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = (reminderId + 200_000L).toInt()
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TIMEOUT
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pending?.let { alarmManager.cancel(it) }
    }
}
