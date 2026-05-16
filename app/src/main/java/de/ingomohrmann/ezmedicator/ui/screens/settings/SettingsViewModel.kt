package de.ingomohrmann.ezmedicator.ui.screens.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.ingomohrmann.ezmedicator.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {

    val delaySteps: StateFlow<List<Int>> = settingsRepository.delaySteps
    val defaultTimeoutSeconds: StateFlow<Int> = settingsRepository.defaultTimeoutSeconds
    val defaultAutoDelayMinutes: StateFlow<Int> = settingsRepository.defaultAutoDelayMinutes

    fun addStep(minutes: Int) {
        val updated = (delaySteps.value + minutes).distinct().sorted()
        settingsRepository.saveDelaySteps(updated)
    }

    fun removeStep(minutes: Int) {
        settingsRepository.saveDelaySteps(delaySteps.value.filter { it != minutes })
    }

    fun saveDefaultTimeoutSeconds(value: Int) = settingsRepository.saveDefaultTimeoutSeconds(value)
    fun saveDefaultAutoDelayMinutes(value: Int) = settingsRepository.saveDefaultAutoDelayMinutes(value)
}
