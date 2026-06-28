package com.hoshikyuu.player.ui.screens.setting

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshikyuu.player.data.local.dao.SongCacheDao
import com.hoshikyuu.player.data.repository.AvatarRepository
import com.hoshikyuu.player.data.repository.DownloadRepository
import com.hoshikyuu.player.data.repository.SettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingRepo: SettingRepository,
    private val downloadRepo: DownloadRepository,
    private val avatarRepo: AvatarRepository,
    private val songCacheDao: SongCacheDao
) : ViewModel() {

    private val _darkMode = MutableStateFlow(settingRepo.getDarkMode())
    val darkMode: StateFlow<Int> = _darkMode.asStateFlow()

    private val _fileNameFormat = MutableStateFlow(settingRepo.getFileNameFormat())
    val fileNameFormat: StateFlow<Int> = _fileNameFormat.asStateFlow()

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    private val _dataSize = MutableStateFlow(0L)
    val dataSize: StateFlow<Long> = _dataSize.asStateFlow()

    init {
        updateSizes()
    }

    fun setDarkMode(mode: Int) {
        settingRepo.setDarkMode(mode)
        _darkMode.value = mode
    }

    fun setFileNameFormat(format: Int) {
        settingRepo.setFileNameFormat(format)
        _fileNameFormat.value = format
    }

    fun clearCache(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val cacheDir = File(context.filesDir, "cache/songs")
                if (cacheDir.exists()) cacheDir.deleteRecursively()
                songCacheDao.clearAllCache()
                updateSizes()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun clearData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                downloadRepo.clearAllDownloads()
                val cacheDir = File(context.filesDir, "cache/songs")
                if (cacheDir.exists()) cacheDir.deleteRecursively()
                songCacheDao.clearAllCache()
                context.getDatabasePath("hoshikyuu_player.db").delete()
                context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().apply()
                avatarRepo.deleteAvatar()
                val songsDir = File(context.filesDir, "songs")
                if (songsDir.exists()) songsDir.deleteRecursively()
                val cacheSongsDir = File(context.filesDir, "cache/songs")
                if (cacheSongsDir.exists()) cacheSongsDir.deleteRecursively()
                songsDir.mkdirs()
                cacheSongsDir.mkdirs()
                updateSizes()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    private fun updateSizes() {
        viewModelScope.launch {
            _cacheSize.value = calculateCacheSize()
            _dataSize.value = calculateDataSize()
        }
    }

    private fun calculateCacheSize(): Long {
        val cacheDir = File(context.filesDir, "cache/songs")
        return if (cacheDir.exists()) {
            cacheDir.walk().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    private fun calculateDataSize(): Long {
        val songsDir = File(context.filesDir, "songs")
        val cacheDir = File(context.filesDir, "cache/songs")
        val dbFile = context.getDatabasePath("hoshikyuu_player.db")
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        var total = 0L
        if (songsDir.exists()) total += songsDir.walk().filter { it.isFile }.sumOf { it.length() }
        if (cacheDir.exists()) total += cacheDir.walk().filter { it.isFile }.sumOf { it.length() }
        if (dbFile.exists()) total += dbFile.length()
        if (prefsDir.exists()) total += prefsDir.walk().filter { it.isFile }.sumOf { it.length() }
        return total
    }
}