package com.hoshikyuu.player.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hoshikyuu.player.data.local.entity.PlaylistEntity
import com.hoshikyuu.player.data.local.entity.PlaylistSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getPlaylistSongs(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(song: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun isSongInPlaylist(playlistId: Long, songId: String): Int
}
