package com.hoshikyuu.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hoshikyuu.player.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY playedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE songId = :songId")
    suspend fun removeHistoryById(songId: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}