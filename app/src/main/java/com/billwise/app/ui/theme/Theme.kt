package com.billwise.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandIndigo80,
    onPrimary = BrandSlateDark,
    secondary = BrandEmerald80,
    onSecondary = BrandSlateDark,
    error = BrandCoral80,
    background = BrandSlateDark,
    surface = BrandSlateCard,
    onBackground = BrandSlateLight,
    onSurface = BrandSlateLight
)

private val LightColorScheme = lightColorScheme(
    primary = BrandIndigo40,
    onPrimary = BrandSlateLight,
    secondary = BrandEmerald40,
    onSecondary = BrandSlateLight,
    error = BrandCoral40,
    background = BrandSlateLight,
    surface = BrandSlateLight,
    onBackground = BrandSlateDark,
    onSurface = BrandSlateDark
)

@Composable
fun BillWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

