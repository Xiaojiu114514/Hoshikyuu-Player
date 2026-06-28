package com.hoshikyuu.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun rememberAlbumColorGradient(albumArtUrl: String?): Brush {
    return remember(albumArtUrl) {
        Brush.verticalGradient(
            colors = listOf(
                DefaultAlbumGradientStart,
                DefaultAlbumGradientEnd,
                Color(0xFF1A1C22)
            )
        )
    }
}
