package de.ingomohrmann.ezmedicator.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("medicationId")],
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val cronExpression: String,
    val isEnabled: Boolean = true,

    // Notification settings
    val notificationTimeoutSeconds: Int = 300,
    val autoDelayMinutes: Int = 30,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundUri: String? = null,

    // Scheduling state
    val skipNextOccurrence: Boolean = false,
    val snoozedUntil: Long? = null,
    val delayedByMinutes: Int? = null,
)
