package com.example.myscreenshot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppTeal,
    onPrimary = AppDeepTeal,
    primaryContainer = Color(0xFF064B54),
    onPrimaryContainer = AppMint,
    secondary = AppCobalt,
    onSecondary = AppMint,
    secondaryContainer = Color(0xFF163F8F),
    onSecondaryContainer = Color(0xFFE8F0FF),
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
    primary = AppDeepTeal,
    onPrimary = AppMint,
    primaryContainer = AppMint,
    onPrimaryContainer = AppDeepTeal,
    secondary = AppCobalt,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8FF),
    onSecondaryContainer = Color(0xFF0D2D6B),
    tertiary = AppCoral,
    tertiaryContainer = Color(0xFFFFE6E1),
    onTertiaryContainer = Color(0xFF651F14),
    background = AppLightBackground,
    onBackground = AppDarkText,
    surface = AppCard,
    onSurface = AppDarkText,
    surfaceVariant = Color(0xFFDDECE8),
    onSurfaceVariant = Color(0xFF43636B),
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
    val DarkBackground = androidx.compose.ui.graphics.Color(0xFF061B22)
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF0D2A33)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF123844)
    val DarkText = androidx.compose.ui.graphics.Color(0xFFE7FFFB)
    val DarkMutedText = androidx.compose.ui.graphics.Color(0xFFA8C9C4)
    val DarkBorder = androidx.compose.ui.graphics.Color(0xFF245460)
}
