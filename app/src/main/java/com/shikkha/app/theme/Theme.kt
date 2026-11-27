package com.shikkha.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryTeal,
    secondaryContainer = SecondaryContainerLight,
    surface = SurfaceLight,
    background = BackgroundLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00306B),
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryContainerDark,
    surface = SurfaceDark,
    background = BackgroundDark,
)

@Composable
fun ShikkhaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
