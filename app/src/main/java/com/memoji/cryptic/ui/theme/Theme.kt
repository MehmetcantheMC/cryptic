package com.memoji.cryptic.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF86EFAC),
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF005226),
    onPrimaryContainer = Color(0xFFA3F4BE),
    secondary = Color(0xFF7DD5EA),
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF004E5A),
    onSecondaryContainer = Color(0xFFA7EEFF),
    background = Color(0xFF001F25),
    surface = Color(0xFF001F25),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006D36),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA3F4BE),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF006876),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFA7EEFF),
    onSecondaryContainer = Color(0xFF001F25),
    background = Color(0xFFFBFCFD),
    surface = Color(0xFFFBFCFD),
)

@Composable
fun CrypticoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}