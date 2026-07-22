package com.hoshikyuu.player.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hoshikyuu.player.domain.UiState
import com.hoshikyuu.player.player.RepeatMode
import com.hoshikyuu.player.ui.components.ErrorMessage
import com.hoshikyuu.player.ui.screens.playlist.PlaylistViewModel
import com.hoshikyuu.player.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp

@Composable
fun FullPlayerScreen(
    songId: String,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val songState by viewModel.songState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val durationMillis by viewModel.duration.collectAsState()
    val lyricLines by viewModel.lyricLines.collectAsState()
    val errorMessage by viewModel.playerManager.errorMessage.collectAsState()

    // 桌面歌词状态
    val desktopLyricsEnabled by viewModel.desktopLyricsEnabled.collectAsState()
    val desktopLyricsLocked by viewModel.desktopLyricsLocked.collectAsState()

    var isLyricsFullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val song = (songState as? UiState.Success)?.data
    val context = LocalContext.current

    val bgGradient = remember(song?.coverUrl) {
        Brush.verticalGradient(
            colors = listOf(
                DefaultAlbumGradientStart,
                DefaultAlbumGradientEnd,
                Color(0xFF0D1117)
            )
        )
    }

    var showPlaylistDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        if (song?.coverUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        if (!isLyricsFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "收起",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // ========== 新增桌面歌词开关按钮 ==========
                    IconButton(
                        onClick = { viewModel.toggleDesktopLyrics() }
                    ) {
                        when {
                            // 锁定状态：显示解锁图标
                            desktopLyricsLocked -> {
                                Icon(
                                    Icons.Default.LockOpen,
                                    contentDescription = "解锁桌面歌词",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            else -> {
                                // 未锁定：显示“词”字，颜色根据启用状态变化
                                Text(
                                    text = "词",
                                    color = if (desktopLyricsEnabled) Color.White else Color.Gray,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .wrapContentSize(Alignment.Center)
                                )
                            }
                        }
                    }
                    // ========================================

                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("加入歌單") },
                                onClick = {
                                    showPlaylistDialog = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("下載") },
                                onClick = {
                                    viewModel.downloadCurrentSong { result ->
                                        scope.launch {
                                            if (result.isSuccess) {
                                                snackbarHostState.showSnackbar("下载完成：${song?.name ?: "歌曲"}")
                                            } else {
                                                val msg = result.exceptionOrNull()?.message ?: "下载失败"
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    }
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("分享") },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.3f))

                when (songState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is UiState.Error -> {
                        ErrorMessage((songState as UiState.Error).message)
                    }
                    is UiState.Idle -> {}
                    is UiState.Success -> {
                        val currentSong = (songState as UiState.Success).data

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentSong.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "專輯封面",
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(280.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = currentSong.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentSong.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Slider(
                                value = progress,
                                onValueChange = { viewModel.seekTo(it) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = BrandMint,
                                    activeTrackColor = BrandMint,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration((progress * durationMillis).toLong()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = formatDuration(durationMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 歌词区域
                        val currentPosMs = (progress * durationMillis).toLong()
                        val currentLyricIdx = viewModel.getLyricIndex(currentPosMs)
                        val lyricsListState = rememberLazyListState()

                        LaunchedEffect(currentLyricIdx) {
                            if (currentLyricIdx > 0) {
                                lyricsListState.animateScrollToItem(currentLyricIdx)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clickable { isLyricsFullscreen = true }
                        ) {
                            if (lyricLines.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp),
                                    state = lyricsListState,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    itemsIndexed(lyricLines) { idx, line ->
                                        Text(
                                            text = line.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (idx == currentLyricIdx) Color.White
                                            else Color.White.copy(alpha = 0.35f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "暫無歌詞\n點擊此處展開",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.3f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(0.5f))

                        // 播放控制
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = { viewModel.toggleFavorite(currentSong) }) {
                                Icon(
                                    imageVector = if (currentSong.id in favoriteIds) Icons.Default.Favorite
                                    else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "收藏",
                                    tint = if (currentSong.id in favoriteIds) HeartRed else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.skipPrevious() }) {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    contentDescription = "上一首",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                color = BrandMint
                            ) {
                                IconButton(onClick = { viewModel.togglePlay() }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "暫停" else "播放",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.skipNext() }) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = "下一首",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val current = repeatMode
                                    val newMode = when (current) {
                                        RepeatMode.NONE -> RepeatMode.ALL
                                        RepeatMode.ALL -> RepeatMode.ONE
                                        RepeatMode.ONE -> RepeatMode.NONE
                                    }
                                    viewModel.playerManager.setRepeatMode(newMode)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Repeat,
                                        contentDescription = when (repeatMode) {
                                            RepeatMode.ONE -> "單曲循環"
                                            RepeatMode.ALL -> "列表循環"
                                            RepeatMode.NONE -> "順序播放"
                                        },
                                        tint = if (repeatMode != RepeatMode.NONE) BrandMint else Color.Gray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    if (repeatMode == RepeatMode.ONE) {
                                        Text(
                                            "1",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when (repeatMode) {
                                    RepeatMode.NONE -> "順序播放"
                                    RepeatMode.ALL -> "列表循環"
                                    RepeatMode.ONE -> "單曲循環"
                                },
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // 全屏歌词（保持不变）
        if (isLyricsFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DefaultAlbumGradientStart,
                                DefaultAlbumGradientEnd,
                                Color(0xFF0D1117)
                            )
                        )
                    )
            ) {
                if (song?.coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(song.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(40.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                }

                IconButton(
                    onClick = { isLyricsFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                val currentPosMs = (progress * durationMillis).toLong()
                val currentLyricIdx = viewModel.getLyricIndex(currentPosMs)
                val fullscreenLyricsState = rememberLazyListState()

                LaunchedEffect(currentLyricIdx) {
                    if (currentLyricIdx > 0) {
                        fullscreenLyricsState.animateScrollToItem(currentLyricIdx)
                    }
                }

                if (lyricLines.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 72.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
                        state = fullscreenLyricsState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        itemsIndexed(lyricLines) { idx, line ->
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (idx == currentLyricIdx) Color.White
                                else Color.White.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暫無歌詞",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // 加入歌单对话框（保持不变）
        if (showPlaylistDialog) {
            val plVm: PlaylistViewModel = hiltViewModel()
            val playlistList by plVm.playlists.collectAsState()
            AlertDialog(
                onDismissRequest = { showPlaylistDialog = false },
                title = { Text("選擇歌單") },
                text = {
                    if (playlistList.isEmpty()) {
                        Text("暫無歌單，請先在「我的」頁面建立")
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            itemsIndexed(playlistList) { _, playlist ->
                                TextButton(
                                    onClick = {
                                        showPlaylistDialog = false
                                        scope.launch {
                                            try {
                                                val success = plVm.addCurrentSongToPlaylist(playlist.id)
                                                if (success) {
                                                    snackbarHostState.showSnackbar("已加入歌單「${playlist.name}」")
                                                } else {
                                                    snackbarHostState.showSnackbar("歌曲已在歌單中")
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("加入失敗：${e.message}")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(playlist.name, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlaylistDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}