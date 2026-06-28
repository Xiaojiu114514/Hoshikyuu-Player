package com.hoshikyuu.player.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarRepository @Inject constructor(
    private val context: Context
) {
    private val avatarFile: File
        get() = File(context.filesDir, "avatar.jpg")

    private val _avatarState = MutableStateFlow<Uri?>(getAvatarUri())
    val avatarState: StateFlow<Uri?> = _avatarState.asStateFlow()

    fun saveAvatar(bitmap: Bitmap) {
        avatarFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        _avatarState.value = getAvatarUri()
    }

    fun getAvatarUri(): Uri? {
        return if (avatarFile.exists()) Uri.fromFile(avatarFile) else null
    }

    // 重命名以避免与属性 getter 冲突
    fun getAvatarFileOrNull(): File? = if (avatarFile.exists()) avatarFile else null

    fun deleteAvatar() {
        avatarFile.delete()
        _avatarState.value = null
    }

    fun hasAvatar(): Boolean = avatarFile.exists()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AvatarRepositoryEntryPoint {
    fun avatarRepository(): AvatarRepository
}