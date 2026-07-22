package com.hoshikyuu.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hoshikyuu.player.player.DesktopLyricsManager
import com.hoshikyuu.player.player.MusicService
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.ui.components.MiniPlayerBar
import com.hoshikyuu.player.ui.components.QueueSheet
import com.hoshikyuu.player.ui.navigation.AppNavGraph
import com.hoshikyuu.player.ui.navigation.Screen
import com.hoshikyuu.player.ui.navigation.bottomNavItems
import com.hoshikyuu.player.ui.theme.HoshikyuuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerManager: PlayerManager
    @Inject lateinit var desktopLyricsManager: DesktopLyricsManager

    // 统一的权限请求启动器（处理多个权限）
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (permission, granted) ->
            if (granted) {
                Log.d("MainActivity", "权限已授予: $permission")
            } else {
                Log.w("MainActivity", "权限被拒绝: $permission")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 请求所需的运行时权限
        requestNecessaryPermissions()

        // 启动音乐服务
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)

        // 恢复桌面歌词状态
        if (desktopLyricsManager.isEnabled()) {
            desktopLyricsManager.setEnabled(true)
        }

        setContent {
            HoshikyuuTheme {
                MainScreen(playerManager = playerManager)
            }
        }
    }

    /**
     * 根据 Android 版本请求必要的权限
     */
    private fun requestNecessaryPermissions() {
        val permissions = mutableListOf<String>()

        // 1. 通知权限（Android 13+ 必需）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. 存储/媒体权限（用于本地歌曲扫描）
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+：使用 READ_MEDIA_AUDIO 读取音频文件
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                }
                // 如需读取封面图片，可增加 READ_MEDIA_IMAGES
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-12：使用 READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            // Android 6 以下无需运行时权限
        }

        // 如果权限列表不为空，发起请求
        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}

// MainScreen 内容保持不变（未修改）
@Composable
fun MainScreen(playerManager: PlayerManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentSong by playerManager.currentSong.collectAsState()
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    val showMiniPlayer = currentRoute in bottomNavItems.map { it.route } && currentSong != null

    var showQueueSheet by remember { mutableStateOf(false) }
    if (showQueueSheet) {
        QueueSheet(
            playerManager = playerManager,
            onSongClick = { idx ->
                playerManager.skipToIndex(idx)
                showQueueSheet = false
            },
            onDismiss = { showQueueSheet = false }
        )
    }

    Scaffold(
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    MiniPlayerBar(
                        playerManager = playerManager,
                        onClick = {
                            navController.navigate(Screen.FullPlayer.createRoute(currentSong?.id ?: "")) {
                                launchSingleTop = true
                            }
                        },
                        onQueueClick = { showQueueSheet = true }
                    )
                }

                AnimatedVisibility(visible = showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = NavigationBarDefaults.Elevation
                    ) {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        AppNavGraph(
            navController = navController,
            playerManager = playerManager,
            modifier = Modifier.padding(paddingValues)
        )
    }
}