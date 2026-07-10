package com.partitionsoft.stacksy.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorWhite = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = Tangerine,
    onPrimary = Cream,
    secondary = Berry,
    tertiary = Mint,
    background = Cream,
    onBackground = Ink,
    surface = ColorWhite,
    onSurface = Ink,
    surfaceVariant = TangerineLight.copy(alpha = 0.35f),
)

private val DarkColors = darkColorScheme(
    primary = TangerineLight,
    onPrimary = Night,
    secondary = Lemon,
    tertiary = Mint,
    background = Night,
    onBackground = Cream,
    surface = NightSurface,
    onSurface = Cream,
)

@Composable
fun StacksyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = StacksyTypography,
        content = content,
    )
}
