package com.hoshikyuu.player.ui.screens.local

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hoshikyuu.player.domain.UiState
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.ui.components.LoadingIndicator
import com.hoshikyuu.player.ui.components.SongItem
import com.hoshikyuu.player.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSongsScreen(
    navController: NavController,
    playerManager: PlayerManager,
    viewModel: LocalSongViewModel = hiltViewModel()
) {
    val songsState by viewModel.songsState.collectAsState()
    val favoriteIds by playerManager.favoriteIds.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadLocalSongs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本地歌曲") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (songsState) {
                is UiState.Loading -> {
                    LoadingIndicator()
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = (songsState as UiState.Error).message)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadLocalSongs() }) {
                            Text("重新扫描")
                        }
                    }
                }
                is UiState.Success -> {
                    val songs = (songsState as UiState.Success).data
                    if (songs.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("未找到本地歌曲")
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "请将 mp3 文件放入\n/download/HoshikyuuPlayer/",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadLocalSongs() }) {
                                Text("重新扫描")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(songs) { song ->
                                SongItem(
                                    song = song,
                                    onClick = {
                                        viewModel.playSong(song)
                                        navController.navigate(Screen.FullPlayer.createRoute(song.id))
                                    },
                                    isFavorite = favoriteIds.contains(song.id),
                                    onToggleFavorite = { playerManager.toggleFavorite(song) },
                                    trailing = {
                                        IconButton(
                                            onClick = {
                                                viewModel.addToQueue(song)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("已加入播放列表：${song.name}")
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlaylistAdd,
                                                contentDescription = "加入播放列表",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    onDownload = {
                                        // 本地歌曲无需下载
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                is UiState.Idle -> {}
            }
        }
    }
}