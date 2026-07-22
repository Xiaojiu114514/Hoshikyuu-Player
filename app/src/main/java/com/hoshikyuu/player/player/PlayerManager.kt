package com.hoshikyuu.player.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hoshikyuu.player.data.repository.*
import com.hoshikyuu.player.domain.Song
import com.hoshikyuu.player.ui.utils.parseLyrics
import com.hoshikyuu.player.utils.NetworkPreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private val networkPreferenceManager: NetworkPreferenceManager,
    private val desktopLyricsManager: DesktopLyricsManager
) {

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build()
    }

    fun getPlayer(): ExoPlayer = exoPlayer

    // ---------- StateFlows ----------
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

    private val _currentLyricsLine = MutableStateFlow("")
    val currentLyricsLine: StateFlow<String> = _currentLyricsLine.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lyricsUpdateJob: Job? = null

    // ---------- 辅助 ----------
    fun isSongInQueue(songId: String): Boolean = _queue.value.any { it.id == songId }
    suspend fun isSongDownloaded(songId: String): Boolean = downloadRepository.isDownloaded(songId)
    fun isNetworkBlocked(): Boolean = networkPreferenceManager.isNetworkBlocked()

    // ---------- 构建带元数据的 MediaItem ----------
    private fun buildMediaItem(song: Song): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(song.name)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
        if (!song.coverUrl.isNullOrEmpty()) {
            try {
                metadataBuilder.setArtworkUri(Uri.parse(song.coverUrl))
            } catch (_: Exception) { /* ignore */ }
        }
        return MediaItem.Builder()
            .setUri(song.mp3Url)
            .setMediaId(song.id)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    // ---------- 确保本地歌曲加载歌词 ----------
    private fun ensureLocalLyrics(song: Song): Song {
        if (song.source != "local" &&
            !song.mp3Url.startsWith("/") &&
            !song.mp3Url.startsWith("file://") &&
            !song.mp3Url.startsWith("content://")) {
            return song
        }
        if (!song.lrc.isNullOrEmpty()) {
            return song
        }
        try {
            val mp3File = File(song.mp3Url)
            val baseName = mp3File.nameWithoutExtension
            val lrcFile = File(mp3File.parent, "$baseName.lrc")
            if (lrcFile.exists()) {
                val lrcContent = lrcFile.readText()
                return song.copy(lrc = lrcContent)
            }
        } catch (e: Exception) { /* ignore */ }
        return song
    }

    // ---------- 同步整个队列到播放器 ----------
    private fun syncQueueToPlayer(startIndex: Int) {
        val songs = _queue.value
        if (songs.isEmpty()) {
            exoPlayer.stop()
            return
        }
        val mediaItems = songs.map { buildMediaItem(it) }
        exoPlayer.setMediaItems(mediaItems, startIndex, 0)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        applyRepeatMode()
    }

    // ---------- 应用循环模式 ----------
    private fun applyRepeatMode() {
        val exoMode = when (_repeatMode.value) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        exoPlayer.repeatMode = exoMode
        android.util.Log.d("PlayerManager", "applyRepeatMode: ${_repeatMode.value} -> $exoMode")
    }

    // ---------- 初始化 ----------
    init {
        _repeatMode.value = RepeatMode.ALL
        applyRepeatMode()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0)
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
                _currentLyricsLine.value = ""
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

        startLyricsUpdate()
    }

    // ---------- 歌词更新协程 ----------
    private fun startLyricsUpdate() {
        lyricsUpdateJob?.cancel()
        lyricsUpdateJob = scope.launch {
            while (true) {
                delay(200)
                updateProgress()
                val pos = (progress.value * duration.value).toLong()
                val song = currentSong.value
                val line = if (song != null) {
                    val lines = parseLyrics(song.lrc)
                    val idx = lines.indexOfLast { it.timestampMs <= pos }
                    if (idx >= 0 && idx < lines.size) lines[idx].text else ""
                } else ""
                if (line != _currentLyricsLine.value) {
                    _currentLyricsLine.value = line
                    desktopLyricsManager.updateLyrics(line)
                }
            }
        }
    }

    // ---------- 核心播放入口 ----------

    fun play(song: Song) {
        scope.launch {
            // 1. 优先检查本地缓存/下载
            val local = withContext(Dispatchers.IO) { getLocalSong(song.id) }
            if (local != null) {
                val finalSong = ensureLocalLyrics(local)
                _queue.value = listOf(finalSong)
                _currentIndex.value = 0
                playInternalDirect(finalSong)
                return@launch
            }

            // 2. 检查是否为本地文件路径
            val isLocal = song.mp3Url.startsWith("/") || song.mp3Url.startsWith("file://") || song.mp3Url.startsWith("content://")
            if (isLocal) {
                val finalSong = ensureLocalLyrics(song)
                _queue.value = listOf(finalSong)
                _currentIndex.value = 0
                playInternalDirect(finalSong)
                return@launch
            }

            // 3. 检查网络是否被禁用
            if (networkPreferenceManager.isNetworkBlocked()) {
                _errorMessage.value = "网络已禁用，无法播放在线歌曲"
                return@launch
            }

            // 4. 请求网络获取完整信息
            val fullSong = withContext(Dispatchers.IO) { fetchSongDetailForce(song) }
            if (fullSong == null) {
                _errorMessage.value = "获取歌曲信息失败"
                return@launch
            }

            withContext(Dispatchers.IO) {
                cacheSongForOffline(fullSong)
            }

            _queue.value = listOf(fullSong)
            _currentIndex.value = 0
            playInternal(fullSong)
        }
    }

    /**
     * 播放整个队列（将整个 _queue 同步到播放器）
     */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        scope.launch {
            // 确保所有歌曲都加载歌词
            val ensuredSongs = songs.map { ensureLocalLyrics(it) }
            _queue.value = ensuredSongs
            _currentIndex.value = startIndex
            _currentSong.value = ensuredSongs.getOrNull(startIndex)

            // 将整个队列同步到播放器
            syncQueueToPlayer(startIndex)
        }
    }

    fun addToQueueAfterCurrent(song: Song) {
        scope.launch {
            val local = withContext(Dispatchers.IO) { getLocalSong(song.id) }
            if (local != null) {
                val finalSong = ensureLocalLyrics(local)
                addToQueueInternal(finalSong, afterCurrent = true)
                return@launch
            }

            val isLocal = song.mp3Url.startsWith("/") || song.mp3Url.startsWith("file://") || song.mp3Url.startsWith("content://")
            if (isLocal) {
                val finalSong = ensureLocalLyrics(song)
                addToQueueInternal(finalSong, afterCurrent = true)
                return@launch
            }

            if (networkPreferenceManager.isNetworkBlocked()) {
                _errorMessage.value = "网络已禁用，无法添加在线歌曲"
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
            val local = withContext(Dispatchers.IO) { getLocalSong(song.id) }
            if (local != null) {
                val finalSong = ensureLocalLyrics(local)
                addToQueueInternal(finalSong, afterCurrent = false)
                return@launch
            }

            val isLocal = song.mp3Url.startsWith("/") || song.mp3Url.startsWith("file://") || song.mp3Url.startsWith("content://")
            if (isLocal) {
                val finalSong = ensureLocalLyrics(song)
                addToQueueInternal(finalSong, afterCurrent = false)
                return@launch
            }

            if (networkPreferenceManager.isNetworkBlocked()) {
                _errorMessage.value = "网络已禁用，无法添加在线歌曲"
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
        // 增量添加，不打断播放
        exoPlayer.addMediaItem(insertIdx, buildMediaItem(song))
        applyRepeatMode()
    }

    fun removeFromQueue(index: Int) {
        val q = _queue.value.toMutableList()
        if (index !in q.indices) return

        val isCurrent = (index == _currentIndex.value)
        q.removeAt(index)
        _queue.value = q

        if (isCurrent) {
            if (q.isEmpty()) {
                exoPlayer.stop()
                _currentSong.value = null
                _currentIndex.value = -1
            } else {
                val nextIdx = if (index < q.size) index else q.size - 1
                scope.launch {
                    val nextSong = q[nextIdx]
                    val local = withContext(Dispatchers.IO) { getLocalSong(nextSong.id) }
                    if (local != null) {
                        val finalLocal = ensureLocalLyrics(local)
                        val newQ = _queue.value.toMutableList()
                        newQ[nextIdx] = finalLocal
                        _queue.value = newQ
                        _currentIndex.value = nextIdx
                        playInternalDirect(finalLocal)
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
            if (index < exoPlayer.mediaItemCount) {
                exoPlayer.removeMediaItem(index)
            }
            applyRepeatMode()
        }
    }

    // ---------- 播放控制 ----------

    fun togglePlay() {
        if (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.playbackState == Player.STATE_BUFFERING) {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        }
    }

    fun seekTo(fraction: Float) {
        val pos = (fraction * _duration.value).toLong()
        exoPlayer.seekTo(pos)
        _progress.value = fraction
    }

    fun skipNext() {
        if (_queue.value.isEmpty()) return
        val nextIdx = (_currentIndex.value + 1) % _queue.value.size
        // 直接使用 exoPlayer 的 seekTo 切换，同时更新状态
        exoPlayer.seekTo(nextIdx, 0)
        _currentIndex.value = nextIdx
        _currentSong.value = _queue.value[nextIdx]
    }

    fun skipPrevious() {
        if (_queue.value.isEmpty()) return
        val prevIdx = if (_currentIndex.value <= 0) _queue.value.size - 1 else _currentIndex.value - 1
        exoPlayer.seekTo(prevIdx, 0)
        _currentIndex.value = prevIdx
        _currentSong.value = _queue.value[prevIdx]
    }

    fun skipToIndex(index: Int) {
        if (index < 0 || index >= _queue.value.size) return
        exoPlayer.seekTo(index, 0)
        _currentIndex.value = index
        _currentSong.value = _queue.value[index]
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        applyRepeatMode()
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
        val dur = exoPlayer.duration.coerceAtLeast(0)
        if (dur > 0) {
            _duration.value = dur
            _progress.value = (exoPlayer.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
        }
    }

    fun release() {
        lyricsUpdateJob?.cancel()
        exoPlayer.release()
    }

    // ---------- 核心内部方法 ----------

    /**
     * 单曲播放（用于 play 方法，不涉及队列同步）
     */
    private fun playInternal(song: Song) {
        if (song.mp3Url.isEmpty()) {
            _errorMessage.value = "歌曲播放链接无效，请尝试其他源"
            return
        }
        _currentSong.value = song
        addToHistory(song)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(buildMediaItem(song))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        _errorMessage.value = null
        applyRepeatMode()
    }

    /**
     * 单曲播放（直接播放，不进行网络检查，用于本地文件或缓存）
     */
    private fun playInternalDirect(song: Song) {
        if (song.mp3Url.isEmpty()) {
            _errorMessage.value = "歌曲文件无效"
            return
        }
        _currentSong.value = song
        addToHistory(song)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(buildMediaItem(song))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        _errorMessage.value = null
        applyRepeatMode()
    }

    private suspend fun getLocalSong(songId: String): Song? {
        // 1. 检查下载管理（存储的是 Uri 字符串）
        val download = downloadRepository.getDownload(songId)
        if (download != null) {
            val uriString = download.localFilePath
            if (uriString.startsWith("content://")) {
                return Song(
                    id = download.songId,
                    name = download.songName,
                    album = download.album,
                    artist = download.artist,
                    coverUrl = download.coverUrl,
                    mp3Url = uriString,
                    lrc = download.lrc,
                    source = download.source
                )
            } else {
                val file = File(uriString)
                if (file.exists()) {
                    return Song(
                        id = download.songId,
                        name = download.songName,
                        album = download.album,
                        artist = download.artist,
                        coverUrl = download.coverUrl,
                        mp3Url = uriString,
                        lrc = download.lrc,
                        source = download.source
                    )
                } else {
                    downloadRepository.removeDownload(songId)
                }
            }
        }

        // 2. 检查缓存（内部私有目录）
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

    private suspend fun cacheSongForOffline(fullSong: Song) {
        try {
            val cached = cacheRepository.getCachedSong(fullSong.id)
            if (cached != null) {
                val file = File(cached.mp3Url)
                if (file.exists()) return
            }
            val download = downloadRepository.getDownload(fullSong.id)
            if (download != null) return
            downloadManager.cacheSongWithFullInfo(fullSong) { result ->
                if (result.isSuccess) {
                    val file = result.getOrNull()
                    file?.let {
                        scope.launch(Dispatchers.IO) {
                            cacheRepository.saveSong(
                                fullSong.copy(mp3Url = it.absolutePath)
                            )
                        }
                    }
                }
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