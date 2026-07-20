package com.hoshikyuu.player.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.ui.screens.playlist.PlaylistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    playerManager: PlayerManager,
    onSongClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val queue by playerManager.queue.collectAsState()
    val currentIdx by playerManager.currentIndex.collectAsState()
    val favoriteIds by playerManager.favoriteIds.collectAsState()
    val playlistViewModel: PlaylistViewModel = hiltViewModel()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (queue.isEmpty()) "播放列表" else "播放列表 (${queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "關閉")
                }
            }

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "播放列表為空",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    itemsIndexed(queue) { idx, song ->
                        SongItem(
                            song = song,
                            onClick = { onSongClick(idx) },
                            trailing = {
                                IconButton(
                                    onClick = { playerManager.removeFromQueue(idx) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "移除",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            isPlaying = (idx == currentIdx),
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
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}