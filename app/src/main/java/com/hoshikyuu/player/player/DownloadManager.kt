package com.hoshikyuu.player.player

import android.content.Context
import com.hoshikyuu.player.data.repository.DownloadRepository
import com.hoshikyuu.player.data.repository.MusicRepository
import com.hoshikyuu.player.data.repository.SettingRepository
import com.hoshikyuu.player.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    private val context: Context,
    private val downloadRepo: DownloadRepository,
    private val settingRepo: SettingRepository,
    private val musicRepository: MusicRepository
) {
    private val client = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _progressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progressMap: StateFlow<Map<String, Float>> = _progressMap.asStateFlow()

    /**
     * 使用已有的完整歌曲信息进行缓存（不调用 API）
     * @param fullSong 已包含 mp3Url 和 lrc 的完整 Song 对象
     * @param onComplete 回调
     */
    fun cacheSongWithFullInfo(fullSong: Song, onComplete: (Result<File>) -> Unit = {}) {
        scope.launch {
            // 检查是否已下载（用户下载管理）
            if (downloadRepo.isDownloaded(fullSong.id)) {
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(Exception("歌曲已下载")))
                }
                return@launch
            }

            val url = fullSong.mp3Url
            if (url.isBlank()) {
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(Exception("无效的播放链接")))
                }
                return@launch
            }

            // 缓存目录
            val cacheDir = File(context.filesDir, "cache/songs")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "${fullSong.id}.mp3")

            // 如果已存在，直接返回成功
            if (file.exists()) {
                withContext(Dispatchers.Main) {
                    onComplete(Result.success(file))
                }
                return@launch
            }

            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onComplete(Result.failure(Exception("下载失败: ${response.code}")))
                    }
                    return@launch
                }

                response.body?.let { body ->
                    val buffer = ByteArray(8192)
                    FileOutputStream(file).use { fos ->
                        body.byteStream().use { input ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                fos.write(buffer, 0, read)
                            }
                        }
                    }
                }

                // 下载完成
                withContext(Dispatchers.Main) {
                    onComplete(Result.success(file))
                }

            } catch (e: Exception) {
                file.delete()
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(e))
                }
            }
        }
    }

    /**
     * 原方法：自行调用 API 获取歌曲信息后再缓存（用户主动下载时使用）
     */
    fun cacheSong(song: Song, onComplete: (Result<File>) -> Unit = {}) {
        scope.launch {
            val detailResult = musicRepository.fetchSongDetailForce(song.id, song.source)
            val fullSong = if (detailResult.isSuccess) {
                detailResult.getOrNull()!!
            } else {
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(Exception("无法获取歌曲信息")))
                }
                return@launch
            }
            // 复用新方法
            cacheSongWithFullInfo(fullSong, onComplete)
        }
    }

    /**
     * 用户主动下载到下载管理（与缓存分离）
     */
    fun downloadSong(song: Song, onComplete: (Result<File>) -> Unit = {}) {
        // 此方法之前已有，保持不变，内部可能也调用 API，但场景不同（用户主动下载）
        // 但为了完整，可复用 cacheSong 逻辑后再复制到下载目录，但原有代码已实现，这里不重复提供。
        // 如果你的项目中已有完整实现，此方法无需修改。
        // 为了保持完整性，这里给出简要框架，实际请保留你的原有实现。
        scope.launch {
            // ... 原有下载逻辑 ...
        }
    }

    suspend fun getCachedFile(songId: String): File? {
        val cacheDir = File(context.filesDir, "cache/songs")
        val file = File(cacheDir, "$songId.mp3")
        return if (file.exists()) file else null
    }

    suspend fun clearCache() {
        val cacheDir = File(context.filesDir, "cache/songs")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
}