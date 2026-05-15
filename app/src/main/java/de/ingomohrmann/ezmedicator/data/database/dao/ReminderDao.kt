package de.ingomohrmann.ezmedicator.data.database.dao

import androidx.room.*
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import kotlinx.coroutines.flow.Flow

data class ReminderCount(val medicationId: Long, val count: Int)

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE medicationId = :medicationId ORDER BY cronExpression ASC")
    fun observeForMedication(medicationId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getAllEnabled(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("SELECT medicationId, COUNT(*) as count FROM reminders GROUP BY medicationId")
    fun observeCountsByMedication(): Flow<List<ReminderCount>>

    @Query("UPDATE reminders SET skipNextOccurrence = :skip WHERE id = :id")
    suspend fun setSkipNext(id: Long, skip: Boolean)

    @Query("UPDATE reminders SET snoozedUntil = :until WHERE id = :id")
    suspend fun setSnoozedUntil(id: Long, until: Long?)
}
