package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

    errorContainer = Color(0xFF3B1E22),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = PurpleDark,
    onPrimary = PureWhite,

    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF26005A),

    secondary = PurplePrimary,
    onSecondary = PureWhite,

    secondaryContainer = Color(0xFFE9E0F2),
    onSecondaryContainer = Color(0xFF21182A),

    tertiary = Color(0xFF00639A),
    onTertiary = PureWhite,

    background = Color(0xFFF9F7FB),
    onBackground = Color(0xFF1A171D),

    surface = Color(0xFFF9F7FB),
    onSurface = Color(0xFF1A171D),

    surfaceVariant = Color(0xFFE8E1EA),
    onSurfaceVariant = Color(0xFF4A454D),

    surfaceContainer = Color(0xFFF0EBF2),
    surfaceContainerHigh = Color(0xFFEAE4EC),
    surfaceContainerHighest = Color(0xFFE4DEE6),

    outline = Color(0xFF7C757F),
    outlineVariant = Color(0xFFD0C8D1),

    error = Color(0xFFBA1A1A),
    onError = PureWhite,

    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun PrimeRepoTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
            } else {
                dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
            }
        }

        darkTheme -> DarkColorScheme

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ),
        content = content
    )
}