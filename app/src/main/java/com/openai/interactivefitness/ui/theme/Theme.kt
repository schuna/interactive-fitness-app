package com.openai.interactivefitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColors = lightColorScheme(
    primary = Color(0xFF7346E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0EAFE),
    onPrimaryContainer = Color(0xFF25114F),
    secondary = Color(0xFF6750A4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7F8),
    onSecondaryContainer = Color(0xFF231A36),
    background = Color(0xFFFBFAFD),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFF3F0F7),
    onSurfaceVariant = Color(0xFF676370),
    outline = Color(0xFFD9D5DF),
    outlineVariant = Color(0xFFE9E5ED),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA985FF),
    onPrimary = Color(0xFF25114F),
    primaryContainer = Color(0xFF4F2A9D),
    onPrimaryContainer = Color(0xFFEBDCFF),
    secondary = Color(0xFFCDBDFF),
    onSecondary = Color(0xFF34275D),
    secondaryContainer = Color(0xFF493D73),
    onSecondaryContainer = Color(0xFFE9DDFF),
    background = Color(0xFF090B12),
    onBackground = Color(0xFFE7E3EC),
    surface = Color(0xFF10131C),
    onSurface = Color(0xFFE7E3EC),
    surfaceVariant = Color(0xFF191C26),
    onSurfaceVariant = Color(0xFFAAA5B2),
    outline = Color(0xFF3B3E49),
    outlineVariant = Color(0xFF292C36),
    error = Color(0xFFFFB4AB),
)

private val FitnessTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

private val FitnessShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun FitnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FitnessTypography,
        shapes = FitnessShapes,
        content = content,
    )
}
