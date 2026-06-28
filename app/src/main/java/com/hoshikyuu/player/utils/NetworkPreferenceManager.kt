package com.hoshikyuu.player.utils

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkPreferenceManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("network_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALLOW_MOBILE_NETWORK = "allow_mobile_network"
        private const val KEY_SHOW_MOBILE_WARNING = "show_mobile_warning"
    }

    /**
     * 是否允许使用移动网络
     */
    fun isMobileNetworkAllowed(): Boolean {
        return prefs.getBoolean(KEY_ALLOW_MOBILE_NETWORK, false)
    }

    /**
     * 设置是否允许使用移动网络
     */
    fun setMobileNetworkAllowed(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_MOBILE_NETWORK, allowed).apply()
    }

    /**
     * 是否应该显示移动网络警告
     */
    fun shouldShowMobileWarning(): Boolean {
        return prefs.getBoolean(KEY_SHOW_MOBILE_WARNING, true)
    }

    /**
     * 设置是否显示移动网络警告（用户选择后关闭）
     */
    fun setShowMobileWarning(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_MOBILE_WARNING, show).apply()
    }

    /**
     * 重置移动网络状态（应用重启时调用）
     */
    fun resetMobileNetworkState() {
        prefs.edit()
            .putBoolean(KEY_ALLOW_MOBILE_NETWORK, false)
            .putBoolean(KEY_SHOW_MOBILE_WARNING, true)
            .apply()
    }

    /**
     * 检查是否允许进行网络请求
     */
    fun isNetworkRequestAllowed(networkUtils: NetworkUtils): Boolean {
        if (isMobileNetworkAllowed()) {
            return true
        }
        return !networkUtils.isMobileData()
    }

    /**
     * 检查网络是否被强制禁用（用户点击取消后）
     */
    fun isNetworkBlocked(): Boolean {
        return !isMobileNetworkAllowed()
    }
}