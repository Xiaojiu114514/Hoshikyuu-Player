package com.hoshikyuu.player.data.repository

import com.hoshikyuu.player.data.local.dao.RankingCacheDao
import com.hoshikyuu.player.data.local.entity.RankingCacheEntity
import com.hoshikyuu.player.data.local.entity.RankingCacheSerializer
import com.hoshikyuu.player.domain.Song
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RankingCacheRepository @Inject constructor(
    private val dao: RankingCacheDao
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDate(): String = dateFormat.format(Date())

    suspend fun isValidCache(rankingId: String): Boolean {
        val cache = dao.getCache(rankingId)
        return cache != null && cache.cachedDate == getTodayDate()
    }

    suspend fun getCachedRanking(rankingId: String): List<Song>? {
        val cache = dao.getCache(rankingId)
        return cache?.songsJson?.let { RankingCacheSerializer.jsonToSongs(it) }
    }

    suspend fun saveRanking(rankingId: String, rankingName: String, songs: List<Song>) {
        val entity = RankingCacheEntity(
            rankingId = rankingId,
            rankingName = rankingName,
            songsJson = RankingCacheSerializer.songsToJson(songs),
            cachedDate = getTodayDate()
        )
        dao.saveCache(entity)
    }

    suspend fun clearRankingCache(rankingId: String) {
        dao.deleteCache(rankingId)
    }
}