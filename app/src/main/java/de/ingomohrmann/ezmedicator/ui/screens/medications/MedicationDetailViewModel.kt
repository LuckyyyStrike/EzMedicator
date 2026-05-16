package de.ingomohrmann.ezmedicator.ui.screens.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import de.ingomohrmann.ezmedicator.data.repository.AppSettingsRepository
import de.ingomohrmann.ezmedicator.data.repository.MedicationRepository
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import de.ingomohrmann.ezmedicator.domain.CronHelper
import de.ingomohrmann.ezmedicator.domain.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class MedicationDetailViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val settingsRepository: AppSettingsRepository,
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
        val zdt = java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(zdt)
    }
}
