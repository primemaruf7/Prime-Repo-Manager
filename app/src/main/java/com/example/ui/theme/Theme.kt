package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val GitHubDarkColorScheme = darkColorScheme(
    primary = GitHubDarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1F385C),
    onPrimaryContainer = Color(0xFFC8E1FF),
    secondary = GitHubDarkGreen,
    onSecondary = Color.White,
    tertiary = GitHubDarkPurple,
    onTertiary = Color.White,
    background = GitHubDarkBg,
    onBackground = GitHubDarkText,
    surface = GitHubDarkSurface,
    onSurface = GitHubDarkText,
    surfaceVariant = GitHubDarkSurfaceVariant,
    onSurfaceVariant = GitHubDarkTextMuted,
    outline = GitHubDarkBorder,
    outlineVariant = Color(0xFF21262D),
    error = GitHubDarkRed,
    onError = Color.White
)

private val GitHubLightColorScheme = lightColorScheme(
    primary = GitHubLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF4FF),
    onPrimaryContainer = Color(0xFF0969DA),
    secondary = GitHubLightGreen,
    onSecondary = Color.White,
    tertiary = GitHubLightPurple,
    onTertiary = Color.White,
    background = GitHubLightBg,
    onBackground = GitHubLightText,
    surface = GitHubLightSurface,
    onSurface = GitHubLightText,
    surfaceVariant = GitHubLightSurfaceVariant,
    onSurfaceVariant = GitHubLightTextMuted,
    outline = GitHubLightBorder,
    outlineVariant = Color(0xFFEAEFF5),
    error = GitHubLightRed,
    onError = Color.White
)

@Composable
fun PrimeRepoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> GitHubDarkColorScheme
        else -> GitHubLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
