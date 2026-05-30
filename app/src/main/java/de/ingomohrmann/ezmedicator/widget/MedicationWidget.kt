package de.ingomohrmann.ezmedicator.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.android.EntryPointAccessors
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import de.ingomohrmann.ezmedicator.data.repository.formatCountdown
import de.ingomohrmann.ezmedicator.domain.CronHelper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val reminderIdKey = ActionParameters.Key<Long>("reminder_id")

data class WidgetReminder(
    val reminderId: Long,
    val medicationName: String,
    val nextLabel: String,
    val nextMillis: Long,
    val isSkipped: Boolean,
    val autoDelayMinutes: Int,
)

class MedicationWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val reminders = ep.reminderRepository().getAllEnabled()
        val medications = ep.medicationRepository().observeAll().first()

        val medMap = medications.associateBy { it.id }
        val now = System.currentTimeMillis()

        val items = reminders
            .mapNotNull { reminder ->
                val med = medMap[reminder.medicationId] ?: return@mapNotNull null
                val millis = nextMillis(reminder, now) ?: return@mapNotNull null
                WidgetReminder(
                    reminderId = reminder.id,
                    medicationName = med.title,
                    nextLabel = nextLabel(context, reminder, millis),
                    nextMillis = millis,
                    isSkipped = reminder.skipNextOccurrence,
                    autoDelayMinutes = reminder.autoDelayMinutes,
                )
            }
            .sortedBy { it.nextMillis }
            .take(5)

        provideContent {
            GlanceTheme {
                WidgetContent(items)
            }
        }
    }

    private fun nextMillis(reminder: Reminder, now: Long): Long? {
        reminder.snoozedUntil?.let { if (it > now) return it }
        val next = if (reminder.skipNextOccurrence)
            CronHelper.secondNextExecution(reminder.cronExpression)
        else
            CronHelper.nextExecution(reminder.cronExpression)
        return next?.toInstant()?.toEpochMilli()
    }

    private fun nextLabel(context: Context, reminder: Reminder, millis: Long): String {
        val date = Date(millis)
        val dow = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        val time = DateFormat.getTimeFormat(context).format(date)
        val countdown = formatCountdown(millis)
        return if (countdown != null) "$dow $time (in $countdown)" else "$dow $time"
    }
}

@Composable
private fun WidgetContent(items: List<WidgetReminder>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "EzMedicator",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurface,
            ),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (items.isEmpty()) {
            Text(
                text = "No reminders scheduled",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(items, itemId = { it.reminderId }) { item ->
                    ReminderWidgetRow(item)
                }
            }
        }
    }
}

@Composable
private fun ReminderWidgetRow(item: WidgetReminder) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = item.medicationName,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
                maxLines = 1,
            )
            Text(
                text = item.nextLabel,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                maxLines = 1,
            )
        }
        Button(
            text = if (item.isSkipped) "✓ Skip" else "Skip",
            onClick = actionRunCallback<SkipActionCallback>(
                actionParametersOf(reminderIdKey to item.reminderId),
            ),
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Button(
            text = "Delay",
            onClick = actionStartActivity<WidgetDelayActivity>(
                actionParametersOf(reminderIdKey to item.reminderId),
            ),
        )
    }
}
