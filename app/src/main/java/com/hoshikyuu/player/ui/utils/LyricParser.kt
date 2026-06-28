package com.hoshikyuu.player.ui.utils

data class LyricLine(val timestampMs: Long, val text: String)

/**
 * 解析 LRC 格式歌詞字串為可滾動的時間軸列表。
 * 支援 [mm:ss.xx] 與 [mm:ss] 兩種時間戳格式。
 */
fun parseLyrics(lrc: String): List<LyricLine> {
    if (lrc.isBlank()) return emptyList()

    val lines = lrc.lines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@mapNotNull null

        // 匹配 [mm:ss.xx] 或 [mm:ss] 支援多時間戳如 [00:01.00][01:15.00]...
        val regex = Regex("""\[(\d{1,3}):(\d{2})(?:\.(\d{2,3}))?\]""")
        val matches = regex.findAll(trimmed).toList()
        if (matches.isEmpty()) return@mapNotNull null

        // 取第一個時間戳
        val m = matches.first()
        val minutes = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
        val seconds = m.groupValues[2].toLongOrNull() ?: return@mapNotNull null
        val millis = m.groupValues[3].let {
            when (it.length) {
                3 -> it.toLongOrNull() ?: 0L
                2 -> (it.toLongOrNull() ?: 0L) * 10
                else -> 0L
            }
        }
        val timestampMs = minutes * 60_000 + seconds * 1000 + millis

        // 歌詞文字 = 移除所有時間戳後的剩餘部分
        val text = trimmed.replace(regex, "").trim()
        if (text.isEmpty()) return@mapNotNull null

        LyricLine(timestampMs, text)
    }

    // 按時間排序
    return lines.sortedBy { it.timestampMs }
}
