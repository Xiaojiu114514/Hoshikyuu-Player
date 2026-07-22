package com.hoshikyuu.player.player

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.hoshikyuu.player.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class LyricsOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var lyricsTextView: TextView? = null
    private var buttonContainer: LinearLayout? = null
    private var btnPlayPause: ImageButton? = null   // 保存引用
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isLocked = false
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Inject
    lateinit var desktopLyricsManager: DesktopLyricsManager

    @Inject
    lateinit var playerManager: PlayerManager

    companion object {
        private var instance: LyricsOverlayService? = null
        private val mainHandler = Handler(Looper.getMainLooper())

        fun updateLyrics(lyrics: String) {
            instance?.updateTextView(lyrics)
        }

        fun setLocked(locked: Boolean) {
            instance?.isLocked = locked
            instance?.updateLockState()
        }

        fun toggleLock() {
            instance?.isLocked = !(instance?.isLocked ?: false)
            instance?.updateLockState()
        }

        fun refreshSettings() {
            instance?.applySettings()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayView()

        // 监听歌词更新
        serviceScope.launch {
            playerManager.currentLyricsLine.collect { line ->
                updateTextView(line)
            }
        }

        // ========== 监听播放状态，更新暂停/播放按钮 ==========
        serviceScope.launch {
            playerManager.isPlaying.collect { isPlaying ->
                mainHandler.post {
                    updatePlayPauseIcon(isPlaying)
                }
            }
        }
        // ====================================================

        updateTextView(playerManager.currentLyricsLine.value)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) createOverlayView()
        else overlayView?.visibility = View.VISIBLE
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        lyricsTextView = null
        buttonContainer = null
        btnPlayPause = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createOverlayView() {
        try {
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            overlayView = inflater.inflate(R.layout.view_lyrics_overlay, null)
            lyricsTextView = overlayView?.findViewById(R.id.tv_lyrics)
            buttonContainer = overlayView?.findViewById(R.id.button_container)

            lyricsTextView?.apply {
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1
                isSelected = true
                setOnTouchListener { _, _ -> false }
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                setTextIsSelectable(false)
            }

            val btnPrev = overlayView?.findViewById<ImageButton>(R.id.btn_prev)
            btnPlayPause = overlayView?.findViewById(R.id.btn_play_pause)
            val btnNext = overlayView?.findViewById<ImageButton>(R.id.btn_next)
            val btnLock = overlayView?.findViewById<ImageButton>(R.id.btn_lock)
            val btnClose = overlayView?.findViewById<ImageButton>(R.id.btn_close)

            btnPrev?.setOnClickListener { playerManager.skipPrevious() }
            btnPlayPause?.setOnClickListener {
                playerManager.togglePlay()
                // 图标会在监听器中自动更新
            }
            btnNext?.setOnClickListener { playerManager.skipNext() }
            btnLock?.setOnClickListener {
                isLocked = !isLocked
                updateLockState()
                // 同步到 DesktopLyricsManager
                desktopLyricsManager.setLocked(isLocked)
            }
            btnClose?.setOnClickListener {
                desktopLyricsManager.setEnabled(false)
            }

            // 初始更新图标（根据当前播放状态）
            updatePlayPauseIcon(playerManager.isPlaying.value)

            if (lyricsTextView?.text.isNullOrEmpty()) {
                lyricsTextView?.text = "当前无正在播放音乐"
            }

            applySettings()

            overlayView?.setOnTouchListener { view, event ->
                if (isLocked) {
                    return@setOnTouchListener false
                }

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val location = IntArray(2)
                        buttonContainer?.getLocationOnScreen(location)
                        if (buttonContainer != null) {
                            val rect = Rect(
                                location[0],
                                location[1],
                                location[0] + buttonContainer!!.width,
                                location[1] + buttonContainer!!.height
                            )
                            val x = event.rawX.toInt()
                            val y = event.rawY.toInt()
                            if (rect.contains(x, y)) {
                                return@setOnTouchListener false
                            }
                        }
                        isDragging = true
                        val params = overlayView?.layoutParams as WindowManager.LayoutParams
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isDragging) {
                            val params = overlayView?.layoutParams as WindowManager.LayoutParams
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(overlayView, params)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                        true
                    }
                    else -> false
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 100
            }

            windowManager.addView(overlayView, params)
            updateLockState()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun applySettings() {
        lyricsTextView?.apply {
            val alpha = desktopLyricsManager.getAlpha()
            setTextColor(desktopLyricsManager.getTextColor())
            textSize = desktopLyricsManager.getTextSize().toFloat()
            val bgColor = desktopLyricsManager.getBgColor()
            val bgAlpha = desktopLyricsManager.getBgAlpha()
            setBackgroundColor(Color.argb(bgAlpha, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)))
            this.alpha = alpha / 255f
            if (text.isNullOrBlank()) {
                text = "当前无正在播放音乐"
            }
        }
    }

    private fun updateTextView(lyrics: String) {
        mainHandler.post {
            lyricsTextView?.apply {
                val displayText = if (lyrics.isBlank()) "当前无正在播放音乐" else lyrics
                if (text.toString() != displayText) {
                    text = displayText
                    isSelected = true
                }
            }
        }
    }

    // 修改为接收 isPlaying 参数
    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        btnPlayPause?.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateLockState() {
        val view = overlayView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return

        if (isLocked) {
            buttonContainer?.visibility = View.GONE
            lyricsTextView?.setBackgroundColor(Color.TRANSPARENT)
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            windowManager.updateViewLayout(view, params)
        } else {
            buttonContainer?.visibility = View.VISIBLE
            applySettings()
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            windowManager.updateViewLayout(view, params)
        }
    }
}