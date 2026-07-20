package com.hoshikyuu.player.ui.screens.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hoshikyuu.player.domain.Song  // 添加导入
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.ui.components.SongItem
import com.hoshikyuu.player.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String = "",
    navController: NavController,
    playerManager: PlayerManager,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val songs by viewModel.getSongs(playlistId).collectAsState()
    val favoriteIds by playerManager.favoriteIds.collectAsState()

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (playlistName.isNotBlank()) playlistName else "歌單詳情",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { pad ->
        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("歌單為空", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(songs) { entity ->
                    val song = Song(
                        id = entity.songId,
                        name = entity.songName,
                        album = entity.songAlbum,
                        artist = entity.songArtist,
                        coverUrl = entity.songCoverUrl,
                        mp3Url = entity.songMp3Url,
                        lrc = "",  // 从实体中可能没有，可留空
                        source = entity.source
                    )
                    SongItem(
                        song = song,
                        onClick = {
                            viewModel.playSongWithDetail(song)
                            navController.navigate(Screen.FullPlayer.createRoute(song.id))
                        },
                        trailing = {
                            Row {
                                IconButton(
                                    onClick = {
                                        val success = viewModel.addSongToQueueWithDetail(song)
                                        scope.launch {
                                            if (success) snackbarHostState.showSnackbar("已加入播放列表：${song.name}")
                                            else snackbarHostState.showSnackbar("歌曲已在播放列表中")
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.removeSongFromPlaylist(playlistId, song.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
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
                        showOverflowMenu = true,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(16.dp))
}