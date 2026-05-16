package de.ingomohrmann.ezmedicator.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import de.ingomohrmann.ezmedicator.data.database.dao.MedicationDao
import de.ingomohrmann.ezmedicator.data.database.dao.ReminderDao
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder

@Database(
    entities = [Medication::class, Reminder::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun reminderDao(): ReminderDao
}
