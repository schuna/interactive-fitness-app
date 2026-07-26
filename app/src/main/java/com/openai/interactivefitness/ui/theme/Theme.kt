package com.openai.interactivefitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2D6A4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F3DC),
    secondary = Color(0xFF3A5A40),
    background = Color(0xFFF7F9F7),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF95D5B2),
    secondary = Color(0xFFB7E4C7),
)

@Composable
fun FitnessTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
