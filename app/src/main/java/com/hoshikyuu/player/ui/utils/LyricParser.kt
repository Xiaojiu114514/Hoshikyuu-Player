package com.hoshikyuu.player.ui.utils

data class LyricLine(val timestampMs: Long, val text: String)

fun parseLyrics(lrc: String): List<LyricLine> {
    if (lrc.isBlank()) return emptyList()
    val lines = lrc.lines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        val regex = Regex("""\[(\d{1,3}):(\d{2})(?:\.(\d{2,3}))?\]""")
        val matches = regex.findAll(trimmed).toList()
        if (matches.isEmpty()) return@mapNotNull null
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
        val text = trimmed.replace(regex, "").trim()
        if (text.isEmpty()) return@mapNotNull null
        LyricLine(timestampMs, text)
    }
    return lines.sortedBy { it.timestampMs }
}