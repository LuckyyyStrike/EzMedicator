package de.ingomohrmann.ezmedicator.ui.screens.log

import android.content.Context
import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ingomohrmann.ezmedicator.data.database.entities.LogEntry
import de.ingomohrmann.ezmedicator.data.repository.LogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logRepository: LogRepository,
) : ViewModel() {

    val entries: StateFlow<List<LogEntry>> = logRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clear() {
        viewModelScope.launch { logRepository.clear() }
    }

    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val datePart = DateFormat.getDateFormat(context).format(date)
        val timePart = DateFormat.getTimeFormat(context).format(date)
        return "$datePart $timePart"
    }
}
