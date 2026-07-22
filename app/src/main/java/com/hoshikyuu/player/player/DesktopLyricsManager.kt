package com.hoshikyuu.player.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DesktopLyricsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("desktop_lyrics", Context.MODE_PRIVATE)

    // 启用状态流
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    companion object {
        private const val KEY_ENABLED = "desktop_lyrics_enabled"
        private const val KEY_ALPHA = "desktop_lyrics_alpha"
        private const val KEY_TEXT_SIZE = "desktop_lyrics_text_size"
        private const val KEY_TEXT_COLOR = "desktop_lyrics_text_color"
        private const val KEY_BG_COLOR = "desktop_lyrics_bg_color"
        private const val KEY_BG_ALPHA = "desktop_lyrics_bg_alpha"

        // 默认值：红色、15sp、背景透明
        const val DEFAULT_ALPHA = 200
        const val DEFAULT_TEXT_SIZE = 15
        const val DEFAULT_TEXT_COLOR = 0xFFFF0000.toInt()
        const val DEFAULT_BG_COLOR = 0x000000
        const val DEFAULT_BG_ALPHA = 0
    }

    fun isEnabled(): Boolean = _enabled.value

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
        if (enabled) {
            context.startService(Intent(context, LyricsOverlayService::class.java))
        } else {
            context.stopService(Intent(context, LyricsOverlayService::class.java))
        }
    }

    fun getAlpha(): Int = prefs.getInt(KEY_ALPHA, DEFAULT_ALPHA)
    fun setAlpha(alpha: Int) {
        prefs.edit().putInt(KEY_ALPHA, alpha).apply()
        refreshDesktop()
    }

    fun getTextSize(): Int = prefs.getInt(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
    fun setTextSize(size: Int) {
        prefs.edit().putInt(KEY_TEXT_SIZE, size).apply()
        refreshDesktop()
    }

    fun getTextColor(): Int = prefs.getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR)
    fun setTextColor(color: Int) {
        prefs.edit().putInt(KEY_TEXT_COLOR, color).apply()
        refreshDesktop()
    }

    fun getBgColor(): Int = prefs.getInt(KEY_BG_COLOR, DEFAULT_BG_COLOR)
    fun setBgColor(color: Int) {
        prefs.edit().putInt(KEY_BG_COLOR, color).apply()
        refreshDesktop()
    }

    fun getBgAlpha(): Int = prefs.getInt(KEY_BG_ALPHA, DEFAULT_BG_ALPHA)
    fun setBgAlpha(alpha: Int) {
        prefs.edit().putInt(KEY_BG_ALPHA, alpha).apply()
        refreshDesktop()
    }

    private fun refreshDesktop() {
        LyricsOverlayService.refreshSettings()
    }

    fun updateLyrics(line: String) {
        if (isEnabled()) {
            LyricsOverlayService.updateLyrics(line)
        }
    }

    fun toggleLock() {
        val newState = !_isLocked.value
        _isLocked.value = newState
        LyricsOverlayService.toggleLock()
    }

    fun setLocked(locked: Boolean) {
        if (_isLocked.value != locked) {
            _isLocked.value = locked
            LyricsOverlayService.setLocked(locked)
        }
    }

    fun getLockedState(): Boolean = _isLocked.value
}