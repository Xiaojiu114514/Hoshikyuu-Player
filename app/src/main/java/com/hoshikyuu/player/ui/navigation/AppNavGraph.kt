package com.hoshikyuu.player.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.ui.screens.download.DownloadManagementScreen
import com.hoshikyuu.player.ui.screens.home.HomeScreen
import com.hoshikyuu.player.ui.screens.library.LibraryScreen
import com.hoshikyuu.player.ui.screens.player.FullPlayerScreen
import com.hoshikyuu.player.ui.screens.playlist.PlaylistDetailScreen
import com.hoshikyuu.player.ui.screens.playlist.PlaylistListScreen
import com.hoshikyuu.player.ui.screens.search.SearchScreen
import com.hoshikyuu.player.ui.screens.setting.SettingScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    playerManager: PlayerManager,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                playerManager = playerManager
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                navController = navController,
                playerManager = playerManager
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                navController = navController,
                playerManager = playerManager
            )
        }
        composable(Screen.FullPlayer.route) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            FullPlayerScreen(
                songId = songId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PlaylistList.route) {
            PlaylistListScreen(navController = navController)
        }
        composable(
            Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
                navArgument("playlistName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStack ->
            PlaylistDetailScreen(
                playlistId = backStack.arguments?.getLong("playlistId") ?: 0,
                playlistName = backStack.arguments?.getString("playlistName") ?: "",
                navController = navController,
                playerManager = playerManager
            )
        }
        // 新增
        composable(Screen.Setting.route) {
            SettingScreen(navController = navController)
        }
        composable(Screen.DownloadManagement.route) {
            DownloadManagementScreen(
                navController = navController,
                playerManager = playerManager
            )
        }
    }
}