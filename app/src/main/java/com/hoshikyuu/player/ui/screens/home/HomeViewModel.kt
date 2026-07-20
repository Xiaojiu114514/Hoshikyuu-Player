package com.hoshikyuu.player.ui.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshikyuu.player.data.repository.MusicRepository
import com.hoshikyuu.player.data.repository.RankingCacheRepository
import com.hoshikyuu.player.domain.Song
import com.hoshikyuu.player.domain.UiState
import com.hoshikyuu.player.player.DownloadManager
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.utils.NetworkPreferenceManager
import com.hoshikyuu.player.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerManager: PlayerManager,
    private val downloadManager: DownloadManager,
    private val cacheRepository: RankingCacheRepository,
    private val networkUtils: NetworkUtils,
    private val networkPreferenceManager: NetworkPreferenceManager
) : ViewModel() {

    companion object {
        val RANKING_CONFIG = mapOf(
            "3778678" to "热歌榜",
            "3779629" to "新歌榜"
        )
    }

    private val _trendingSongs = MutableStateFlow<UiState<List<Song>>>(UiState.Loading)
    val trendingSongs: StateFlow<UiState<List<Song>>> = _trendingSongs.asStateFlow()

    private val _recommendedSongs = MutableStateFlow<UiState<List<Song>>>(UiState.Loading)
    val recommendedSongs: StateFlow<UiState<List<Song>>> = _recommendedSongs.asStateFlow()

    private val _showMobileDataWarning = MutableStateFlow<Boolean>(false)
    val showMobileDataWarning: StateFlow<Boolean> = _showMobileDataWarning.asStateFlow()

    init {
        checkNetworkAndLoad()
    }

    private fun checkNetworkAndLoad() {
        viewModelScope.launch {
            if (!networkPreferenceManager.shouldShowMobileWarning()) {
                if (networkPreferenceManager.isMobileNetworkAllowed()) {
                    loadHomeData()
                } else {
                    loadCachedDataOnly()
                }
                return@launch
            }

            val isMobileData = withContext(Dispatchers.IO) {
                networkUtils.isMobileData()
            }

            if (isMobileData) {
                _showMobileDataWarning.value = true
            } else {
                networkPreferenceManager.setShowMobileWarning(false)
                networkPreferenceManager.setMobileNetworkAllowed(true)
                loadHomeData()
            }
        }
    }

    fun confirmMobileDataLoad() {
        viewModelScope.launch {
            _showMobileDataWarning.value = false
            networkPreferenceManager.apply {
                setMobileNetworkAllowed(true)
                setShowMobileWarning(false)
            }
            loadHomeData()
        }
    }

    fun cancelMobileDataLoad() {
        viewModelScope.launch {
            _showMobileDataWarning.value = false
            networkPreferenceManager.apply {
                setMobileNetworkAllowed(false)
                setShowMobileWarning(false)
            }
            loadCachedDataOnly()
        }
    }

    private fun loadCachedDataOnly() {
        viewModelScope.launch {
            val trendingCached = withContext(Dispatchers.IO) {
                cacheRepository.getCachedRanking("3778678")
            }
            if (trendingCached != null && trendingCached.isNotEmpty()) {
                _trendingSongs.value = UiState.Success(trendingCached)
            } else {
                _trendingSongs.value = UiState.Error("网络已禁用，且无本地缓存")
            }

            val recommendedCached = withContext(Dispatchers.IO) {
                cacheRepository.getCachedRanking("3779629")
            }
            if (recommendedCached != null && recommendedCached.isNotEmpty()) {
                _recommendedSongs.value = UiState.Success(recommendedCached)
            } else {
                _recommendedSongs.value = UiState.Error("网络已禁用，且无本地缓存")
            }
        }
    }

    fun loadHomeData() {
        loadRanking("3778678", "热歌榜", _trendingSongs)
        loadRanking("3779629", "新歌榜", _recommendedSongs)
    }

    private fun loadRanking(
        rankingId: String,
        rankingName: String,
        stateFlow: MutableStateFlow<UiState<List<Song>>>
    ) {
        viewModelScope.launch {
            val isValid = withContext(Dispatchers.IO) {
                cacheRepository.isValidCache(rankingId)
            }

            if (isValid) {
                val cachedSongs = withContext(Dispatchers.IO) {
                    cacheRepository.getCachedRanking(rankingId)
                }
                if (cachedSongs != null && cachedSongs.isNotEmpty()) {
                    stateFlow.value = UiState.Success(cachedSongs)
                    return@launch
                }
            }

            if (!networkPreferenceManager.isMobileNetworkAllowed()) {
                val cachedSongs = withContext(Dispatchers.IO) {
                    cacheRepository.getCachedRanking(rankingId)
                }
                if (cachedSongs != null && cachedSongs.isNotEmpty()) {
                    stateFlow.value = UiState.Success(cachedSongs)
                } else {
                    stateFlow.value = UiState.Error("网络已禁用，请连接WiFi后重试")
                }
                return@launch
            }

            fetchRankingFromApi(rankingId, rankingName, stateFlow)
        }
    }

    private suspend fun fetchRankingFromApi(
        rankingId: String,
        rankingName: String,
        stateFlow: MutableStateFlow<UiState<List<Song>>>
    ) {
        stateFlow.value = UiState.Loading
        try {
            val result = withContext(Dispatchers.IO) {
                repository.getRanking(rankingId, "wy")
            }
            result.onSuccess { songs ->
                withContext(Dispatchers.IO) {
                    cacheRepository.saveRanking(rankingId, rankingName, songs)
                }
                stateFlow.value = UiState.Success(songs)
            }.onFailure { e ->
                val cachedSongs = withContext(Dispatchers.IO) {
                    cacheRepository.getCachedRanking(rankingId)
                }
                if (cachedSongs != null && cachedSongs.isNotEmpty()) {
                    stateFlow.value = UiState.Success(cachedSongs)
                } else {
                    stateFlow.value = UiState.Error(e.message ?: "加载失败")
                }
            }
        } catch (e: Exception) {
            val cachedSongs = withContext(Dispatchers.IO) {
                cacheRepository.getCachedRanking(rankingId)
            }
            if (cachedSongs != null && cachedSongs.isNotEmpty()) {
                stateFlow.value = UiState.Success(cachedSongs)
            } else {
                stateFlow.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun playSong(song: Song) {
        playerManager.play(song)
    }

    fun addSongToQueue(song: Song): Boolean {
        if (playerManager.isSongInQueue(song.id)) {
            return false
        }
        playerManager.addToQueueAfterCurrent(song)
        return true
    }

    // 下载歌曲 - 返回 Uri
    fun downloadSong(song: Song, onResult: (Result<Uri>) -> Unit = {}) {
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