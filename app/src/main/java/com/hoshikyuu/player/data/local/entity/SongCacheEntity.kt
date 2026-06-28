package com.hoshikyuu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_cache")
data class SongCacheEntity(
    @PrimaryKey val songId: String,
    val name: String,
    val album: String,
    val artist: String,
    val coverUrl: String = "",
    val mp3Url: String = "",
    val lrc: String = "",
    val source: String = "wy",
    val cachedAt: Long = System.currentTimeMillis()
)