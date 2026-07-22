package com.hoshikyuu.player.player

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    private val context: Context,
    private val downloadRepo: DownloadRepository,
    private val settingRepo: SettingRepository,
    private val musicRepository: MusicRepository
) {
    companion object {
        private const val TAG = "DownloadManager"
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://music.163.com/")
                .build()
            chain.proceed(request)
        }
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _progressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progressMap: StateFlow<Map<String, Float>> = _progressMap.asStateFlow()

    /**
     * 用户主动下载到 Download/HoshikyuuPlayer 目录（返回 Uri）
     */
    fun downloadSong(song: Song, onComplete: (Result<Uri>) -> Unit = {}) {
        scope.launch {
            try {
                Log.d(TAG, "downloadSong: ${song.id}")
                // 检查是否已下载
                val existing = downloadRepo.getDownload(song.id)
                if (existing != null) {
                    val uri = Uri.parse(existing.localFilePath)
                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: "")
                        if (file.exists()) {
                            withContext(Dispatchers.Main) {
                                onComplete(Result.success(uri))
                            }
                            return@launch
                        } else {
                            downloadRepo.removeDownload(song.id)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onComplete(Result.success(uri))
                        }
                        return@launch
                    }
                }

                // 获取完整歌曲信息
                val detailResult = musicRepository.fetchSongDetailForce(song.id, song.source)
                val fullSong = if (detailResult.isSuccess) {
                    detailResult.getOrNull()!!
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(Result.failure(Exception("无法获取歌曲信息")))
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

                // 生成文件名
                val format = settingRepo.getFileNameFormat()
                val fileName = if (format == SettingRepository.FORMAT_SONG_ARTIST) {
                    "${fullSong.name}-${fullSong.artist}"
                } else {
                    "${fullSong.artist}-${fullSong.name}"
                }.replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".mp3"
                Log.d(TAG, "文件名: $fileName")

                // 创建文件 Uri
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HoshikyuuPlayer")
                    }
                    val contentUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    context.contentResolver.insert(contentUri, contentValues)
                        ?: throw IOException("无法创建 MediaStore 条目")
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val playerDir = File(downloadsDir, "HoshikyuuPlayer")
                    if (!playerDir.exists()) {
                        if (!playerDir.mkdirs()) {
                            throw IOException("无法创建目录: ${playerDir.absolutePath}")
                        }
                    }
                    val file = File(playerDir, fileName)
                    if (file.exists()) file.delete()
                    Uri.fromFile(file)
                }

                // 下载并写入
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    if (uri.scheme == "content") {
                        context.contentResolver.delete(uri, null, null)
                    } else {
                        File(uri.path!!).delete()
                    }
                    withContext(Dispatchers.Main) {
                        onComplete(Result.failure(IOException("下载失败: ${response.code}")))
                    }
                    return@launch
                }

                response.body?.let { body ->
                    val totalBytes = body.contentLength()
                    var downloadedBytes = 0L
                    val buffer = ByteArray(8192)
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        body.byteStream().use { input ->
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                if (totalBytes > 0) {
                                    val progress = downloadedBytes.toFloat() / totalBytes
                                    _progressMap.value = _progressMap.value + (fullSong.id to progress)
                                }
                            }
                        }
                    } ?: throw IOException("无法打开输出流")
                }

                // 保存歌词（如果开关开启且有歌词）
                if (settingRepo.shouldSaveLyrics() && fullSong.lrc.isNotBlank()) {
                    try {
                        val lrcFileName = fileName.replace(".mp3", ".lrc")
                        val lrcUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val contentValues = ContentValues().apply {
                                put(MediaStore.Downloads.DISPLAY_NAME, lrcFileName)
                                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HoshikyuuPlayer")
                            }
                            val contentUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                            context.contentResolver.insert(contentUri, contentValues)
                        } else {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val playerDir = File(downloadsDir, "HoshikyuuPlayer")
                            if (!playerDir.exists()) playerDir.mkdirs()
                            val file = File(playerDir, lrcFileName)
                            if (file.exists()) file.delete()
                            Uri.fromFile(file)
                        }
                        lrcUri?.let {
                            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                                outputStream.write(fullSong.lrc.toByteArray(Charsets.UTF_8))
                            }
                            Log.d(TAG, "歌词保存成功: $lrcFileName")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "保存歌词失败", e)
                    }
                }

                // 保存到数据库
                downloadRepo.addDownload(fullSong, uri.toString())
                Log.d(TAG, "下载完成, Uri: $uri")
                withContext(Dispatchers.Main) {
                    onComplete(Result.success(uri))
                }

            } catch (e: Exception) {
                Log.e(TAG, "下载异常", e)
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(e))
                }
            }
        }
    }

    /**
     * 缓存到内部私有目录（用于离线播放缓存，不显示在 Downloads 中）
     */
    fun cacheSongWithFullInfo(fullSong: Song, onComplete: (Result<File>) -> Unit = {}) {
        scope.launch {
            try {
                if (downloadRepo.isDownloaded(fullSong.id)) {
                    withContext(Dispatchers.Main) {
                        onComplete(Result.failure(Exception("歌曲已下载到下载管理")))
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

                val cacheDir = File(context.filesDir, "cache/songs")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val file = File(cacheDir, "${fullSong.id}.mp3")

                if (file.exists()) {
                    withContext(Dispatchers.Main) {
                        onComplete(Result.success(file))
                    }
                    return@launch
                }

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onComplete(Result.failure(IOException("缓存下载失败: ${response.code}")))
                    }
                    return@launch
                }

                response.body?.let { body ->
                    val buffer = ByteArray(8192)
                    FileOutputStream(file).use { fos ->
                        body.byteStream().use { input ->
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                fos.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onComplete(Result.success(file))
                }

            } catch (e: Exception) {
                Log.e(TAG, "缓存异常", e)
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(e))
                }
            }
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