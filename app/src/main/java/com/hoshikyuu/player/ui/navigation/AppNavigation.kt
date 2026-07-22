package com.hoshikyuu.player.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "探索", Icons.Default.Explore)
    data object Search : Screen("search", "搜寻", Icons.Default.Search)
    data object Library : Screen("library", "我的", Icons.Default.LibraryMusic)
    data object FullPlayer : Screen("player/{songId}", "播放器", Icons.Default.Explore) {
        fun createRoute(songId: String = "") = "player/$songId"
    }
    data object PlaylistList : Screen("playlists", "歌單", Icons.Default.QueueMusic)
    data object PlaylistDetail : Screen("playlist/{playlistId}/{playlistName}", "歌单详情", Icons.Default.QueueMusic) {
        fun createRoute(id: Long, name: String = "") = "playlist/$id/$name"
    }
    data object Setting : Screen("setting", "设定", Icons.Default.Settings)
    data object DownloadManagement : Screen("download_management", "下载管理", Icons.Default.Download)
    // ========== 新增：本地歌曲 ==========
    data object LocalSongs : Screen("local_songs", "本地歌曲", Icons.Default.Folder)
}

val bottomNavItems = listOf(Screen.Home, Screen.Search, Screen.Library)