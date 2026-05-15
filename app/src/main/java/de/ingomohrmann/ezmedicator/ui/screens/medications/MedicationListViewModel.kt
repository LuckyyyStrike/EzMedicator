package de.ingomohrmann.ezmedicator.ui.screens.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.data.repository.MedicationRepository
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicationListItem(val medication: Medication, val reminderCount: Int)

@HiltViewModel
class MedicationListViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    val items = combine(
        medicationRepository.observeAll(),
        reminderRepository.observeCountsByMedication(),
    ) { medications, counts ->
        medications.map { med -> MedicationListItem(med, counts[med.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.delete(medication)
        }
    }
}
