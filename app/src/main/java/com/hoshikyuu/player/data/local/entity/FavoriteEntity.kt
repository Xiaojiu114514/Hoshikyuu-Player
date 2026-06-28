package com.hoshikyuu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val songName: String,
    val songArtist: String,
    val songAlbum: String = "",
    val songCoverUrl: String = "",
    val songMp3Url: String = "",
    val source: String = "wy",
    val addedAt: Long = System.currentTimeMillis()
)