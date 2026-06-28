package com.hoshikyuu.player.di

import android.content.Context
import androidx.room.Room
import com.hoshikyuu.player.data.local.AppDatabase
import com.hoshikyuu.player.data.local.dao.*
import com.hoshikyuu.player.data.remote.MusicApi
import com.hoshikyuu.player.data.remote.NetworkClient
import com.hoshikyuu.player.data.repository.*
import com.hoshikyuu.player.player.DownloadManager
import com.hoshikyuu.player.utils.NetworkPreferenceManager
import com.hoshikyuu.player.utils.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMusicApi(): MusicApi = NetworkClient.musicApi

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "hoshikyuu_player.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .build()

    @Provides
    @Singleton
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    @Singleton
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    @Singleton
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideSongCacheDao(db: AppDatabase): SongCacheDao = db.songCacheDao()

    @Provides
    @Singleton
    fun provideDownloadDao(db: AppDatabase): DownloadDao = db.downloadDao()

    @Provides
    @Singleton
    fun provideRankingCacheDao(db: AppDatabase): RankingCacheDao = db.rankingCacheDao()

    @Provides
    @Singleton
    fun providePlaylistRepository(dao: PlaylistDao): PlaylistRepository = PlaylistRepository(dao)

    @Provides
    @Singleton
    fun provideFavoriteRepository(dao: FavoriteDao): FavoriteRepository = FavoriteRepository(dao)

    @Provides
    @Singleton
    fun provideHistoryRepository(dao: HistoryDao): HistoryRepository = HistoryRepository(dao)

    @Provides
    @Singleton
    fun provideSongCacheRepository(dao: SongCacheDao): SongCacheRepository = SongCacheRepository(dao)

    @Provides
    @Singleton
    fun provideDownloadRepository(
        dao: DownloadDao,
        @ApplicationContext context: Context
    ): DownloadRepository = DownloadRepository(dao, context)

    @Provides
    @Singleton
    fun provideRankingCacheRepository(dao: RankingCacheDao): RankingCacheRepository =
        RankingCacheRepository(dao)

    @Provides
    @Singleton
    fun provideMusicRepository(
        api: MusicApi,
        cacheRepository: SongCacheRepository
    ): MusicRepository = MusicRepository(api, cacheRepository)

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        downloadRepo: DownloadRepository,
        settingRepo: SettingRepository,
        musicRepository: MusicRepository
    ): DownloadManager = DownloadManager(context, downloadRepo, settingRepo, musicRepository)

    @Provides
    @Singleton
    fun provideAvatarRepository(@ApplicationContext context: Context): AvatarRepository =
        AvatarRepository(context)

    @Provides
    @Singleton
    fun provideSettingRepository(@ApplicationContext context: Context): SettingRepository =
        SettingRepository(context)

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils =
        NetworkUtils(context)

    @Provides
    @Singleton
    fun provideNetworkPreferenceManager(@ApplicationContext context: Context): NetworkPreferenceManager =
        NetworkPreferenceManager(context)
}