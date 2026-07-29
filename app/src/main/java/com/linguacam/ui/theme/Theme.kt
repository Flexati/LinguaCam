package com.linguacam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4DB8AA),
    onPrimaryContainer = Color(0xFF00695C),
    
    secondary = Color(0xFFFF6F00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFB74D),
    onSecondaryContainer = Color(0xFFE65100),
    
    tertiary = Color(0xFF7B1FA2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBA68C8),
    onTertiaryContainer = Color(0xFF4A148C),
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color.White,
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF757575),
    
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFEF5350),
    onErrorContainer = Color(0xFFB71C1C),
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0),
    scrim = Color(0x80000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DB8AA),
    onPrimary = Color(0xFF00695C),
    primaryContainer = Color(0xFF00897B),
    onPrimaryContainer = Color(0xFFB2E8E0),
    
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFFE65100),
    secondaryContainer = Color(0xFFFF6F00),
    onSecondaryContainer = Color(0xFFFFDCC4),
    
    tertiary = Color(0xFFBA68C8),
    onTertiary = Color(0xFF4A148C),
    tertiaryContainer = Color(0xFF7B1FA2),
    onTertiaryContainer = Color(0xFFE1BEE7),
    
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    
    error = Color(0xFFEF5350),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFCDD2),
    
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF424242),
    scrim = Color(0x80000000)
)

@Composable
fun LinguaCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
