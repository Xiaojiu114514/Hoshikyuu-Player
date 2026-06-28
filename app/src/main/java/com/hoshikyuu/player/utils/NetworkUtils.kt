package com.hoshikyuu.player.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(
    private val context: Context
) {

    /**
     * 检查当前是否为移动数据网络（蜂窝网络）
     */
    fun isMobileData(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val activeNetwork = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return activeNetwork.type == ConnectivityManager.TYPE_MOBILE
        }
    }

    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val activeNetwork = connectivityManager.activeNetworkInfo ?: return false
            return activeNetwork.isConnected
        }
    }

    /**
     * 检查当前是否为 WiFi 网络
     */
    fun isWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val activeNetwork = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return activeNetwork.type == ConnectivityManager.TYPE_WIFI
        }
    }
}