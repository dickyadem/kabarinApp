package com.kabarinpacar.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kabarinpacar.app.data.model.StatusUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusUpdate): Long

    @Query("SELECT * FROM status_updates WHERE userId = :userId AND timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayStatuses(userId: String, startOfDay: Long): Flow<List<StatusUpdate>>

    @Query("SELECT * FROM status_updates WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestStatus(userId: String): StatusUpdate?

    @Query("DELETE FROM status_updates WHERE timestamp < :startOfDay")
    suspend fun deleteOldStatuses(startOfDay: Long)

    @Query("SELECT * FROM status_updates WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestStatusFlow(userId: String): Flow<StatusUpdate?>
}
