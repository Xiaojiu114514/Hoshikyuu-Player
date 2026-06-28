package com.hoshikyuu.player.ui.screens.home

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
            // 检查是否应该显示警告
            if (!networkPreferenceManager.shouldShowMobileWarning()) {
                // 用户已做出选择
                if (networkPreferenceManager.isMobileNetworkAllowed()) {
                    loadHomeData()
                } else {
                    loadCachedDataOnly()
                }
                return@launch
            }

            // 首次启动，检查网络类型
            val isMobileData = withContext(Dispatchers.IO) {
                networkUtils.isMobileData()
            }

            if (isMobileData) {
                // 移动网络，显示警告
                _showMobileDataWarning.value = true
            } else {
                // WiFi，直接加载
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
            // 重新加载数据
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
            // 只加载缓存数据
            loadCachedDataOnly()
        }
    }

    private fun loadCachedDataOnly() {
        viewModelScope.launch {
            // 热歌榜缓存
            val trendingCached = withContext(Dispatchers.IO) {
                cacheRepository.getCachedRanking("3778678")
            }
            if (trendingCached != null && trendingCached.isNotEmpty()) {
                _trendingSongs.value = UiState.Success(trendingCached)
            } else {
                _trendingSongs.value = UiState.Error("网络已禁用，且无本地缓存")
            }

            // 新歌榜缓存
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
        // 热歌榜
        loadRanking("3778678", "热歌榜", _trendingSongs)
        // 新歌榜
        loadRanking("3779629", "新歌榜", _recommendedSongs)
    }

    private fun loadRanking(
        rankingId: String,
        rankingName: String,
        stateFlow: MutableStateFlow<UiState<List<Song>>>
    ) {
        viewModelScope.launch {
            // 1. 先检查本地缓存是否有效（今日缓存）
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

            // 2. 检查是否允许网络请求
            if (!networkPreferenceManager.isMobileNetworkAllowed()) {
                // 网络被禁用，尝试加载旧缓存
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

            // 3. 从 API 获取
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