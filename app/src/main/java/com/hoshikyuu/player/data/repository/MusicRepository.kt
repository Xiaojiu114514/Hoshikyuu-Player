package com.hoshikyuu.player.data.repository

import com.hoshikyuu.player.data.remote.MusicApi
import com.hoshikyuu.player.data.remote.MusicSearchItem
import com.hoshikyuu.player.domain.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val api: MusicApi,
    private val cacheRepository: SongCacheRepository
) {

    companion object {
        const val API_TOKEN = "???"
        val SOURCES = listOf("wy", "qq", "kw", "mg")
    }

    suspend fun searchSongs(
        query: String,
        source: String = "wy",
        page: Int = 1,
        limit: Int = 30
    ): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchSongs(
                token = API_TOKEN, name = query, type = source, page = page, limit = limit
            )
            if (response.code == 1 && response.data != null) {
                Result.success(response.data.map { it.toSong() })
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSongDetailWithCache(id: String, source: String = "wy"): Result<Song> =
        withContext(Dispatchers.IO) {
            try {
                val cached = cacheRepository.getCachedSong(id)
                if (cached != null && cached.mp3Url.isNotEmpty()) {
                    return@withContext Result.success(cached)
                }

                val response = api.getSongDetail(token = API_TOKEN, id = id, type = source)
                if (response.code == 1 && response.data != null) {
                    val detail = response.data
                    val song = Song(
                        id = id,
                        name = detail.name,
                        album = detail.album,
                        artist = detail.artist,
                        coverUrl = detail.pic,
                        mp3Url = detail.url,
                        lrc = detail.lrc,
                        source = source
                    )
                    cacheRepository.saveSong(song)
                    Result.success(song)
                } else {
                    if (cached != null) {
                        Result.success(cached)
                    } else {
                        Result.failure(Exception(response.msg))
                    }
                }
            } catch (e: Exception) {
                val cached = cacheRepository.getCachedSong(id)
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(e)
                }
            }
        }

    /**
     * 强制从网络获取歌曲详情（忽略本地缓存），并更新缓存
     */
    suspend fun fetchSongDetailForce(id: String, source: String = "wy"): Result<Song> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getSongDetail(token = API_TOKEN, id = id, type = source)
                if (response.code == 1 && response.data != null) {
                    val detail = response.data
                    val song = Song(
                        id = id,
                        name = detail.name,
                        album = detail.album,
                        artist = detail.artist,
                        coverUrl = detail.pic,
                        mp3Url = detail.url,
                        lrc = detail.lrc,
                        source = source
                    )
                    // 更新缓存
                    cacheRepository.saveSong(song)
                    Result.success(song)
                } else {
                    Result.failure(Exception(response.msg))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getRanking(
        rankingId: String,
        source: String = "wy"
    ): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRanking(
                token = API_TOKEN,
                id = rankingId,
                type = source
            )
            if (response.code == "1" || response.code == "200") {
                val songs = response.data?.map { item ->
                    Song(
                        id = item.songId,
                        name = item.songName,
                        album = item.albumName,
                        artist = item.artistName,
                        coverUrl = item.pic,
                        source = item.type
                    )
                } ?: emptyList()
                Result.success(songs)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun MusicSearchItem.toSong() = Song(
        id = id,
        name = name,
        album = album,
        artist = artist,
        source = type
    )
}