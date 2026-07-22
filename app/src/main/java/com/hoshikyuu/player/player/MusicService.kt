@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.hoshikyuu.player.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.hoshikyuu.player.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var playerManager: PlayerManager

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    companion object {
        private const val TAG = "MusicService"
        private const val NOTIFICATION_CHANNEL_ID = "music_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        // 只创建通知渠道，不进行其他耗时操作
        createNotificationChannel()
        Log.d(TAG, "MusicService onCreate")
    }

    // 真正初始化放在 onStartCommand 中，避免卡死主线程
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理通知栏按钮点击（来自自定义通知按钮）
        when (intent?.action) {
            "action_play_pause" -> playerManager.togglePlay()
            "action_next" -> playerManager.skipNext()
            "action_previous" -> playerManager.skipPrevious()
        }

        // 初始化播放器和 MediaSession（只在第一次调用时初始化）
        if (!::player.isInitialized) {
            player = playerManager.getPlayer()   // ✅ 使用 getPlayer()
            mediaSession = MediaSession.Builder(this, player).build()

            // 监听播放器事件更新通知
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    updateNotification()
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    updateNotification()
                }
                override fun onPlaybackStateChanged(state: Int) {
                    updateNotification()
                }
            })
        }

        // 启动前台服务（必须）
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession.release()
        super.onDestroy()
    }

    // ===== 通知相关 =====
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放控制"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val notification = buildNotification()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val song = playerManager.currentSong.value
        val isPlaying = playerManager.isPlaying.value
        val title = song?.name ?: "未知歌曲"
        val artist = song?.artist ?: "未知歌手"

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 通知栏控制按钮的 PendingIntent
        val playPauseIntent = Intent(this, MusicService::class.java).apply { action = "action_play_pause" }
        val playPausePending = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, MusicService::class.java).apply { action = "action_next" }
        val nextPending = PendingIntent.getService(
            this, 2, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MusicService::class.java).apply { action = "action_previous" }
        val prevPending = PendingIntent.getService(
            this, 3, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play) // 请替换为自己的图标
            .setContentIntent(openPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(0)

        builder.addAction(android.R.drawable.ic_media_previous, "上一首", prevPending)
        builder.addAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "暂停" else "播放",
            playPausePending
        )
        builder.addAction(android.R.drawable.ic_media_next, "下一首", nextPending)

        return builder.build()
    }
}