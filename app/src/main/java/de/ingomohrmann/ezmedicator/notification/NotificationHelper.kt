package de.ingomohrmann.ezmedicator.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ingomohrmann.ezmedicator.MainActivity
import de.ingomohrmann.ezmedicator.R
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import de.ingomohrmann.ezmedicator.receiver.NotificationActionReceiver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        fun notificationId(reminderId: Long) = reminderId.toInt()
    }

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminder(reminder: Reminder, medication: Medication) {
        val notifId = notificationId(reminder.id)

        val tapIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissIntent = actionPendingIntent(
            NotificationActionReceiver.ACTION_DISMISS,
            reminder.id,
            notifId,
            requestCode = notifId,
        )
        val delayIntent = actionPendingIntent(
            NotificationActionReceiver.ACTION_DELAY,
            reminder.id,
            notifId,
            requestCode = notifId + 50_000,
        )

        val soundUri: Uri? = when {
            !reminder.soundEnabled -> Uri.EMPTY
            reminder.soundUri != null -> Uri.parse(reminder.soundUri)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val vibrationPattern = if (reminder.vibrationEnabled) longArrayOf(0, 300, 200, 300) else null

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_title, medication.title))
            .setContentText(medication.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(soundUri)
            .apply { vibrationPattern?.let { setVibrate(it) } }
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
