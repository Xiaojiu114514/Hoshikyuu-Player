package com.hoshikyuu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hoshikyuu.player.domain.Song

@Entity(tableName = "ranking_cache")
data class RankingCacheEntity(
    @PrimaryKey val rankingId: String,
    val rankingName: String,
    val songsJson: String,
    val cachedDate: String,
    val cachedAt: Long = System.currentTimeMillis()
)

// 辅助对象，用于序列化/反序列化
object RankingCacheSerializer {
    private val gson = Gson()
    private val songListType = object : TypeToken<List<Song>>() {}.type

    fun songsToJson(songs: List<Song>): String = gson.toJson(songs)

    fun jsonToSongs(json: String): List<Song> {
        return try {
            gson.fromJson(json, songListType)
        } catch (e: Exception) {
            emptyList()
        }
    }
}