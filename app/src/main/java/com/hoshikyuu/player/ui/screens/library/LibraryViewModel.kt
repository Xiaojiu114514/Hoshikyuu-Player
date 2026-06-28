package com.hoshikyuu.player.ui.screens.library

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshikyuu.player.data.repository.AvatarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val avatarRepo: AvatarRepository
) : ViewModel() {

    private val _avatarUri = MutableStateFlow<Uri?>(avatarRepo.getAvatarUri())
    val avatarUri: StateFlow<Uri?> = _avatarUri.asStateFlow()

    fun saveAvatar(bitmap: Bitmap) {
        viewModelScope.launch {
            avatarRepo.saveAvatar(bitmap)
            _avatarUri.value = avatarRepo.getAvatarUri()
        }
    }

    fun deleteAvatar() {
        viewModelScope.launch {
            avatarRepo.deleteAvatar()
            _avatarUri.value = null
        }
    }
}