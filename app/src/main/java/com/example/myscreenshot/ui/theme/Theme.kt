package com.example.myscreenshot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppOrange,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4A2100),
    onPrimaryContainer = Color(0xFFFFE1BF),
    secondary = Color(0xFFFFB347),
    onSecondary = AppInk,
    secondaryContainer = Color(0xFF3A2A16),
    onSecondaryContainer = Color(0xFFFFE1BF),
    tertiary = Color(0xFFE38A70),
    tertiaryContainer = Color(0xFF4D2119),
    onTertiaryContainer = Color(0xFFFFD8CC),
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
    primary = AppOrange,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = AppPeach,
    onPrimaryContainer = Color(0xFF3C1C00),
    secondary = AppScan,
    onSecondary = AppInk,
    secondaryContainer = AppPeach,
    onSecondaryContainer = Color(0xFF3E2A04),
    tertiary = AppCoral,
    tertiaryContainer = Color(0xFFFFE1D8),
    onTertiaryContainer = Color(0xFF5B1C12),
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
    val DarkBackground = androidx.compose.ui.graphics.Color(0xFF101113)
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF191B1F)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF24272D)
    val DarkText = androidx.compose.ui.graphics.Color(0xFFFFF8F0)
    val DarkMutedText = androidx.compose.ui.graphics.Color(0xFFC9C1B8)
    val DarkBorder = androidx.compose.ui.graphics.Color(0xFF3A342E)
}
