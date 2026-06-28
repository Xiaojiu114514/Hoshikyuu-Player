package com.hoshikyuu.player.data.repository

import com.hoshikyuu.player.data.local.dao.PlaylistDao
import com.hoshikyuu.player.data.local.entity.PlaylistEntity
import com.hoshikyuu.player.data.local.entity.PlaylistSongEntity
import com.hoshikyuu.player.domain.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.createPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        playlistDao.deletePlaylist(playlist)
    }

    fun getPlaylistSongs(playlistId: Long): Flow<List<PlaylistSongEntity>> =
        playlistDao.getPlaylistSongs(playlistId)

    suspend fun addSongToPlaylist(playlistId: Long, song: Song): Boolean {
        // 检测是否已存在
        val exists = playlistDao.isSongInPlaylist(playlistId, song.id) > 0
        if (exists) return false
        playlistDao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                songName = song.name,
                songArtist = song.artist,
                songAlbum = song.album,
                songCoverUrl = song.coverUrl,
                songMp3Url = song.mp3Url,
                source = song.source
            )
        )
        return true
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun isSongInPlaylist(playlistId: Long, songId: String): Boolean {
        return playlistDao.isSongInPlaylist(playlistId, songId) > 0
    }
}