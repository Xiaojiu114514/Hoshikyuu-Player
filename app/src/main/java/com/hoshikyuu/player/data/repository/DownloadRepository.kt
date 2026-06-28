package com.hoshikyuu.player.data.repository

import android.content.Context
import com.hoshikyuu.player.data.local.dao.DownloadDao
import com.hoshikyuu.player.data.local.entity.DownloadEntity
import com.hoshikyuu.player.domain.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao,
    private val context: Context
) {

    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    suspend fun addDownload(song: Song, localFilePath: String) {
        downloadDao.addDownload(
            DownloadEntity(
                songId = song.id,
                songName = song.name,
                artist = song.artist,
                album = song.album,
                coverUrl = song.coverUrl,
                localFilePath = localFilePath,
                lrc = song.lrc,   // 保存歌词
                source = song.source
            )
        )
    }

    suspend fun removeDownload(songId: String) {
        val entity = downloadDao.getDownload(songId)
        entity?.localFilePath?.let { File(it).delete() }
        downloadDao.removeDownload(songId)
    }

    suspend fun getDownload(songId: String): DownloadEntity? = downloadDao.getDownload(songId)

    fun getDownloadedSongs(): Flow<List<Song>> =
        downloadDao.getAllDownloads().map { entities ->
            entities.map {
                Song(
                    id = it.songId,
                    name = it.songName,
                    album = it.album,
                    artist = it.artist,
                    coverUrl = it.coverUrl,
                    mp3Url = it.localFilePath,
                    lrc = it.lrc,        // 加载歌词
                    source = it.source
                )
            }
        }

    suspend fun isDownloaded(songId: String): Boolean = downloadDao.getDownload(songId) != null

    suspend fun getDownloadTotalSize(): Long {
        val entities = downloadDao.getAllDownloads().first()
        return entities.sumOf { File(it.localFilePath).takeIf { f -> f.exists() }?.length() ?: 0L }
    }

    suspend fun clearAllDownloads() {
        val entities = downloadDao.getAllDownloads().first()
        entities.forEach { File(it.localFilePath).delete() }
        entities.forEach { downloadDao.removeDownload(it.songId) }
    }
}