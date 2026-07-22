package com.hoshikyuu.player.ui.screens.download

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.ui.components.SongItem
import com.hoshikyuu.player.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagementScreen(
    navController: NavController,
    playerManager: PlayerManager,
    viewModel: DownloadManagementViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val favoriteIds by playerManager.favoriteIds.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    if (downloadedSongs.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true }
                        ) {
                            Icon(Icons.Default.Delete, "全部删除")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("确认清空") },
                text = {
                    Text("此操作将删除所有已下载的歌曲文件及数据库记录，确定继续吗？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            viewModel.clearAllDownloads { success ->
                                scope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("已清除所有下载")
                                    } else {
                                        snackbarHostState.showSnackbar("清除失败")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (downloadedSongs.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Download, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("暂无下载的歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(downloadedSongs) { song ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    SongItem(
                        song = song,
                        onClick = {
                            playerManager.play(song)
                            navController.navigate(Screen.FullPlayer.createRoute(song.id))
                        },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (!playerManager.isSongInQueue(song.id)) {
                                            playerManager.addToQueueAfterCurrent(song)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("已加入播放列表：${song.name}")
                                            }
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("歌曲已在播放列表中")
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlaylistAdd,
                                        contentDescription = "添加到队列",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = !menuExpanded },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "更多",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        if (favoriteIds.contains(song.id)) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                        tint = if (favoriteIds.contains(song.id)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(if (favoriteIds.contains(song.id)) "取消收藏" else "收藏")
                                                }
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                playerManager.toggleFavorite(song)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("删除下载")
                                                }
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.removeDownload(song.id)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("已删除：${song.name}")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        isFavorite = favoriteIds.contains(song.id),
                        onToggleFavorite = { /* 已在菜单中处理，此处留空 */ },
                        showOverflowMenu = false,
                        onDownload = null,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}