package com.hoshikyuu.player.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
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
import dagger.hilt.android.EntryPointAccessors
import com.hoshikyuu.player.data.repository.AvatarRepositoryEntryPoint
import com.hoshikyuu.player.domain.UiState
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.ui.components.ErrorMessage
import com.hoshikyuu.player.ui.components.LoadingIndicator
import com.hoshikyuu.player.ui.components.SongItem
import com.hoshikyuu.player.ui.navigation.Screen
import com.hoshikyuu.player.ui.theme.BrandMint
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    playerManager: PlayerManager,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val trendingState by viewModel.trendingSongs.collectAsState()
    val recommendedState by viewModel.recommendedSongs.collectAsState()
    val favoriteIds by playerManager.favoriteIds.collectAsState()
    val showMobileWarning by viewModel.showMobileDataWarning.collectAsState()

    val avatarRepo = remember {
        EntryPointAccessors.fromApplication(context, AvatarRepositoryEntryPoint::class.java).avatarRepository()
    }
    val avatarUri by avatarRepo.avatarState.collectAsState()

    if (showMobileWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelMobileDataLoad() },
            title = { Text("使用移动数据") },
            text = {
                Text(
                    "当前正在使用移动数据网络，加载数据将消耗流量。\n\n" +
                            "点击「继续」将允许使用移动网络，\n" +
                            "点击「取消」将只使用本地缓存数据，\n" +
                            "所有网络请求将被禁止。"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmMobileDataLoad() }) {
                    Text("继续", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelMobileDataLoad() }) {
                    Text("取消")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hoshikyuu", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("探索你的音樂之旅", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (avatarUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(avatarUri).crossfade(true).build(),
                            contentDescription = "头像",
                            modifier = Modifier.size(48.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = BrandMint.copy(alpha = 0.2f)) {
                            Icon(Icons.Default.Person, null, tint = BrandMint, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }

            item { SectionHeader("熱門榜單") }
            when (val state = trendingState) {
                is UiState.Loading -> item { LoadingIndicator(modifier = Modifier.height(200.dp)) }
                is UiState.Error -> item { ErrorMessage(state.message) }
                is UiState.Success -> {
                    items(state.data.take(10)) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                viewModel.playSong(song)
                                navController.navigate(Screen.FullPlayer.createRoute(song.id))
                            },
                            trailing = {
                                IconButton(
                                    onClick = {
                                        val success = viewModel.addSongToQueue(song)
                                        scope.launch {
                                            if (success) snackbarHostState.showSnackbar("已加入播放列表：${song.name}")
                                            else snackbarHostState.showSnackbar("歌曲已在播放列表中")
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                            },
                            isFavorite = favoriteIds.contains(song.id),
                            onToggleFavorite = { playerManager.toggleFavorite(song) },
                            onDownload = {
                                viewModel.downloadSong(song) { result ->
                                    scope.launch {
                                        if (result.isSuccess) snackbarHostState.showSnackbar("下载完成：${song.name}")
                                        else snackbarHostState.showSnackbar("下载失败：${result.exceptionOrNull()?.message}")
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
                is UiState.Idle -> {}
            }

            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("新歌榜單")
            }
            when (val state = recommendedState) {
                is UiState.Loading -> item { LoadingIndicator(modifier = Modifier.height(200.dp)) }
                is UiState.Error -> item { ErrorMessage(state.message) }
                is UiState.Success -> {
                    items(state.data.take(10)) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                viewModel.playSong(song)
                                navController.navigate(Screen.FullPlayer.createRoute(song.id))
                            },
                            trailing = {
                                IconButton(
                                    onClick = {
                                        val success = viewModel.addSongToQueue(song)
                                        scope.launch {
                                            if (success) snackbarHostState.showSnackbar("已加入播放列表：${song.name}")
                                            else snackbarHostState.showSnackbar("歌曲已在播放列表中")
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                            },
                            isFavorite = favoriteIds.contains(song.id),
                            onToggleFavorite = { playerManager.toggleFavorite(song) },
                            onDownload = {
                                viewModel.downloadSong(song) { result ->
                                    scope.launch {
                                        if (result.isSuccess) snackbarHostState.showSnackbar("下载完成：${song.name}")
                                        else snackbarHostState.showSnackbar("下载失败：${result.exceptionOrNull()?.message}")
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
                is UiState.Idle -> {}
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}