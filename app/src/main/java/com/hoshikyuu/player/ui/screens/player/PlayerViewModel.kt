package com.hoshikyuu.player.ui.screens.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshikyuu.player.domain.Song
import com.hoshikyuu.player.domain.UiState
import com.hoshikyuu.player.player.DownloadManager
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.player.RepeatMode
import com.hoshikyuu.player.ui.utils.LyricLine
import com.hoshikyuu.player.ui.utils.parseLyrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playerManager: PlayerManager,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val songState: StateFlow<UiState<Song>> = playerManager.currentSong
        .map { song -> if (song != null) UiState.Success(song) else UiState.Idle }
        .stateIn(viewModelScope, SharingStarted.Eagerly,
            playerManager.currentSong.value?.let { UiState.Success(it) } ?: UiState.Idle)

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val progress: StateFlow<Float> = playerManager.progress
    val duration: StateFlow<Long> = playerManager.duration

    private val _repeatMode = MutableStateFlow(playerManager.repeatMode.value)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = playerManager.favoriteIds
    val queue: StateFlow<List<Song>> = playerManager.queue
    val currentIndex: StateFlow<Int> = playerManager.currentIndex

    val lyricLines: StateFlow<List<LyricLine>> = playerManager.currentSong
        .map { song -> if (song != null) parseLyrics(song.lrc) else emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var progressJob: Job? = null

    init {
        viewModelScope.launch {
            playerManager.repeatMode.collect { mode -> _repeatMode.value = mode }
        }
        viewModelScope.launch {
            playerManager.isPlaying.collect { playing ->
                if (playing) startProgressTracker() else stopProgressTracker()
            }
        }
    }

    fun togglePlay() = playerManager.togglePlay()
    fun toggleFavorite(song: Song) = playerManager.toggleFavorite(song)
    fun seekTo(fraction: Float) = playerManager.seekTo(fraction)
    fun skipNext() = playerManager.skipNext()
    fun skipPrevious() = playerManager.skipPrevious()
    fun addCurrentToQueue() {
        playerManager.currentSong.value?.let { playerManager.addToQueueAfterCurrent(it) }
    }

    fun getLyricIndex(positionMs: Long): Int {
        val lines = lyricLines.value
        if (lines.isEmpty()) return -1
        val idx = lines.indexOfLast { it.timestampMs <= positionMs }
        return if (idx < 0) 0 else idx
    }

    fun downloadCurrentSong(onResult: (Result<Uri>) -> Unit = {}) {
        val song = playerManager.currentSong.value
        if (song == null) {
            onResult(Result.failure(Exception("当前没有播放歌曲")))
            return
        }
        viewModelScope.launch {
            if (playerManager.isSongDownloaded(song.id)) {
                onResult(Result.failure(Exception("歌曲已下载")))
                return@launch
            }
            downloadManager.downloadSong(song, onResult)
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(200)
                playerManager.updateProgress()
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}