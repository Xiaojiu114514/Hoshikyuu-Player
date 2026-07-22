package com.hoshikyuu.player.ui.screens.setting

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hoshikyuu.player.data.repository.SettingRepository
import com.hoshikyuu.player.utils.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    navController: NavController,
    viewModel: SettingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val darkMode by viewModel.darkMode.collectAsState()
    val fileNameFormat by viewModel.fileNameFormat.collectAsState()
    val saveLyrics by viewModel.saveLyrics.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val dataSize by viewModel.dataSize.collectAsState()

    val desktopLyricsEnabled by viewModel.desktopLyricsEnabled.collectAsState()
    val desktopTextSize by viewModel.desktopTextSize.collectAsState()
    val desktopTextColor by viewModel.desktopTextColor.collectAsState()
    val desktopBgColor by viewModel.desktopBgColor.collectAsState()
    val desktopBgAlpha by viewModel.desktopBgAlpha.collectAsState()

    val previewText = "歌词预览 - Hoshikyuu"

    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("") }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf("desktop_text") }

    LaunchedEffect(desktopLyricsEnabled) {
        if (desktopLyricsEnabled && !PermissionHelper.hasOverlayPermission(context)) {
            showPermissionDialog = true
            permissionType = "overlay"
        }
    }

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
            // ===== 原有设置 =====
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("下载时保存歌词", modifier = Modifier.weight(1f))
                    Switch(
                        checked = saveLyrics,
                        onCheckedChange = { viewModel.setSaveLyrics(it) }
                    )
                }
            }

            // ===== 桌面歌词 =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("桌面歌词", modifier = Modifier.weight(1f))
                    Switch(
                        checked = desktopLyricsEnabled,
                        onCheckedChange = { viewModel.setDesktopLyricsEnabled(it) }
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("桌面歌词字体大小: ${desktopTextSize}sp", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = desktopTextSize.toFloat(),
                        onValueChange = { viewModel.setDesktopTextSize(it.toInt()) },
                        valueRange = 12f..40f,
                        steps = 28
                    )
                }
            }

            // 桌面文字颜色
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            colorPickerTarget = "desktop_text"
                            showColorPickerDialog = true
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("桌面歌词文字颜色", modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(desktopTextColor))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "#${Integer.toHexString(desktopTextColor).uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 桌面背景颜色
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            colorPickerTarget = "desktop_bg"
                            showColorPickerDialog = true
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("桌面歌词背景颜色", modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(desktopBgColor))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "#${Integer.toHexString(desktopBgColor).uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("桌面歌词背景透明度: ${desktopBgAlpha}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = desktopBgAlpha.toFloat(),
                        onValueChange = { viewModel.setDesktopBgAlpha(it.toInt()) },
                        valueRange = 0f..255f,
                        steps = 255
                    )
                }
            }

            // ===== 预览区域 =====
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("预览", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

                        Text(
                            text = previewText,
                            color = Color(desktopTextColor),
                            fontSize = desktopTextSize.sp,
                            modifier = Modifier
                                .background(Color(desktopBgColor).copy(alpha = desktopBgAlpha / 255f))
                                .padding(8.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // ===== 颜色选择器 =====
    if (showColorPickerDialog) {
        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text("选择颜色") },
            text = {
                Column {
                    val presetColors = listOf(
                        0xFFFFFFFF.toInt(), 0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(),
                        0xFFFFFF00.toInt(), 0xFFFF00FF.toInt(), 0xFF00FFFF.toInt(), 0xFF888888.toInt(),
                        0xFFFF8800.toInt(), 0xFF8800FF.toInt(), 0xFF0088FF.toInt(), 0xFF88FF00.toInt()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presetColors.take(6).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(color))
                                        .clickable {
                                            when (colorPickerTarget) {
                                                "desktop_text" -> viewModel.setDesktopTextColor(color)
                                                "desktop_bg" -> viewModel.setDesktopBgColor(color)
                                            }
                                            showColorPickerDialog = false
                                        }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presetColors.drop(6).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(color))
                                        .clickable {
                                            when (colorPickerTarget) {
                                                "desktop_text" -> viewModel.setDesktopTextColor(color)
                                                "desktop_bg" -> viewModel.setDesktopBgColor(color)
                                            }
                                            showColorPickerDialog = false
                                        }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击颜色选择", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPickerDialog = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPickerDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // ===== 权限提示 =====
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要权限") },
            text = {
                Text("开启桌面歌词需要悬浮窗权限，请点击「去设置」并开启「允许显示在其他应用上层」。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        PermissionHelper.openOverlayPermissionSettings(context as Activity)
                    }
                ) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        viewModel.setDesktopLyricsEnabled(false)
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // ===== 清除缓存 =====
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("确认清除缓存") },
            text = { Text("此操作将删除所有缓存的歌曲文件（约 ${formatSize(cacheSize)}），但不会删除下载的歌曲。\n\n确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        viewModel.clearCache { /* 可选提示 */ }
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("取消") }
            }
        )
    }

    // ===== 清除数据 =====
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("确认清除数据") },
            text = {
                Text(
                    "此操作将删除所有本地数据，包括：\n" +
                            "• 收藏列表\n• 播放历史\n• 下载的歌曲\n• 应用设置\n• 歌曲缓存\n\n" +
                            "此操作不可恢复，确定继续吗？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.clearData { /* 可选提示 */ }
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingItem(title: String, subtitle: String, onClick: () -> Unit) {
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

private fun formatSize(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
    else -> "${size / (1024 * 1024 * 1024)} GB"
}