package de.ingomohrmann.ezmedicator.ui.screens.medications

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.data.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MedicationEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationRepository: MedicationRepository,
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _imagePath = MutableStateFlow<String?>(null)
    val imagePath: StateFlow<String?> = _imagePath.asStateFlow()

    private val _iconName = MutableStateFlow<String?>(null)
    val iconName: StateFlow<String?> = _iconName.asStateFlow()

    private val _iconColor = MutableStateFlow<Int?>(null)
    val iconColor: StateFlow<Int?> = _iconColor.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private var existingId: Long? = null

    fun load(medicationId: Long?) {
        if (medicationId == null) return
        viewModelScope.launch {
            val med = medicationRepository.getById(medicationId) ?: return@launch
            existingId = med.id
            _title.value = med.title
            _imagePath.value = med.imagePath
            _iconName.value = med.iconName
            _iconColor.value = med.iconColor
        }
    }

    fun setTitle(value: String) { _title.value = value }

    fun setImage(uri: Uri) {
        viewModelScope.launch {
            val destDir = File(context.filesDir, "medication_images").also { it.mkdirs() }
            val dest = File(destDir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            _imagePath.value?.let { old -> File(old).takeIf { it.exists() }?.delete() }
            _imagePath.value = dest.absolutePath
            _iconName.value = null
            _iconColor.value = null
        }
    }

    fun setIcon(name: String, color: Int) {
        _imagePath.value?.let { old -> File(old).takeIf { it.exists() }?.delete() }
        _imagePath.value = null
        _iconName.value = name
        _iconColor.value = color
    }

    fun removeImage() {
        _imagePath.value?.let { old -> File(old).takeIf { it.exists() }?.delete() }
        _imagePath.value = null
        _iconName.value = null
        _iconColor.value = null
    }

    fun save() {
        val title = _title.value.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            val medication = Medication(
                id = existingId ?: 0,
                title = title,
                imagePath = _imagePath.value,
                iconName = _iconName.value,
                iconColor = _iconColor.value,
            )
            medicationRepository.save(medication)
            _saved.value = true
        }
    }
}
