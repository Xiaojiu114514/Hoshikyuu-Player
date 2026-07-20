package com.hoshikyuu.player.ui.screens.playlist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshikyuu.player.data.local.entity.PlaylistEntity
import com.hoshikyuu.player.data.local.entity.PlaylistSongEntity
import com.hoshikyuu.player.data.repository.MusicRepository
import com.hoshikyuu.player.data.repository.PlaylistRepository
import com.hoshikyuu.player.domain.Song
import com.hoshikyuu.player.player.DownloadManager
import com.hoshikyuu.player.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val musicRepository: MusicRepository,
    private val playerManager: PlayerManager,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch { repository.deletePlaylist(playlist) }
    }

    fun getSongs(playlistId: Long): StateFlow<List<PlaylistSongEntity>> {
        return repository.getPlaylistSongs(playlistId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    suspend fun addCurrentSongToPlaylist(playlistId: Long): Boolean {
        val song = playerManager.currentSong.value ?: return false
        return repository.addSongToPlaylist(playlistId, song)
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: Song): Boolean {
        return repository.addSongToPlaylist(playlistId, song)
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch { repository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun playSongWithDetail(song: Song) = playerManager.play(song)

    fun addSongToQueueWithDetail(song: Song): Boolean {
        if (playerManager.isSongInQueue(song.id)) return false
        playerManager.addToQueueAfterCurrent(song)
        return true
    }

    fun downloadSong(song: Song, onResult: (Result<Uri>) -> Unit = {}) {
        viewModelScope.launch {
            val detail = musicRepository.fetchSongDetailForce(song.id, song.source)
            if (detail.isSuccess) {
                downloadManager.downloadSong(detail.getOrNull()!!, onResult)
            } else {
                onResult(Result.failure(detail.exceptionOrNull() ?: Exception("无法获取歌曲信息")))
            }
        }
    }
}