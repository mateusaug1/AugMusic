package com.augustos.augmusic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val AugInk = Color(0xFF10120F)
internal val AugPanel = Color(0xFF1A1E19)
internal val AugLime = Color(0xFFB7F36B)
internal val AugMuted = Color(0xFF9BA397)

@Composable
internal fun WMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AugLime,
            onPrimary = AugInk,
            background = AugInk,
            surface = AugPanel,
            onSurface = Color.White,
            onBackground = Color.White,
            surfaceContainer = AugPanel,
            surfaceContainerHigh = AugPanel
        ),
        content = content
    )
}
