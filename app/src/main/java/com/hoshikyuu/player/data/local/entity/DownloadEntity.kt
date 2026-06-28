package com.hoshikyuu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val songId: String,
    val songName: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String = "",
    val localFilePath: String,
    val lrc: String = "",               // 新增歌词字段
    val downloadTime: Long = System.currentTimeMillis(),
    val source: String = "wy"
)