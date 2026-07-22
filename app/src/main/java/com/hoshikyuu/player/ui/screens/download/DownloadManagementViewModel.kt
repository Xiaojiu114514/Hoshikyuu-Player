package com.hoshikyuu.player.ui.screens.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshikyuu.player.data.repository.DownloadRepository
import com.hoshikyuu.player.domain.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadManagementViewModel @Inject constructor(
    private val downloadRepo: DownloadRepository
) : ViewModel() {

    val downloadedSongs: StateFlow<List<Song>> = downloadRepo.getDownloadedSongs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun removeDownload(songId: String) {
        viewModelScope.launch {
            downloadRepo.removeDownload(songId)
        }
    }

    fun clearAllDownloads(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                downloadRepo.clearAllDownloads()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}