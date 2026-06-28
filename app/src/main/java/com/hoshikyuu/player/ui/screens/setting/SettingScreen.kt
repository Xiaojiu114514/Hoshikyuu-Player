package com.hoshikyuu.player.ui.screens.setting

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hoshikyuu.player.data.repository.SettingRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    navController: NavController,
    viewModel: SettingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val darkMode by viewModel.darkMode.collectAsState()
    val fileNameFormat by viewModel.fileNameFormat.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val dataSize by viewModel.dataSize.collectAsState()

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设定") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingItem(
                    title = "清除缓存",
                    subtitle = formatSize(cacheSize),
                    onClick = { showClearCacheDialog = true }
                )
            }

            item {
                SettingItem(
                    title = "清除数据",
                    subtitle = formatSize(dataSize),
                    onClick = { showClearDataDialog = true }
                )
            }

            item {
                ExpandableSettingGroup(
                    title = "深色模式",
                    currentValue = when (darkMode) {
                        AppCompatDelegate.MODE_NIGHT_NO -> "保持白色"
                        AppCompatDelegate.MODE_NIGHT_YES -> "保持深色"
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "跟随系统"
                        else -> "跟随系统"
                    },
                    options = listOf(
                        "保持白色" to AppCompatDelegate.MODE_NIGHT_NO,
                        "保持深色" to AppCompatDelegate.MODE_NIGHT_YES,
                        "跟随系统" to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    ),
                    onOptionSelected = { mode ->
                        viewModel.setDarkMode(mode)
                        (context as? Activity)?.recreate()
                    }
                )
            }

            item {
                ExpandableSettingGroup(
                    title = "下载文件名格式",
                    currentValue = if (fileNameFormat == SettingRepository.FORMAT_SONG_ARTIST) "歌曲名-歌手" else "歌手-歌曲名",
                    options = listOf(
                        "歌曲名-歌手" to SettingRepository.FORMAT_SONG_ARTIST,
                        "歌手-歌曲名" to SettingRepository.FORMAT_ARTIST_SONG
                    ),
                    onOptionSelected = { format ->
                        viewModel.setFileNameFormat(format)
                    }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // 清除缓存确认对话框
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("确认清除缓存") },
            text = {
                Text(
                    "此操作将删除所有已缓存的歌曲文件（约 ${formatSize(cacheSize)}），\n" +
                            "但不会删除您下载的歌曲。\n\n" +
                            "确定继续吗？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.clearCache { success ->
                            if (success) {
                                // 提示成功
                            }
                        }
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 清除数据确认对话框
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("确认清除数据") },
            text = {
                Text(
                    "此操作将删除所有本地数据，包括：\n" +
                            "• 收藏列表\n" +
                            "• 播放历史\n" +
                            "• 下载的歌曲\n" +
                            "• 应用设置\n" +
                            "• 歌曲缓存\n\n" +
                            "此操作不可恢复，确定继续吗？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.clearData { success ->
                            if (success) {
                                // 提示重启
                            }
                        }
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExpandableSettingGroup(
    title: String,
    currentValue: String,
    options: List<Pair<String, Int>>,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentValue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                }
            }
            if (expanded) {
                options.forEach { (label, value) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(value)
                                expanded = false
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}