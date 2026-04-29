package com.DeBiaseRamiro.gymera.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary        = PurplePrimary,
    secondary      = CyanAccent,
    background     = BackgroundDark,
    surface        = SurfaceDark,
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = OnBackground,
    onSurface      = OnSurface,
)

@Composable
fun GymeraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}