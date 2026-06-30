package de.ingomohrmann.ezmedicator.ui.screens.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ingomohrmann.ezmedicator.data.repository.MedicationRepository
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import de.ingomohrmann.ezmedicator.domain.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ScheduledAlarmInfo(
    val reminderId: Long,
    val medicationName: String,
    val cronExpression: String,
    val isEnabled: Boolean,
    val regularScheduled: Boolean,
    val snoozeScheduled: Boolean,
    val snoozedUntil: Long?,
)

@HiltViewModel
class ScheduledAlarmsViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val medicationRepository: MedicationRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val alarms = combine(
        reminderRepository.observeAll(),
        medicationRepository.observeAll(),
    ) { reminders, medications ->
        val medMap = medications.associateBy { it.id }
        reminders.mapNotNull { reminder ->
            val med = medMap[reminder.medicationId] ?: return@mapNotNull null
            ScheduledAlarmInfo(
                reminderId = reminder.id,
                medicationName = med.title,
                cronExpression = reminder.cronExpression,
                isEnabled = reminder.isEnabled,
                regularScheduled = reminderScheduler.isScheduled(reminder.id, isSnooze = false),
                snoozeScheduled = reminderScheduler.isScheduled(reminder.id, isSnooze = true),
                snoozedUntil = reminder.snoozedUntil,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
