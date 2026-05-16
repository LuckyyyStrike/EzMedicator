package de.ingomohrmann.ezmedicator.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import de.ingomohrmann.ezmedicator.data.database.entities.LogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Insert
    suspend fun insert(entry: LogEntry)

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<LogEntry>>

    @Query("DELETE FROM log_entries")
    suspend fun clearAll()
}
