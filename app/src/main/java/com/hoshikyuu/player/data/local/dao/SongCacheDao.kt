package com.hoshikyuu.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hoshikyuu.player.data.local.entity.SongCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongCacheDao {
    @Query("SELECT * FROM song_cache WHERE songId = :songId")
    suspend fun getSong(songId: String): SongCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSong(song: SongCacheEntity)

    @Query("DELETE FROM song_cache WHERE songId = :songId")
    suspend fun deleteSong(songId: String)

    // 新增：清除所有缓存记录（用于清除缓存功能）
    @Query("DELETE FROM song_cache")
    suspend fun clearAllCache()
}