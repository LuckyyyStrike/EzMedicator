package de.ingomohrmann.ezmedicator.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.ingomohrmann.ezmedicator.data.repository.AppSettingsRepository
import de.ingomohrmann.ezmedicator.data.repository.LogRepository
import de.ingomohrmann.ezmedicator.data.repository.MedicationRepository
import de.ingomohrmann.ezmedicator.data.repository.ReminderRepository
import de.ingomohrmann.ezmedicator.domain.ReminderScheduler

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun reminderRepository(): ReminderRepository
    fun medicationRepository(): MedicationRepository
    fun reminderScheduler(): ReminderScheduler
    fun settingsRepository(): AppSettingsRepository
    fun logRepository(): LogRepository
}
