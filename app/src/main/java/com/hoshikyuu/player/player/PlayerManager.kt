package com.hoshikyuu.player.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hoshikyuu.player.data.repository.*
import com.hoshikyuu.player.domain.Song
import com.hoshikyuu.player.utils.NetworkPreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class RepeatMode { NONE, ONE, ALL }

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    private val musicRepository: MusicRepository,
    private val downloadRepository: DownloadRepository,
    private val cacheRepository: SongCacheRepository,
    private val downloadManager: DownloadManager,
    private val networkPreferenceManager: NetworkPreferenceManager
) {

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build()
    }

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs.asStateFlow()

    private val _playHistory = MutableStateFlow<List<Song>>(emptyList())
    val playHistory: StateFlow<List<Song>> = _playHistory.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun isSongInQueue(songId: String): Boolean = _queue.value.any { it.id == songId }

    suspend fun isSongDownloaded(songId: String): Boolean =
        downloadRepository.isDownloaded(songId)

    fun isNetworkBlocked(): Boolean = networkPreferenceManager.isNetworkBlocked()

    init {
        player.repeatMode = Player.REPEAT_MODE_ALL

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId
                val idx = _queue.value.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    _currentSong.value = _queue.value[idx]
                    _currentIndex.value = idx
                    _queue.value.getOrNull(idx)?.let { addToHistory(it) }
                } else {
                    val uri = mediaItem?.localConfiguration?.uri?.toString()
                    val fallbackIdx = _queue.value.indexOfFirst { it.mp3Url == uri }
                    if (fallbackIdx >= 0) {
                        _currentSong.value = _queue.value[fallbackIdx]
                        _currentIndex.value = fallbackIdx
                        _queue.value.getOrNull(fallbackIdx)?.let { addToHistory(it) }
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _errorMessage.value = "播放出错，请稍后重试"
            }
        })

        scope.launch(Dispatchers.IO) {
            favoriteRepository.getAllFavorites().collect { favorites ->
                val songs = favorites.map { entity ->
                    Song(
                        id = entity.songId,
                        name = entity.songName,
                        album = entity.songAlbum,
                        artist = entity.songArtist,
                        coverUrl = entity.songCoverUrl,
                        mp3Url = entity.songMp3Url,
                        source = entity.source
                    )
                }
                _favoriteSongs.value = songs
                _favoriteIds.value = songs.map { it.id }.toSet()
            }
        }
        scope.launch(Dispatchers.IO) {
            historyRepository.getAllHistory().collect { historyEntities ->
                val songs = historyEntities.map { entity ->
                    Song(
                        id = entity.songId,
                        name = entity.songName,
                        album = entity.songAlbum,
                        artist = entity.songArtist,
                        coverUrl = entity.songCoverUrl,
                        mp3Url = entity.songMp3Url,
                        source = entity.source
                    )
                }
                _playHistory.value = songs
            }
        }
    }

    // ---------- 核心播放入口 ----------

    fun play(song: Song) {
        scope.launch {
            // 检查是否为本地文件
            val isLocal = song.mp3Url.startsWith("/") || song.mp3Url.startsWith("file://")
            if (!isLocal && networkPreferenceManager.isNetworkBlocked()) {
                _errorMessage.value = "网络已禁用，无法播放在线歌曲"
                return@launch
            }

            val local = withContext(Dispatchers.IO) { getLocalSong(song.id) }
            if (local != null) {
                _queue.value = listOf(local)
                _currentIndex.value = 0
                playInternalDirect(local)
                return@launch
            }

            val fullSong = withContext(Dispatchers.IO) { fetchSongDetailForce(song) }
            if (fullSong == null) {
                _errorMessage.value = "获取歌曲信息失败"
                return@launch
            }

            // 异步缓存（使用已有 fullSong，不再调用 API）
            withContext(Dispatchers.IO) {
                cacheSongForOffline(fullSong)
            }

            _queue.value = listOf(fullSong)
            _currentIndex.value = 0
            playInternal(fullSong)
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        scope.launch {
            _queue.value = songs
            val target = songs.getOrNull(startIndex) ?: return@launch

            val isLocal = target.mp3Url.startsWith("/") || target.mp3Url.startsWith("file://")
            if (!isLocal && networkPreferenceManager.isNetworkBlocked()) {
                _errorMessage.value = "网络已禁用，无法播放在线歌曲"
                return@launch
            }

            val local = withContext(Dispatchers.IO) { getLocalSong(target.id) }
            if (local != null) {
                val newQueue = _queue.value.toMutableList()
                newQueue[startIndex] = local
                _queue.value = newQueue
                _currentIndex.value = startIndex
                playInternalDirect(local)
                return@launch
            }

            val fullSong = withContext(Dispatchers.IO) { fetchSongDetailForce(target) }
            if (fullSong == null) {
                _errorMessage.value = "获取歌曲信息失败"
                return@launch
            }

            withContext(Dispatchers.IO) {
                cacheSongForOffline(fullSong)
            }

            val newQueue = _queue.value.toMutableList()
            newQueue[startIndex] = fullSong
            _queue.value = newQueue
            _currentIndex.value = startIndex
            playInternal(fullSong)
        }
    }

    fun addToQueueAfterCurrent(song: Song) {
        scope.launch {
            val isLocal = song.mp3Url.startsWith("/") || song.mp3Url.startsWith("file://")
            if (!isLocal && networkPreferenceManager.isNetworkBlocked()) {
                _errorMessage.value = "网络已禁用，无法添加在线歌曲"
                return@launch
            }

            val local = withContext(Dispatchers.IO) { getLocalSong(song.id) }
            if (local != null) {
                addToQueueInternal(local, afterCurrent = true)
                return@launch
            }

            val fullSong = withContext(Dispatchers.IO) { fetchSongDetailForce(song) }
            if (fullSong == null) {
                _errorMessage.value = "添加失败，无法获取歌曲信息"
                return@launch
            }

            withContext(Dispatchers.IO) {
                cacheSongForOffline(fullSong)
            }
            addToQueueInternal(fullSong, afterCurrent = true)
        }
    }

    fun addToQueue(song: Song) {
        scope.launch {
            val isLocal = song.mp3Url.startsWith("/") || song.mp3Url.startsWith("file://")
            if (!isLocal && networkPreferenceManager.isNetworkBlocked()) {
                _errorMessage.value = "网络已禁用，无法添加在线歌曲"
                return@launch
            }

            val local = withContext(Dispatchers.IO) { getLocalSong(song.id) }
            if (local != null) {
                addToQueueInternal(local, afterCurrent = false)
                return@launch
            }

            val fullSong = withContext(Dispatchers.IO) { fetchSongDetailForce(song) }
            if (fullSong == null) {
                _errorMessage.value = "添加失败，无法获取歌曲信息"
                return@launch
            }

            withContext(Dispatchers.IO) {
                cacheSongForOffline(fullSong)
            }
            addToQueueInternal(fullSong, afterCurrent = false)
        }
    }

    private fun addToQueueInternal(song: Song, afterCurrent: Boolean) {
        val q = _queue.value.toMutableList()
        if (q.isEmpty()) {
            _queue.value = listOf(song)
            _currentIndex.value = 0
            playInternalDirect(song)
            return
        }
        val insertIdx = if (afterCurrent && _currentIndex.value >= 0 && _currentIndex.value < q.size) {
            _currentIndex.value + 1
        } else {
            q.size
        }.coerceIn(0, q.size)
        q.add(insertIdx, song)
        _queue.value = q
        if (_currentIndex.value >= insertIdx) {
            _currentIndex.value = _currentIndex.value + 1
        }
        player.addMediaItem(
            insertIdx,
            MediaItem.Builder()
                .setUri(song.mp3Url)
                .setMediaId(song.id)
                .build()
        )
    }

    fun removeFromQueue(index: Int) {
        val q = _queue.value.toMutableList()
        if (index !in q.indices) return

        val isCurrent = (index == _currentIndex.value)
        q.removeAt(index)
        _queue.value = q

        if (isCurrent) {
            if (q.isEmpty()) {
                player.stop()
                _currentSong.value = null
                _currentIndex.value = -1
            } else {
                val nextIdx = if (index < q.size) index else q.size - 1
                scope.launch {
                    val nextSong = q[nextIdx]
                    val local = withContext(Dispatchers.IO) { getLocalSong(nextSong.id) }
                    if (local != null) {
                        val newQ = _queue.value.toMutableList()
                        newQ[nextIdx] = local
                        _queue.value = newQ
                        _currentIndex.value = nextIdx
                        playInternalDirect(local)
                    } else {
                        val fullSong = withContext(Dispatchers.IO) { fetchSongDetailForce(nextSong) }
                        if (fullSong == null) {
                            _errorMessage.value = "获取歌曲信息失败"
                            return@launch
                        }
                        withContext(Dispatchers.IO) {
                            cacheSongForOffline(fullSong)
                        }
                        val newQ = _queue.value.toMutableList()
                        newQ[nextIdx] = fullSong
                        _queue.value = newQ
                        _currentIndex.value = nextIdx
                        playInternal(fullSong)
                    }
                }
            }
        } else {
            if (index < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            }
            if (index < player.mediaItemCount) {
                player.removeMediaItem(index)
            }
        }
    }

    // ---------- 播放控制 ----------

    fun togglePlay() {
        if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun seekTo(fraction: Float) {
        val pos = (fraction * _duration.value).toLong()
        player.seekTo(pos)
        _progress.value = fraction
    }

    fun skipNext() {
        if (_queue.value.isEmpty()) return
        val nextIdx = (_currentIndex.value + 1) % _queue.value.size
        playSongAtIndex(nextIdx)
    }

    fun skipPrevious() {
        if (_queue.value.isEmpty()) return
        val prevIdx = if (_currentIndex.value <= 0) _queue.value.size - 1 else _currentIndex.value - 1
        playSongAtIndex(prevIdx)
    }

    fun skipToIndex(index: Int) {
        if (index < 0 || index >= _queue.value.size) return
        playSongAtIndex(index)
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        player.repeatMode = when (mode) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    fun toggleFavorite(song: Song) {
        scope.launch(Dispatchers.IO) {
            val currentFavorites = _favoriteSongs.value.toMutableList()
            val currentIds = _favoriteIds.value.toMutableSet()
            if (song.id in currentIds) {
                favoriteRepository.removeFavorite(song.id)
                currentFavorites.removeAll { it.id == song.id }
                currentIds.remove(song.id)
            } else {
                favoriteRepository.addFavorite(song)
                currentFavorites.add(0, song)
                currentIds.add(song.id)
            }
            withContext(Dispatchers.Main) {
                _favoriteSongs.value = currentFavorites
                _favoriteIds.value = currentIds
            }
        }
    }

    fun isFavorite(songId: String): Boolean = songId in _favoriteIds.value

    fun updateProgress() {
        val dur = player.duration.coerceAtLeast(0)
        if (dur > 0) {
            _duration.value = dur
            _progress.value = (player.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
        }
    }

    fun release() {
        player.release()
    }

    // ---------- 核心内部方法 ----------

    private fun playSongAtIndex(index: Int) {
        if (index < 0 || index >= _queue.value.size) return
        val song = _queue.value[index]
        scope.launch {
            val isLocal = song.mp3Url.startsWith("/") || song.mp3Url.startsWith("file://")
            if (!isLocal && networkPreferenceManager.isNetworkBlocked()) {
                _errorMessage.value = "网络已禁用，无法播放在线歌曲"
                return@launch
            }

            val local = withContext(Dispatchers.IO) { getLocalSong(song.id) }
            if (local != null) {
                val newQueue = _queue.value.toMutableList()
                newQueue[index] = local
                _queue.value = newQueue
                _currentIndex.value = index
                playInternalDirect(local)
                return@launch
            }

            val fullSong = withContext(Dispatchers.IO) { fetchSongDetailForce(song) }
            if (fullSong == null) {
                _errorMessage.value = "获取歌曲信息失败"
                return@launch
            }

            withContext(Dispatchers.IO) {
                cacheSongForOffline(fullSong)
            }

            val newQueue = _queue.value.toMutableList()
            newQueue[index] = fullSong
            _queue.value = newQueue
            _currentIndex.value = index
            playInternal(fullSong)
        }
    }

    private fun playInternal(song: Song) {
        if (song.mp3Url.isEmpty()) {
            _errorMessage.value = "歌曲播放链接无效，请尝试其他源"
            return
        }
        _currentSong.value = song
        addToHistory(song)
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(song.mp3Url)
                .setMediaId(song.id)
                .build()
        )
        player.prepare()
        player.playWhenReady = true
        _errorMessage.value = null
    }

    private fun playInternalDirect(song: Song) {
        if (song.mp3Url.isEmpty()) {
            _errorMessage.value = "歌曲文件无效"
            return
        }
        _currentSong.value = song
        addToHistory(song)
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(song.mp3Url)
                .setMediaId(song.id)
                .build()
        )
        player.prepare()
        player.playWhenReady = true
        _errorMessage.value = null
    }

    private suspend fun getLocalSong(songId: String): Song? {
        val download = downloadRepository.getDownload(songId)
        if (download != null) {
            val file = File(download.localFilePath)
            if (file.exists()) {
                return Song(
                    id = download.songId,
                    name = download.songName,
                    album = download.album,
                    artist = download.artist,
                    coverUrl = download.coverUrl,
                    mp3Url = download.localFilePath,
                    lrc = download.lrc,
                    source = download.source
                )
            } else {
                downloadRepository.removeDownload(songId)
            }
        }

        val cachedFile = downloadManager.getCachedFile(songId)
        if (cachedFile != null && cachedFile.exists()) {
            val cached = cacheRepository.getCachedSong(songId)
            if (cached != null) {
                return cached.copy(mp3Url = cachedFile.absolutePath)
            } else {
                return Song(
                    id = songId,
                    name = "未知歌曲",
                    album = "",
                    artist = "未知歌手",
                    mp3Url = cachedFile.absolutePath,
                    source = "wy"
                )
            }
        }

        return null
    }

    private suspend fun fetchSongDetailForce(song: Song): Song? {
        return try {
            val result = musicRepository.fetchSongDetailForce(song.id, song.source)
            if (result.isSuccess) result.getOrNull() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 缓存歌曲到本地（使用已有的 fullSong，不再调用 API）
     */
    private suspend fun cacheSongForOffline(fullSong: Song) {
        try {
            // 检查是否已缓存
            val cached = cacheRepository.getCachedSong(fullSong.id)
            if (cached != null) {
                val file = File(cached.mp3Url)
                if (file.exists()) {
                    return
                }
            }

            // 检查是否已下载
            val download = downloadRepository.getDownload(fullSong.id)
            if (download != null && File(download.localFilePath).exists()) {
                return
            }

            // 使用已有完整信息缓存（不再调用 API）
            downloadManager.cacheSongWithFullInfo(fullSong) { result ->
                if (result.isSuccess) {
                    val file = result.getOrNull()!!
                    scope.launch(Dispatchers.IO) {
                        cacheRepository.saveSong(
                            fullSong.copy(mp3Url = file.absolutePath)
                        )
                    }
                }
                // 缓存失败静默处理
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addToHistory(song: Song) {
        scope.launch(Dispatchers.IO) {
            historyRepository.addHistory(song)
            val current = _playHistory.value.toMutableList()
            current.removeAll { it.id == song.id }
            current.add(0, song)
            withContext(Dispatchers.Main) {
                _playHistory.value = current
            }
        }
    }
}