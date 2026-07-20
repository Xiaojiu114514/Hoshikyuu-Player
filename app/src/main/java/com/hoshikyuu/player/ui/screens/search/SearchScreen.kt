package com.hoshikyuu.player.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hoshikyuu.player.domain.UiState
import com.hoshikyuu.player.player.PlayerManager
import com.hoshikyuu.player.ui.components.ErrorMessage
import com.hoshikyuu.player.ui.components.LoadingIndicator
import com.hoshikyuu.player.ui.components.SongItem
import com.hoshikyuu.player.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    playerManager: PlayerManager,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val query by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val history by viewModel.searchHistory.collectAsState()
    val hotSearches by viewModel.hotSearches.collectAsState()
    val favoriteIds by playerManager.favoriteIds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("搜尋歌曲、歌手、專輯...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                Row {
                    if (query.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.onSearch(query) },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "搜索",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("搜索", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardActions = KeyboardActions(
                onSearch = { viewModel.onSearch(query) }
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                searchState is UiState.Idle -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (history.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "搜索歷史",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = { viewModel.clearHistory() }) {
                                        Text("清除", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(history) { item ->
                                        SuggestionChip(
                                            onClick = { viewModel.onHistoryClick(item) },
                                            label = { Text(item) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "熱門搜索",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(hotSearches) { item ->
                                    SuggestionChip(
                                        onClick = { viewModel.onSearch(item) },
                                        label = { Text(item) }
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    when (val state = searchState) {
                        is UiState.Loading -> LoadingIndicator()
                        is UiState.Error -> ErrorMessage(state.message)
                        is UiState.Success -> {
                            if (state.data.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "沒有找到相關結果",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "搜索結果 (${state.data.size})",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            AssistChip(
                                                onClick = { viewModel.onSearch(query) },
                                                label = { Text("重新搜索", style = MaterialTheme.typography.labelSmall) },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                            )
                                        }
                                    }
                                    items(state.data) { song ->
                                        SongItem(
                                            song = song,
                                            onClick = {
                                                viewModel.playSong(song)
                                                navController.navigate(Screen.FullPlayer.createRoute(song.id))
                                            },
                                            trailing = {
                                                IconButton(
                                                    onClick = {
                                                        val success = viewModel.addSongToQueueWithSong(song)
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
                                                        contentDescription = "加入播放列表",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            isFavorite = favoriteIds.contains(song.id),
                                            onToggleFavorite = { playerManager.toggleFavorite(song) },
                                            onDownload = {
                                                viewModel.downloadSong(song) { result ->
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
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                    item { Spacer(modifier = Modifier.height(80.dp)) }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}