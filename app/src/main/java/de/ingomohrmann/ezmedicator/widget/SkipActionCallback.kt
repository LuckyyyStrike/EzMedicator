package de.ingomohrmann.ezmedicator.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.EntryPointAccessors
import de.ingomohrmann.ezmedicator.data.database.entities.LogEntry

class SkipActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val reminderId = parameters[reminderIdKey] ?: return
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val repo = ep.reminderRepository()
        val reminder = repo.getById(reminderId) ?: return
        val medName = ep.medicationRepository().getById(reminder.medicationId)?.title ?: ""

        val newSkip = !reminder.skipNextOccurrence
        val updated = reminder.copy(
            skipNextOccurrence = newSkip,
            snoozedUntil = if (newSkip) null else reminder.snoozedUntil,
            delayedByMinutes = if (newSkip) null else reminder.delayedByMinutes,
        )
        repo.save(updated)
        ep.reminderScheduler().schedule(updated)
        if (newSkip) {
            ep.logRepository().log(
                LogEntry(
                    timestamp = System.currentTimeMillis(),
                    type = LogEntry.TYPE_SKIPPED,
                    medicationName = medName,
                    reminderId = reminderId,
                )
            )
        }

        MedicationWidget().updateAll(context)
    }
}
