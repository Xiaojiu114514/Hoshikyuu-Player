package com.hoshikyuu.player.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshikyuu.player.data.repository.MusicRepository
import com.hoshikyuu.player.domain.Song
import com.hoshikyuu.player.domain.UiState
import com.hoshikyuu.player.player.DownloadManager
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.utils.NetworkPreferenceManager
import com.hoshikyuu.player.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerManager: PlayerManager,
    private val downloadManager: DownloadManager,
    private val networkPreferenceManager: NetworkPreferenceManager,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow<UiState<List<Song>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<Song>>> = _searchState.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _hotSearches = MutableStateFlow(listOf("周杰倫", "林俊傑", "五月天", "告五人", "鄧紫棋", "陳奕迅"))
    val hotSearches: StateFlow<List<String>> = _hotSearches.asStateFlow()

    fun onQueryChange(query: String) { _searchQuery.value = query }

    fun onSearch(query: String) {
        if (query.isBlank()) return
        
        // 检查网络是否被禁用
        if (!networkPreferenceManager.isMobileNetworkAllowed()) {
            _searchState.value = UiState.Error("网络已禁用，请连接WiFi后搜索")
            return
        }
        
        addToHistory(query.trim())
        doSearch(query.trim())
    }

    fun onHistoryClick(query: String) {
        _searchQuery.value = query
        // 检查网络是否被禁用
        if (!networkPreferenceManager.isMobileNetworkAllowed()) {
            _searchState.value = UiState.Error("网络已禁用，请连接WiFi后搜索")
            return
        }
        doSearch(query)
    }

    fun clearHistory() { _searchHistory.value = emptyList() }

    fun removeFromHistory(query: String) {
        _searchHistory.value = _searchHistory.value.filter { it != query }
    }

    private fun addToHistory(query: String) {
        val h = _searchHistory.value.toMutableList()
        h.remove(query); h.add(0, query)
        _searchHistory.value = h.take(10)
    }

    private fun doSearch(query: String) {
        viewModelScope.launch {
            _searchState.value = UiState.Loading
            repository.searchSongs(query)
                .onSuccess { songs ->
                    _searchState.value = UiState.Success(songs)
                }
                .onFailure { e ->
                    _searchState.value = UiState.Error(e.message ?: "搜索失敗")
                }
        }
    }

    fun addSongToQueueWithSong(song: Song): Boolean {
        if (playerManager.isSongInQueue(song.id)) {
            return false
        }
        playerManager.addToQueueAfterCurrent(song)
        return true
    }

    fun playSong(song: Song) {
        playerManager.play(song)
    }

    fun downloadSong(song: Song, onResult: (Result<File>) -> Unit = {}) {
        if (!networkPreferenceManager.isMobileNetworkAllowed()) {
            onResult(Result.failure(Exception("网络已禁用，请连接WiFi后下载")))
            return
        }
        viewModelScope.launch {
            val detailResult = withContext(Dispatchers.IO) {
                repository.fetchSongDetailForce(song.id, song.source)
            }
            if (detailResult.isSuccess) {
                val fullSong = detailResult.getOrNull()!!
                downloadManager.downloadSong(fullSong, onResult)
            } else {
                val error = detailResult.exceptionOrNull() ?: Exception("无法获取歌曲下载链接")
                onResult(Result.failure(error))
            }
        }
    }
}