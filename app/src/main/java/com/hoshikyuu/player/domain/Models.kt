package com.hoshikyuu.player.domain

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val name: String,
    val album: String,
    val artist: String,
    val coverUrl: String = "",
    val mp3Url: String = "",
    val lrc: String = "",
    val source: String = "wy"
)

data class Playlist(
    val id: Long = 0,
    val name: String,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
