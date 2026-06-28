// file: data/local/dao/DownloadDao.kt
package com.hoshikyuu.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hoshikyuu.player.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadTime DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE songId = :songId")
    suspend fun removeDownload(songId: String)

    @Query("SELECT * FROM downloads WHERE songId = :songId")
    suspend fun getDownload(songId: String): DownloadEntity?
}