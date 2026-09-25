package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.viewmodel.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = PureWhite,

    primaryContainer = NavigationSelected,
    onPrimaryContainer = TextPrimary,

    secondary = PurpleLight,
    onSecondary = PureWhite,

    secondaryContainer = AppSurfaceVariant,
    onSecondaryContainer = TextPrimary,

    tertiary = InfoBlue,
    onTertiary = PureWhite,

    background = AppBackground,
    onBackground = TextPrimary,

    surface = AppSurface,
    onSurface = TextPrimary,

    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    surfaceContainer = AppSurface,
    surfaceContainerHigh = AppSurfaceElevated,
    surfaceContainerHighest = AppSurfaceVariant,

    outline = AppBorder,
    outlineVariant = AppDivider,

    error = ErrorRed,
    onError = PureWhite,

    errorContainer = ErrorRed.copy(alpha = 0.18f),
    onErrorContainer = PureWhite
)

private val LightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = PurpleDark,
    onPrimary = PureWhite,

    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE9DDFF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF26005A),

    secondary = PurplePrimary,
    onSecondary = PureWhite,

    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE9E0F2),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF21182A),

    tertiary = androidx.compose.ui.graphics.Color(0xFF00639A),
    onTertiary = PureWhite,

    background = androidx.compose.ui.graphics.Color(0xFFF9F7FB),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A171D),

    surface = androidx.compose.ui.graphics.Color(0xFFF9F7FB),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A171D),

    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8E1EA),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF4A454D),

    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFF0EBF2),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFEAE4EC),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFFE4DEE6),

    outline = androidx.compose.ui.graphics.Color(0xFF7C757F),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFD0C8D1),

    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A),
    onError = PureWhite
)

@Composable
fun PrimeRepoTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.SYSTEM -> {
            if (androidx.compose.foundation.isSystemInDarkTheme()) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(20.dp)
        ),
        content = content
    )
}