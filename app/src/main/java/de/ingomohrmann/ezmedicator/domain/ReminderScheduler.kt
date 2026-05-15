package de.ingomohrmann.ezmedicator.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import de.ingomohrmann.ezmedicator.receiver.AlarmReceiver
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        if (!reminder.isEnabled) {
            cancel(reminder.id)
            return
        }

        val triggerAtMillis = nextTriggerMillis(reminder) ?: return
        val pending = buildPendingIntent(reminder.id, isSnooze = false) ?: return

        setAlarm(triggerAtMillis, pending)
    }

    fun scheduleSnooze(reminderId: Long, triggerAtMillis: Long) {
        val pending = buildPendingIntent(reminderId, isSnooze = true) ?: return
        setAlarm(triggerAtMillis, pending)
    }

    fun cancel(reminderId: Long) {
        listOf(false, true).forEach { isSnooze ->
            buildPendingIntent(reminderId, isSnooze, noCreate = true)?.let {
                alarmManager.cancel(it)
            }
        }
    }

    suspend fun rescheduleAll() {
        reminderRepository.getAllEnabled().forEach { schedule(it) }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun setAlarm(triggerAtMillis: Long, pending: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    private fun nextTriggerMillis(reminder: Reminder): Long? {
        // Explicit snooze takes priority
        reminder.snoozedUntil?.let { until ->
            if (until > System.currentTimeMillis()) return until
        }

        val baseFrom = ZonedDateTime.now()
        return if (reminder.skipNextOccurrence) {
            CronHelper.secondNextExecution(reminder.cronExpression)
        } else {
            CronHelper.nextExecution(reminder.cronExpression, baseFrom)
        }?.toInstant()?.toEpochMilli()
    }

    private fun buildPendingIntent(
        reminderId: Long,
        isSnooze: Boolean,
        noCreate: Boolean = false,
    ): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, isSnooze)
        }
        val flags = (if (noCreate) PendingIntent.FLAG_NO_CREATE else PendingIntent.FLAG_UPDATE_CURRENT) or
                PendingIntent.FLAG_IMMUTABLE
        // Use different request codes for regular vs snooze alarms so they don't collide
        val requestCode = if (isSnooze) (reminderId + 100_000L).toInt() else reminderId.toInt()
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
