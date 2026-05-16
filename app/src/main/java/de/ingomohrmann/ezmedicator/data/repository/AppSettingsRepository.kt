package de.ingomohrmann.ezmedicator.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

fun formatDelayMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

@Singleton
class AppSettingsRepository @Inject constructor(
    private val prefs: SharedPreferences,
) {
    companion object {
        private const val KEY_DELAY_STEPS = "delay_steps"
        private const val KEY_DEFAULT_TIMEOUT_SECONDS = "default_timeout_seconds"
        private const val KEY_DEFAULT_AUTO_DELAY_MINUTES = "default_auto_delay_minutes"
        val DEFAULT_DELAY_STEPS = listOf(15, 30, 60, 120)
        const val DEFAULT_TIMEOUT_SECONDS = 300
        const val DEFAULT_AUTO_DELAY_MINUTES = 30
    }

    private val _delaySteps = MutableStateFlow(loadDelaySteps())
    val delaySteps: StateFlow<List<Int>> = _delaySteps.asStateFlow()

    private val _defaultTimeoutSeconds = MutableStateFlow(
        prefs.getInt(KEY_DEFAULT_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS)
    )
    val defaultTimeoutSeconds: StateFlow<Int> = _defaultTimeoutSeconds.asStateFlow()

    private val _defaultAutoDelayMinutes = MutableStateFlow(
        prefs.getInt(KEY_DEFAULT_AUTO_DELAY_MINUTES, DEFAULT_AUTO_DELAY_MINUTES)
    )
    val defaultAutoDelayMinutes: StateFlow<Int> = _defaultAutoDelayMinutes.asStateFlow()

    private fun loadDelaySteps(): List<Int> {
        val raw = prefs.getString(KEY_DELAY_STEPS, null) ?: return DEFAULT_DELAY_STEPS
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.ifEmpty { DEFAULT_DELAY_STEPS }
    }

    fun saveDelaySteps(steps: List<Int>) {
        prefs.edit().putString(KEY_DELAY_STEPS, steps.joinToString(",")).apply()
        _delaySteps.value = steps
    }

    fun saveDefaultTimeoutSeconds(value: Int) {
        prefs.edit().putInt(KEY_DEFAULT_TIMEOUT_SECONDS, value).apply()
        _defaultTimeoutSeconds.value = value
    }

    fun saveDefaultAutoDelayMinutes(value: Int) {
        prefs.edit().putInt(KEY_DEFAULT_AUTO_DELAY_MINUTES, value).apply()
        _defaultAutoDelayMinutes.value = value
    }
}
