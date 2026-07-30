package com.linguacam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), // Indigo 100
    onPrimaryContainer = Color(0xFF312E81), // Indigo 900
    
    secondary = Color(0xFF8B5CF6), // Violet 500
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE), // Violet 100
    onSecondaryContainer = Color(0xFF4C1D95), // Violet 900
    
    tertiary = Color(0xFFEC4899), // Pink 500
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3), // Pink 100
    onTertiaryContainer = Color(0xFF831843), // Pink 900
    
    background = Color(0xFFF8FAFC), // Slate 50
    onBackground = Color(0xFF0F172A), // Slate 900
    surface = Color.White,
    onSurface = Color(0xFF1E293B), // Slate 800
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = Color(0xFF64748B), // Slate 500
    
    error = Color(0xFFEF4444), // Red 500
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2), // Red 100
    onErrorContainer = Color(0xFF7F1D1D), // Red 900
    
    outline = Color(0xFFCBD5E1), // Slate 300
    outlineVariant = Color(0xFFE2E8F0), // Slate 200
    scrim = Color(0x80000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8), // Indigo 400
    onPrimary = Color(0xFF312E81), // Indigo 900
    primaryContainer = Color(0xFF4338CA), // Indigo 700
    onPrimaryContainer = Color(0xFFE0E7FF), // Indigo 100
    
    secondary = Color(0xFFA78BFA), // Violet 400
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF6D28D9), // Violet 700
    onSecondaryContainer = Color(0xFFEDE9FE), // Violet 100
    
    tertiary = Color(0xFFF472B6), // Pink 400
    onTertiary = Color(0xFF831843), // Pink 900
    tertiaryContainer = Color(0xFFBE185D), // Pink 700
    onTertiaryContainer = Color(0xFFFCE7F3), // Pink 100
    
    background = Color(0xFF0F172A), // Slate 900
    onBackground = Color(0xFFF8FAFC), // Slate 50
    surface = Color(0xFF1E293B), // Slate 800
    onSurface = Color(0xFFF1F5F9), // Slate 100
    surfaceVariant = Color(0xFF334155), // Slate 700
    onSurfaceVariant = Color(0xFF94A3B8), // Slate 400
    
    error = Color(0xFFF87171), // Red 400
    onError = Color(0xFF7F1D1D), // Red 900
    errorContainer = Color(0xFF991B1B), // Red 800
    onErrorContainer = Color(0xFFFEE2E2), // Red 100
    
    outline = Color(0xFF475569), // Slate 600
    outlineVariant = Color(0xFF334155), // Slate 700
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
