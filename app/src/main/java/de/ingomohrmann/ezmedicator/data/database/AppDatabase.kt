package de.ingomohrmann.ezmedicator.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import de.ingomohrmann.ezmedicator.data.database.dao.LogEntryDao
import de.ingomohrmann.ezmedicator.data.database.dao.MedicationDao
import de.ingomohrmann.ezmedicator.data.database.dao.ReminderDao
import de.ingomohrmann.ezmedicator.data.database.entities.LogEntry
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder

@Database(
    entities = [Medication::class, Reminder::class, LogEntry::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun reminderDao(): ReminderDao
    abstract fun logEntryDao(): LogEntryDao
}
