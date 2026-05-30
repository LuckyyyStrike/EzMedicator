package de.ingomohrmann.ezmedicator.data.repository

import de.ingomohrmann.ezmedicator.data.database.dao.ReminderDao
import de.ingomohrmann.ezmedicator.data.database.entities.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val dao: ReminderDao,
) {
    fun observeAll(): Flow<List<Reminder>> = dao.observeAll()

    fun observeForMedication(medicationId: Long): Flow<List<Reminder>> =
        dao.observeForMedication(medicationId)

    /** Returns a map of medicationId → reminder count, live-updating. */
    fun observeCountsByMedication(): Flow<Map<Long, Int>> =
        dao.observeCountsByMedication().map { list -> list.associate { it.medicationId to it.count } }

    suspend fun getById(id: Long): Reminder? = dao.getById(id)

    suspend fun getAllEnabled(): List<Reminder> = dao.getAllEnabled()

    suspend fun save(reminder: Reminder): Long = dao.insert(reminder)

    suspend fun delete(reminder: Reminder) = dao.delete(reminder)

    suspend fun setSkipNext(id: Long, skip: Boolean) = dao.setSkipNext(id, skip)

    suspend fun setSnoozedUntil(id: Long, until: Long?) = dao.setSnoozedUntil(id, until)

    suspend fun setSnoozeState(id: Long, until: Long, minutes: Int) = dao.setSnoozeState(id, until, minutes)

    suspend fun clearDelayState(id: Long) = dao.clearDelayState(id)
}
