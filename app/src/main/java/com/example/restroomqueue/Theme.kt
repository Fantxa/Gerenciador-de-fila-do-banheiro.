package com.example.restroomqueue

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0C2840)
val Blue = Color(0xFF1A90D9)
val Slate = Color(0xFF2E3940)
val Cyan = Color(0xFF32BAD9)
val Mint = Color(0xFF6AD9C4)

@Composable fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Blue, secondary = Cyan, tertiary = Mint, background = Navy, surface = Slate,
        onPrimary = Color.White, onSecondary = Navy, onBackground = Color.White, onSurface = Color.White,
        surfaceContainerLowest = Navy, surfaceContainerLow = Slate, surfaceContainer = Slate,
        surfaceContainerHigh = Slate, surfaceContainerHighest = Slate, onSurfaceVariant = Color(0xFFB8D4E3),
        error = Color(0xFFFF6B6B)
    ),
    content = content
)
