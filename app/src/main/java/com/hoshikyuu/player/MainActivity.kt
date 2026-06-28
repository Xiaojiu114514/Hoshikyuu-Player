package com.hoshikyuu.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HoshikyuuTheme {
                MainScreen(playerManager = playerManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

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