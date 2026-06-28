package com.hoshikyuu.player.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingRepository @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FILE_NAME_FORMAT = "file_name_format"
        const val FORMAT_SONG_ARTIST = 0   // 歌曲名-歌手
        const val FORMAT_ARTIST_SONG = 1   // 歌手-歌曲名
    }

    fun getDarkMode(): Int {
        return prefs.getInt(KEY_DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setDarkMode(mode: Int) {
        prefs.edit().putInt(KEY_DARK_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getFileNameFormat(): Int {
        return prefs.getInt(KEY_FILE_NAME_FORMAT, FORMAT_SONG_ARTIST)
    }

    fun setFileNameFormat(format: Int) {
        prefs.edit().putInt(KEY_FILE_NAME_FORMAT, format).apply()
    }
}