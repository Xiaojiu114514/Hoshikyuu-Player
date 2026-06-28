package com.hoshikyuu.player.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RankingResponse(
    val code: String,
    val msg: String,
    val data: List<RankingItem>? = null
)

@Serializable
data class RankingItem(
    val songId: String,
    val songName: String,
    val artistName: String,
    val albumName: String,
    val type: String,
    val pic: String = ""
)