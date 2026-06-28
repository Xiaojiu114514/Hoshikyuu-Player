package com.hoshikyuu.player.data.repository

import com.hoshikyuu.player.data.local.dao.FavoriteDao
import com.hoshikyuu.player.data.local.entity.FavoriteEntity
import com.hoshikyuu.player.domain.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao
) {

    fun getAllFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    suspend fun addFavorite(song: Song) {
        favoriteDao.addFavorite(
            FavoriteEntity(
                songId = song.id,
                songName = song.name,
                songArtist = song.artist,
                songAlbum = song.album,
                songCoverUrl = song.coverUrl,
                songMp3Url = song.mp3Url,
                source = song.source
            )
        )
    }

    suspend fun removeFavorite(songId: String) {
        favoriteDao.removeFavoriteById(songId)
    }

    suspend fun isFavorite(songId: String): Boolean = favoriteDao.isFavorite(songId) > 0
}