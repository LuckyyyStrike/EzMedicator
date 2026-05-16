package de.ingomohrmann.ezmedicator.data.repository

import de.ingomohrmann.ezmedicator.data.database.dao.LogEntryDao
import de.ingomohrmann.ezmedicator.data.database.entities.LogEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(private val dao: LogEntryDao) {
    fun observeAll(): Flow<List<LogEntry>> = dao.observeAll()
    suspend fun log(entry: LogEntry) = dao.insert(entry)
    suspend fun clear() = dao.clearAll()
}
