package com.hoshikyuu.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.hoshikyuu.player.domain.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSongRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 扫描本地歌曲：优先使用 MediaStore 查询公共下载目录中的 mp3 和 lrc 文件。
     * 如果权限不足，则回退到传统文件访问（但 Android 10+ 可能无法读取）。
     */
    suspend fun scanLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        // 1. 使用 MediaStore 查询 mp3 文件（Android 10+ 推荐方式）
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection = MediaStore.Files.FileColumns.DATA + " LIKE ?"
        // 查询 Download 目录下的 mp3 文件（路径包含 /Download/HoshikyuuPlayer/）
        val selectionArgs = arrayOf("%/Download/HoshikyuuPlayer/%.mp3")

        val queryUri = MediaStore.Files.getContentUri("external")
        val cursor = context.contentResolver.query(
            queryUri,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

            while (it.moveToNext()) {
                val filePath = it.getString(dataColumn)
                val fileName = it.getString(nameColumn)
                // 提取文件名（不含扩展名）
                val baseName = fileName.substringBeforeLast('.')
                // 通过文件路径查找对应的 lrc 文件
                val lrcFile = File(filePath).parent?.let { dir ->
                    File(dir, "$baseName.lrc")
                }
                val lrcContent = lrcFile?.takeIf { it.exists() }?.readText() ?: ""

                songs.add(
                    Song(
                        id = "local_${File(filePath).hashCode()}",
                        name = baseName,
                        artist = "本地歌曲",
                        album = "本地",
                        coverUrl = "",
                        mp3Url = filePath,
                        lrc = lrcContent,
                        source = "local"
                    )
                )
            }
        }

        // 如果 MediaStore 未找到（比如权限问题或路径不对），回退到直接文件遍历（仅在 Android 10 以下或授权后有效）
        if (songs.isEmpty()) {
            songs.addAll(fallbackFileScan())
        }

        songs
    }

    /**
     * 回退方案：直接通过 File 访问（需要文件权限，Android 10+ 可能不可用）
     */
    private fun fallbackFileScan(): List<Song> {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(downloadDir, "HoshikyuuPlayer")
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        val mp3Files = targetDir.listFiles { file ->
            file.isFile && file.extension.equals("mp3", ignoreCase = true)
        } ?: return emptyList()

        val lrcFiles = targetDir.listFiles { file ->
            file.isFile && file.extension.equals("lrc", ignoreCase = true)
        }?.associateBy { it.nameWithoutExtension } ?: emptyMap()

        return mp3Files.mapNotNull { mp3File ->
            val baseName = mp3File.nameWithoutExtension
            val lrcFile = lrcFiles[baseName]
            val lrcContent = lrcFile?.readText() ?: ""
            Song(
                id = "local_${mp3File.absolutePath.hashCode()}",
                name = baseName,
                artist = "本地歌曲",
                album = "本地",
                coverUrl = "",
                mp3Url = mp3File.absolutePath,
                lrc = lrcContent,
                source = "local"
            )
        }
    }
}