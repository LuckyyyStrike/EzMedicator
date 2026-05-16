package de.ingomohrmann.ezmedicator.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    val medicationName: String,
    val reminderId: Long,
    val delayMinutes: Int? = null,
) {
    companion object {
        const val TYPE_TRIGGERED = "TRIGGERED"
        const val TYPE_DISMISSED = "DISMISSED"
        const val TYPE_DELAYED_MANUAL = "DELAYED_MANUAL"
        const val TYPE_DELAYED_AUTO = "DELAYED_AUTO"
        const val TYPE_SKIPPED = "SKIPPED"
        const val TYPE_RESET = "RESET"
    }
}
