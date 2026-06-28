package com.hoshikyuu.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hoshikyuu.player.data.local.entity.RankingCacheEntity

@Dao
interface RankingCacheDao {
    @Query("SELECT * FROM ranking_cache WHERE rankingId = :rankingId")
    suspend fun getCache(rankingId: String): RankingCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCache(cache: RankingCacheEntity)

    @Query("DELETE FROM ranking_cache WHERE rankingId = :rankingId")
    suspend fun deleteCache(rankingId: String)
}