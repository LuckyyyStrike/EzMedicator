package de.ingomohrmann.ezmedicator.ui.screens.reminders

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import de.ingomohrmann.ezmedicator.data.repository.AppSettingsRepository
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import de.ingomohrmann.ezmedicator.domain.CronHelper
import de.ingomohrmann.ezmedicator.domain.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {

    private val _cronExpression = MutableStateFlow("")
    val cronExpression: StateFlow<String> = _cronExpression.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _timeoutSeconds = MutableStateFlow(settingsRepository.defaultTimeoutSeconds.value)
    val timeoutSeconds: StateFlow<Int> = _timeoutSeconds.asStateFlow()

    private val _autoDelayMinutes = MutableStateFlow(settingsRepository.defaultAutoDelayMinutes.value)
    val autoDelayMinutes: StateFlow<Int> = _autoDelayMinutes.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _soundUri = MutableStateFlow<String?>(null)
    val soundUri: StateFlow<String?> = _soundUri.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private var existingId: Long? = null
    private var medicationId: Long = 0

    val isValid: StateFlow<Boolean> = cronExpression
        .map { CronHelper.isValid(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun load(medicationId: Long, reminderId: Long?) {
        this.medicationId = medicationId
        if (reminderId == null) return
        viewModelScope.launch {
            val rem = reminderRepository.getById(reminderId) ?: return@launch
            existingId = rem.id
            _cronExpression.value = rem.cronExpression
            _isEnabled.value = rem.isEnabled
            _timeoutSeconds.value = rem.notificationTimeoutSeconds
            _autoDelayMinutes.value = rem.autoDelayMinutes
            _vibrationEnabled.value = rem.vibrationEnabled
            _soundEnabled.value = rem.soundEnabled
            _soundUri.value = rem.soundUri
        }
    }

    fun setCron(value: String) { _cronExpression.value = value }
    fun setEnabled(value: Boolean) { _isEnabled.value = value }
    fun setTimeout(value: Int) { _timeoutSeconds.value = value }
    fun setAutoDelayMinutes(value: Int) { _autoDelayMinutes.value = value }
    fun setVibration(value: Boolean) { _vibrationEnabled.value = value }
    fun setSoundEnabled(value: Boolean) { _soundEnabled.value = value }
    fun setSoundUri(uri: Uri?) { _soundUri.value = uri?.toString() }

    fun defaultSoundLabel(): String {
        val uri = _soundUri.value?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        return RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Default"
    }

    fun save() {
        if (!isValid.value) return
        viewModelScope.launch {
            val reminder = Reminder(
                id = existingId ?: 0,
                medicationId = medicationId,
                cronExpression = _cronExpression.value.trim(),
                isEnabled = _isEnabled.value,
                notificationTimeoutSeconds = _timeoutSeconds.value,
                autoDelayMinutes = _autoDelayMinutes.value,
                vibrationEnabled = _vibrationEnabled.value,
                soundEnabled = _soundEnabled.value,
                soundUri = _soundUri.value,
            )
            val savedId = reminderRepository.save(reminder)
            val saved = reminder.copy(id = if (existingId == null) savedId else reminder.id)
            if (saved.isEnabled) reminderScheduler.schedule(saved)
            else reminderScheduler.cancel(saved.id)
            _saved.value = true
        }
    }
}
