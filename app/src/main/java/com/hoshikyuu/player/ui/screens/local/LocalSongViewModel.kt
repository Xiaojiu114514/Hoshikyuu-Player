package com.hoshikyuu.player.ui.screens.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshikyuu.player.data.repository.LocalSongRepository
import com.hoshikyuu.player.domain.Song
import com.hoshikyuu.player.domain.UiState
import com.hoshikyuu.player.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalSongViewModel @Inject constructor(
    private val localSongRepository: LocalSongRepository,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _songsState = MutableStateFlow<UiState<List<Song>>>(UiState.Loading)
    val songsState: StateFlow<UiState<List<Song>>> = _songsState.asStateFlow()

    fun loadLocalSongs() {
        viewModelScope.launch {
            _songsState.value = UiState.Loading
            try {
                val songs = localSongRepository.scanLocalSongs()
                _songsState.value = if (songs.isNotEmpty()) {
                    UiState.Success(songs)
                } else {
                    UiState.Error("未找到本地歌曲，请将 mp3 文件放入 /download/HoshikyuuPlayer/")
                }
            } catch (e: Exception) {
                _songsState.value = UiState.Error(e.message ?: "扫描本地歌曲失败")
            }
        }
    }

    fun playSong(song: Song) {
        playerManager.play(song)
    }

    fun addToQueue(song: Song) {
        playerManager.addToQueue(song)
    }
}