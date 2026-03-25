package io.j3mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = J3Primary,
    secondary = J3Secondary,
    error = J3Error,
    background = J3Dark,
    surface = J3DarkSurface,
    surfaceVariant = J3DarkCard,
    onPrimary = J3Dark,
    onSecondary = J3Dark,
    onBackground = J3OnDark,
    onSurface = J3OnDark,
    onSurfaceVariant = J3OnDarkSecondary,
    outline = J3Border,
)

private val LightColorScheme = lightColorScheme(
    primary = J3Primary,
    secondary = J3Secondary,
    error = J3Error,
    background = J3Light,
    surface = J3LightSurface,
    surfaceVariant = J3LightCard,
    onPrimary = J3Light,
    onSecondary = J3Light,
    onBackground = J3OnLight,
    onSurface = J3OnLight,
    onSurfaceVariant = J3OnLightSecondary,
    outline = J3LightBorder,
)

@Composable
fun J3MobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = J3Typography,
        content = content,
    )
}
