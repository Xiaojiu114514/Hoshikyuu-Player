package com.hoshikyuu.player.data.repository

import com.hoshikyuu.player.data.local.dao.SongCacheDao
import com.hoshikyuu.player.data.local.entity.SongCacheEntity
import com.hoshikyuu.player.domain.Song
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongCacheRepository @Inject constructor(
    private val cacheDao: SongCacheDao
) {

    suspend fun getCachedSong(songId: String): Song? {
        val entity = cacheDao.getSong(songId) ?: return null
        return Song(
            id = entity.songId,
            name = entity.name,
            album = entity.album,
            artist = entity.artist,
            coverUrl = entity.coverUrl,
            mp3Url = entity.mp3Url,
            lrc = entity.lrc,
            source = entity.source
        )
    }

    suspend fun saveSong(song: Song) {
        cacheDao.saveSong(
            SongCacheEntity(
                songId = song.id,
                name = song.name,
                album = song.album,
                artist = song.artist,
                coverUrl = song.coverUrl,
                mp3Url = song.mp3Url,
                lrc = song.lrc,
                source = song.source
            )
        )
    }

    suspend fun updateMp3Url(songId: String, newMp3Url: String) {
        val entity = cacheDao.getSong(songId) ?: return
        cacheDao.saveSong(entity.copy(mp3Url = newMp3Url))
    }
}