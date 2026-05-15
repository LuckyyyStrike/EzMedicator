package de.ingomohrmann.ezmedicator.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import de.ingomohrmann.ezmedicator.data.repository.MedicationRepository
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import de.ingomohrmann.ezmedicator.domain.ReminderScheduler
import de.ingomohrmann.ezmedicator.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_IS_SNOOZE = "is_snooze"
    }

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(context, reminderId, isSnooze)
            } finally {
                result.finish()
            }
        }
    }

    private suspend fun handle(context: Context, reminderId: Long, isSnooze: Boolean) {
        val reminder = reminderRepository.getById(reminderId) ?: return
        val medication = medicationRepository.getById(reminder.medicationId) ?: return

        if (!reminder.isEnabled) return

        if (!isSnooze && reminder.skipNextOccurrence) {
            // Skip this occurrence: clear flag, schedule next
            reminderRepository.setSkipNext(reminderId, false)
            reminderScheduler.schedule(reminder.copy(skipNextOccurrence = false))
            return
        }

        // Clear snooze state now that the snooze alarm has fired
        if (isSnooze) {
            reminderRepository.setSnoozedUntil(reminderId, null)
        }

        notificationHelper.showReminder(reminder, medication)
        scheduleTimeout(context, reminderId, reminder.notificationTimeoutSeconds)

        // Schedule the next regular cron occurrence (only for non-snooze alarms)
        if (!isSnooze) {
            reminderScheduler.schedule(reminder.copy(skipNextOccurrence = false, snoozedUntil = null))
        }
    }

    private fun scheduleTimeout(context: Context, reminderId: Long, timeoutSeconds: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TIMEOUT
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, NotificationHelper.notificationId(reminderId))
        }
        val requestCode = (reminderId + 200_000L).toInt()
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = System.currentTimeMillis() + timeoutSeconds * 1_000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }
}
