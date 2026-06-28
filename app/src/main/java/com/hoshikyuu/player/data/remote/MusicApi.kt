package com.hoshikyuu.player.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApi {

    @GET("API/???")
    suspend fun searchSongs(
        @Query("token") token: String,
        @Query("name") name: String,
        @Query("type") type: String = "wy",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): ApiResponse<List<MusicSearchItem>>

    @GET("API/???")
    suspend fun getSongDetail(
        @Query("token") token: String,
        @Query("id") id: String,
        @Query("type") type: String = "wy"
    ): ApiResponse<SongDetail>

    @GET("API/???")
    suspend fun getRanking(
        @Query("token") token: String,
        @Query("id") id: String,
        @Query("type") type: String = "wy"
    ): RankingResponse
}