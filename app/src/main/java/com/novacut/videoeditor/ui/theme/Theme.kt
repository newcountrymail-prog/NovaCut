package com.novacut.videoeditor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    secondary = PinkAccent,
    tertiary = BeatYellow,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceCard,
    onPrimary = BgDark,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextSecondary
)

@Composable
fun NovaCutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
