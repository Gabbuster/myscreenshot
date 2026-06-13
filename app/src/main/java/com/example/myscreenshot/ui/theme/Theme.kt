package com.example.myscreenshot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF7A4C),
    onPrimary = AppInk,
    primaryContainer = Color(0xFF3B1820),
    onPrimaryContainer = Color(0xFFFFD7C7),
    secondary = Color(0xFFFFB15E),
    onSecondary = AppInk,
    secondaryContainer = Color(0xFF4A2A0C),
    onSecondaryContainer = Color(0xFFFFDDB7),
    tertiary = AppCoral,
    tertiaryContainer = Color(0xFF5A211A),
    onTertiaryContainer = Color(0xFFFFE6E1),
    background = ColorTokens.DarkBackground,
    onBackground = ColorTokens.DarkText,
    surface = ColorTokens.DarkSurface,
    onSurface = ColorTokens.DarkText,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.DarkMutedText,
    outline = ColorTokens.DarkBorder,
    error = Color(0xFFFF8A7A),
    errorContainer = Color(0xFF5F1F1A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColorScheme = lightColorScheme(
    primary = AppInk,
    onPrimary = Color.White,
    primaryContainer = AppPeach,
    onPrimaryContainer = AppInk,
    secondary = AppOrange,
    onSecondary = AppInk,
    secondaryContainer = Color(0xFFFFE7C7),
    onSecondaryContainer = Color(0xFF35200A),
    tertiary = AppCoral,
    tertiaryContainer = Color(0xFFFFE4E8),
    onTertiaryContainer = Color(0xFF5B111B),
    background = AppLightBackground,
    onBackground = AppDarkText,
    surface = AppCard,
    onSurface = AppDarkText,
    surfaceVariant = AppSurface,
    onSurfaceVariant = AppMuted,
    outline = AppBorder,
    error = AppError,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF7A1A12),
)

@Composable
fun MyScreenshotTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        Surface(
            color = colorScheme.background,
            contentColor = colorScheme.onBackground,
            content = content,
        )
    }
}

private object ColorTokens {
    val DarkBackground = androidx.compose.ui.graphics.Color(0xFF070B12)
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF111925)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF1B2430)
    val DarkText = androidx.compose.ui.graphics.Color(0xFFF4F2EF)
    val DarkMutedText = androidx.compose.ui.graphics.Color(0xFFADB3BA)
    val DarkBorder = androidx.compose.ui.graphics.Color(0xFF303947)
}
