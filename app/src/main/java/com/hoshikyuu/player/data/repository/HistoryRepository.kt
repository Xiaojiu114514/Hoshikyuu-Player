package com.hoshikyuu.player.data.repository

import com.hoshikyuu.player.data.local.dao.HistoryDao
import com.hoshikyuu.player.data.local.entity.HistoryEntity
import com.hoshikyuu.player.domain.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {

    fun getAllHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun addHistory(song: Song) {
        historyDao.addHistory(
            HistoryEntity(
                songId = song.id,
                songName = song.name,
                songArtist = song.artist,
                songAlbum = song.album,
                songCoverUrl = song.coverUrl,
                songMp3Url = song.mp3Url,
                source = song.source,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeHistory(songId: String) {
        historyDao.removeHistoryById(songId)
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}