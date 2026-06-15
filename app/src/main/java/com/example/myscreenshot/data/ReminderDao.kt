package com.example.myscreenshot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status != 'deleted' ORDER BY COALESCE(dateTime, createdAt) ASC")
    fun observeActive(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<Reminder?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: Reminder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlerts(alerts: List<ReminderAlert>)

    @Query("UPDATE reminders SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("UPDATE reminders SET status = 'deleted', updatedAt = :updatedAt WHERE status != 'deleted' AND dateTime IS NOT NULL AND dateTime < :now")
    suspend fun deletePastEvents(now: Long, updatedAt: Long): Int
}
