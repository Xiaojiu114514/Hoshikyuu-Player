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
        private const val KEY_SAVE_LYRICS = "save_lyrics"

        const val FORMAT_SONG_ARTIST = 0
        const val FORMAT_ARTIST_SONG = 1
        const val DEFAULT_SAVE_LYRICS = true
    }

    fun getDarkMode(): Int = prefs.getInt(KEY_DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    fun setDarkMode(mode: Int) {
        prefs.edit().putInt(KEY_DARK_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getFileNameFormat(): Int = prefs.getInt(KEY_FILE_NAME_FORMAT, FORMAT_SONG_ARTIST)
    fun setFileNameFormat(format: Int) {
        prefs.edit().putInt(KEY_FILE_NAME_FORMAT, format).apply()
    }

    fun shouldSaveLyrics(): Boolean = prefs.getBoolean(KEY_SAVE_LYRICS, DEFAULT_SAVE_LYRICS)
    fun setSaveLyrics(save: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_LYRICS, save).apply()
    }
}