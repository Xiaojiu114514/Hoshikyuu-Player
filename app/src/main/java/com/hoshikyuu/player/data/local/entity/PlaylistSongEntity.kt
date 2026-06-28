package com.hoshikyuu.player.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: String,
    val songName: String,
    val songArtist: String,
    val songAlbum: String = "",
    val songCoverUrl: String = "",
    val songMp3Url: String = "",
    val source: String = "wy",
    val addedAt: Long = System.currentTimeMillis()
)