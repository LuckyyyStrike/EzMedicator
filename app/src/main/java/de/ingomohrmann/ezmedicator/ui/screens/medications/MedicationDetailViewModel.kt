package de.ingomohrmann.ezmedicator.ui.screens.medications

import android.content.Context
import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import de.ingomohrmann.ezmedicator.data.repository.AppSettingsRepository
import de.ingomohrmann.ezmedicator.data.repository.LogRepository
import de.ingomohrmann.ezmedicator.data.repository.formatCountdown
import de.ingomohrmann.ezmedicator.data.repository.MedicationRepository
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import de.ingomohrmann.ezmedicator.data.database.entities.LogEntry
import de.ingomohrmann.ezmedicator.domain.CronHelper
import de.ingomohrmann.ezmedicator.domain.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MedicationDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationRepository: MedicationRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val settingsRepository: AppSettingsRepository,
    private val logRepository: LogRepository,
) : ViewModel() {

    private val _medication = MutableStateFlow<Medication?>(null)
    val medication: StateFlow<Medication?> = _medication.asStateFlow()

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    val delaySteps: StateFlow<List<Int>> = settingsRepository.delaySteps

    fun load(medicationId: Long) {
        viewModelScope.launch {
            _medication.value = medicationRepository.getById(medicationId)
        }
        viewModelScope.launch {
            reminderRepository.observeForMedication(medicationId).collect { list ->
                _reminders.value = list
            }
        }
    }

    fun toggleEnabled(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = !reminder.isEnabled)
            reminderRepository.save(updated)
            if (updated.isEnabled) reminderScheduler.schedule(updated)
            else reminderScheduler.cancel(updated.id)
        }
    }

    fun toggleSkipNext(reminder: Reminder) {
        viewModelScope.launch {
            val newSkip = !reminder.skipNextOccurrence
            val updated = reminder.copy(
                skipNextOccurrence = newSkip,
                snoozedUntil = if (newSkip) null else reminder.snoozedUntil,
                delayedByMinutes = if (newSkip) null else reminder.delayedByMinutes,
            )
            reminderRepository.save(updated)
            reminderScheduler.schedule(updated)
            if (newSkip) {
                val medName = _medication.value?.title ?: return@launch
                logRepository.log(LogEntry(
                    timestamp = System.currentTimeMillis(),
                    type = LogEntry.TYPE_SKIPPED,
                    medicationName = medName,
                    reminderId = reminder.id,
                ))
            }
        }
    }

    fun delayNext(reminder: Reminder, delayMinutes: Int) {
        viewModelScope.launch {
            val baseTime = CronHelper.nextExecution(reminder.cronExpression)
                ?: ZonedDateTime.now()
            val snoozedUntil = baseTime.toInstant().toEpochMilli() + delayMinutes * 60_000L
            val updated = reminder.copy(snoozedUntil = snoozedUntil, skipNextOccurrence = false, delayedByMinutes = delayMinutes)
            reminderRepository.save(updated)
            reminderScheduler.schedule(updated)
            val medName = _medication.value?.title ?: return@launch
            logRepository.log(LogEntry(
                timestamp = System.currentTimeMillis(),
                type = LogEntry.TYPE_DELAYED_MANUAL,
                medicationName = medName,
                reminderId = reminder.id,
                delayMinutes = delayMinutes,
            ))
        }
    }

    fun resetNext(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(
                skipNextOccurrence = false,
                snoozedUntil = null,
                delayedByMinutes = null,
            )
            reminderRepository.save(updated)
            reminderScheduler.cancel(updated.id)
            reminderScheduler.schedule(updated)
            val medName = _medication.value?.title ?: return@launch
            logRepository.log(LogEntry(
                timestamp = System.currentTimeMillis(),
                type = LogEntry.TYPE_RESET,
                medicationName = medName,
                reminderId = reminder.id,
            ))
        }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch {
            reminderScheduler.cancel(reminder.id)
            reminderRepository.delete(reminder)
        }
    }

    fun nextTriggerLabel(reminder: Reminder): String {
        val now = System.currentTimeMillis()
        val millis: Long? = when {
            reminder.snoozedUntil != null && reminder.snoozedUntil > now -> reminder.snoozedUntil
            reminder.skipNextOccurrence ->
                CronHelper.secondNextExecution(reminder.cronExpression)?.toInstant()?.toEpochMilli()
            else ->
                CronHelper.nextExecution(reminder.cronExpression)?.toInstant()?.toEpochMilli()
        }
        millis ?: return "–"
        val date = Date(millis)
        val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        val datePart = DateFormat.getDateFormat(context).format(date)
        val timePart = DateFormat.getTimeFormat(context).format(date)
        val countdown = formatCountdown(millis)
        val label = "$dayOfWeek $datePart $timePart"
        return if (countdown != null) "$label (in $countdown)" else label
    }
}
