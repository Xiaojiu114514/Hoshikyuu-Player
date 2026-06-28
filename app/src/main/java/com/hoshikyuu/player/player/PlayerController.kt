package com.hoshikyuu.player.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors

class PlayerController(private val context: Context) {
    private var controller: MediaController? = null
    private val listeners = mutableListOf<Player.Listener>()

    fun connect(onReady: () -> Unit = {}) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            onReady()
        }, MoreExecutors.directExecutor())
    }

    fun play(mediaItems: List<MediaItem>, startIndex: Int = 0) {
        controller?.apply {
            setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            prepare()
            play()
        }
    }

    fun playPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun skipToNext() { controller?.seekToNextMediaItem() }
    fun skipToPrevious() { controller?.seekToPreviousMediaItem() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    val isPlaying: Boolean get() = controller?.isPlaying ?: false
    val currentPosition: Long get() = controller?.currentPosition ?: 0
    val duration: Long get() = controller?.duration ?: 0

    fun addListener(listener: Player.Listener) {
        listeners.add(listener)
        controller?.addListener(listener)
    }

    fun release() {
        listeners.forEach { controller?.removeListener(it) }
        listeners.clear()
        controller?.release()
    }
}
