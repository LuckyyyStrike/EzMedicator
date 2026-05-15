package de.ingomohrmann.ezmedicator.data.repository

import de.ingomohrmann.ezmedicator.data.database.dao.MedicationDao
import de.ingomohrmann.ezmedicator.data.database.entities.Medication
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val dao: MedicationDao,
) {
    fun observeAll(): Flow<List<Medication>> = dao.observeAll()

    suspend fun getById(id: Long): Medication? = dao.getById(id)

    suspend fun save(medication: Medication): Long = dao.insert(medication)

    suspend fun delete(medication: Medication) = dao.delete(medication)
}
