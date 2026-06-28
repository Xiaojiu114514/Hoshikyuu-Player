package com.hoshikyuu.player

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.hoshikyuu.player.utils.NetworkPreferenceManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HoshikyuuApp : Application() {

    @Inject
    lateinit var networkPreferenceManager: NetworkPreferenceManager

    override fun onCreate() {
        super.onCreate()

        // 读取保存的深色模式设置
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val mode = prefs.getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)

        // 每次启动应用时重置移动网络状态，重新询问用户
        networkPreferenceManager.resetMobileNetworkState()
    }
}