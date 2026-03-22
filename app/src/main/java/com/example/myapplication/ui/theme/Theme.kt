package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Add these if they are missing!
val DarkOnSurface = Color(0xFFE0E0E0)
val LightBackground = Color(0xFFF8FAF9)
val LightSurface = Color(0xFFFFFFFF)


// 1. DARK MODE COLORS (Professional Charcoal)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF52B788),            // Your Mint Green
    onPrimary = Color.White,                // Forces button text to stay White (Not Blue!)

    background = Color(0xFF121212),         // Deep Black background
    onBackground = Color.White,

    surface = DarkGreySurface,              // Replaces Blue cards with Dark Grey
    onSurface = Color.White,

    surfaceVariant = DarkGreyContainer,     // This is the "Light Grey" box color you liked
    onSurfaceVariant = Color(0xFFBDBDBD)    // Subtle grey for icons/descriptions
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B4332),
    background = LightBackground, // Fixes error #2
    surface = LightSurface,       // Fixes error #3
    onPrimary = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Ensure your Type.kt is clean
        content = content
    )
}