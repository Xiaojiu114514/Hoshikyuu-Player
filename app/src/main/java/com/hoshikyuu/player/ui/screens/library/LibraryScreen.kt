package com.hoshikyuu.player.ui.screens.library

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.ui.components.SongItem
import com.hoshikyuu.player.ui.navigation.Screen
import com.hoshikyuu.player.ui.screens.playlist.PlaylistViewModel
import com.hoshikyuu.player.ui.theme.BrandMint
import kotlinx.coroutines.launch
import java.io.InputStream

data class LibraryMenuItem(
    val title: String,
    val subtitle: String,
    val icon: @Composable () -> Unit,
    val badge: String? = null,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    playerManager: PlayerManager,
    viewModel: LibraryViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val playlists by playlistViewModel.playlists.collectAsState()
    val favoriteSongs by playerManager.favoriteSongs.collectAsState()
    val favoriteIds by playerManager.favoriteIds.collectAsState()
    val playHistory by playerManager.playHistory.collectAsState()
    val queue by playerManager.queue.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                    viewModel.saveAvatar(scaled)
                    bitmap.recycle()
                }
                inputStream?.close()
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("头像选择失败")
                }
            }
        }
    }

    var showSheet by remember { mutableStateOf("") }
    val sheetSongs = when (showSheet) {
        "favorites" -> favoriteSongs
        "history" -> playHistory
        else -> emptyList()
    }
    val sheetTitle = when (showSheet) {
        "favorites" -> "我喜欢的 (${favoriteSongs.size})"
        "history" -> "播放历史 (${playHistory.size})"
        else -> ""
    }

    if (showSheet.isNotEmpty()) {
        ModalBottomSheet(onDismissRequest = { showSheet = "" }) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    Text(
                        sheetTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                    if (sheetSongs.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(sheetSongs) { song ->
                                SongItem(
                                    song = song,
                                    onClick = {
                                        playerManager.play(song)
                                        navController.navigate(Screen.FullPlayer.createRoute(song.id))
                                        showSheet = ""
                                    },
                                    trailing = {
                                        IconButton(
                                            onClick = {
                                                val success = playlistViewModel.addSongToQueueWithDetail(song)
                                                scope.launch {
                                                    if (success) {
                                                        snackbarHostState.showSnackbar("已加入播放列表：${song.name}")
                                                    } else {
                                                        snackbarHostState.showSnackbar("歌曲已在播放列表中")
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlaylistAdd,
                                                "加入播放列表",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    isFavorite = favoriteIds.contains(song.id),
                                    onToggleFavorite = { playerManager.toggleFavorite(song) },
                                    onDownload = {
                                        playlistViewModel.downloadSong(song) { result ->
                                            scope.launch {
                                                if (result.isSuccess) {
                                                    snackbarHostState.showSnackbar("下载完成：${song.name}")
                                                } else {
                                                    snackbarHostState.showSnackbar("下载失败：${result.exceptionOrNull()?.message}")
                                                }
                                            }
                                        }
                                    },
                                    showOverflowMenu = true,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }
        }
    }

    val menuItems = listOf(
        LibraryMenuItem(
            "我喜欢的",
            "已收藏的歌曲",
            icon = { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.error) },
            badge = "${favoriteSongs.size} 首",
            onClick = { showSheet = "favorites" }
        ),
        LibraryMenuItem(
            "我的歌单",
            "自定义播放列表",
            icon = { Icon(Icons.Default.QueueMusic, null, tint = MaterialTheme.colorScheme.primary) },
            badge = "${playlists.size} 个",
            onClick = { navController.navigate(Screen.PlaylistList.route) }
        ),
        // ========== 新增：本地歌曲 ==========
        LibraryMenuItem(
            "本地歌曲",
            "扫描手机中的音乐文件",
            icon = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.tertiary) },
            badge = "",
            onClick = { navController.navigate(Screen.LocalSongs.route) }
        ),
        // ====================================
        LibraryMenuItem(
            "下载管理",
            "离线音乐",
            icon = { Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.tertiary) },
            badge = "",
            onClick = { navController.navigate(Screen.DownloadManagement.route) }
        ),
        LibraryMenuItem(
            "播放历史",
            "最近播放的歌曲",
            icon = { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = { showSheet = "history" }
        ),
        LibraryMenuItem(
            "设定",
            "应用设置",
            icon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { navController.navigate(Screen.Setting.route) }
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .clickable {
                            pickImageLauncher.launch("image/*")
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        color = BrandMint.copy(alpha = 0.2f)
                    ) {
                        if (avatarUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(avatarUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "头像",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                null,
                                tint = BrandMint,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "我的音乐库",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${favoriteSongs.size} 收藏 · ${queue.size} 队列 · ${playHistory.size} 历史",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            items(menuItems) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable(onClick = item.onClick),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                item.icon()
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (item.subtitle.isNotEmpty()) {
                                Text(
                                    item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (item.badge != null && item.badge.isNotEmpty()) {
                            Text(
                                item.badge,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}