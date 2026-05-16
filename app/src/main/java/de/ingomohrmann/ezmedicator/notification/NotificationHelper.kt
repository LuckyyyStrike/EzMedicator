package de.ingomohrmann.ezmedicator.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ingomohrmann.ezmedicator.R
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import de.ingomohrmann.ezmedicator.data.repository.formatDelayMinutes
import de.ingomohrmann.ezmedicator.receiver.NotificationActionReceiver
import de.ingomohrmann.ezmedicator.ui.alarm.AlarmActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        // Increment the suffix whenever the channel audio/vibration settings change.
        // Android ignores createNotificationChannel() calls for existing channel IDs,
        // so a new ID is the only way to apply updated settings.
        const val CHANNEL_ID = "medication_reminders_v3"
        private const val CHANNEL_ID_LEGACY = "medication_reminders"
        private const val CHANNEL_ID_LEGACY_V2 = "medication_reminders_v2"
        const val CHANNEL_ID_INFO = "medication_info"

        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        fun notificationId(reminderId: Long) = reminderId.toInt()
        fun autoDelayedNotificationId(reminderId: Long) = (reminderId + 500_000L).toInt()
    }

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        // Remove legacy channels.
        manager.deleteNotificationChannel(CHANNEL_ID_LEGACY)
        manager.deleteNotificationChannel(CHANNEL_ID_LEGACY_V2)

        // Alarm channel — silent; AlarmActivity handles sound and vibration directly
        // so the per-reminder sound selection is respected.
        val alarmChannel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(alarmChannel)

        // Info channel — silent, for informational messages like auto-delay confirmations.
        val infoChannel = NotificationChannel(
            CHANNEL_ID_INFO,
            context.getString(R.string.notification_channel_info_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(infoChannel)
    }

    fun showReminder(reminder: Reminder, medication: Medication) {
        val notifId = notificationId(reminder.id)

        // Full-screen intent: shows AlarmActivity over the lock screen (alarm-clock style).
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            notifId + 300_000,
            Intent(context, AlarmActivity::class.java).apply {
                putExtra(AlarmActivity.EXTRA_REMINDER_ID, reminder.id)
                putExtra(AlarmActivity.EXTRA_MEDICATION_NAME, medication.title)
                putExtra(AlarmActivity.EXTRA_TIMEOUT_SECONDS, reminder.notificationTimeoutSeconds)
                putExtra(AlarmActivity.EXTRA_SOUND_ENABLED, reminder.soundEnabled)
                putExtra(AlarmActivity.EXTRA_SOUND_URI, reminder.soundUri)
                putExtra(AlarmActivity.EXTRA_VIBRATION_ENABLED, reminder.vibrationEnabled)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissIntent = actionPendingIntent(
            NotificationActionReceiver.ACTION_DISMISS,
            reminder.id, notifId,
            requestCode = notifId,
        )
        val delayIntent = actionPendingIntent(
            NotificationActionReceiver.ACTION_DELAY,
            reminder.id, notifId,
            requestCode = notifId + 50_000,
        )

        // On API 26+, setSound() / setVibrate() on the builder are ignored — the channel
        // controls both. Sound and vibration are configured in createChannel().
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_title, medication.title))
            .setContentText(medication.title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, /* highPriority = */ true)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_dismiss),
                dismissIntent,
            )
            .addAction(
                android.R.drawable.ic_menu_rotate,
                context.getString(R.string.notification_delay),
                delayIntent,
            )
            .build()

        manager.notify(notifId, notification)
    }

    fun dismiss(notificationId: Int) = manager.cancel(notificationId)

    fun showAutoDelayed(medicationName: String, delayMinutes: Int, reminderId: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INFO)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.auto_delayed_title))
            .setContentText(
                context.getString(
                    R.string.auto_delayed_text,
                    medicationName,
                    formatDelayMinutes(delayMinutes),
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(autoDelayedNotificationId(reminderId), notification)
    }

    private fun actionPendingIntent(
        action: String,
        reminderId: Long,
        notificationId: Int,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
