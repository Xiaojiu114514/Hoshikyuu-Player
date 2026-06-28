package com.hoshikyuu.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hoshikyuu.player.data.local.dao.*
import com.hoshikyuu.player.data.local.entity.*

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        SongCacheEntity::class,
        DownloadEntity::class,
        RankingCacheEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun songCacheDao(): SongCacheDao
    abstract fun downloadDao(): DownloadDao
    abstract fun rankingCacheDao(): RankingCacheDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE playlist_songs ADD COLUMN source TEXT NOT NULL DEFAULT 'wy'")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS favorites (
                        songId TEXT PRIMARY KEY,
                        songName TEXT NOT NULL,
                        songArtist TEXT NOT NULL,
                        songAlbum TEXT NOT NULL DEFAULT '',
                        songCoverUrl TEXT NOT NULL DEFAULT '',
                        songMp3Url TEXT NOT NULL DEFAULT '',
                        source TEXT NOT NULL DEFAULT 'wy',
                        addedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS history (
                        songId TEXT PRIMARY KEY,
                        songName TEXT NOT NULL,
                        songArtist TEXT NOT NULL,
                        songAlbum TEXT NOT NULL DEFAULT '',
                        songCoverUrl TEXT NOT NULL DEFAULT '',
                        songMp3Url TEXT NOT NULL DEFAULT '',
                        source TEXT NOT NULL DEFAULT 'wy',
                        playedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS song_cache (
                        songId TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        album TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        coverUrl TEXT NOT NULL DEFAULT '',
                        mp3Url TEXT NOT NULL DEFAULT '',
                        lrc TEXT NOT NULL DEFAULT '',
                        source TEXT NOT NULL DEFAULT 'wy',
                        cachedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloads (
                        songId TEXT PRIMARY KEY,
                        songName TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL DEFAULT '',
                        coverUrl TEXT NOT NULL DEFAULT '',
                        localFilePath TEXT NOT NULL,
                        lrc TEXT NOT NULL DEFAULT '',
                        downloadTime INTEGER NOT NULL,
                        source TEXT NOT NULL DEFAULT 'wy'
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ranking_cache (
                        rankingId TEXT PRIMARY KEY,
                        rankingName TEXT NOT NULL,
                        songsJson TEXT NOT NULL,
                        cachedDate TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}