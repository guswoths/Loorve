package com.loorve.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LoorveLightColorScheme = lightColorScheme(
    primary          = Primary,
    secondary        = Secondary,
    tertiary         = Tertiary,
    background       = Background,
    surface          = Surface,
    surfaceVariant   = SurfaceVariant,
    onBackground     = OnBackground,
    onSurface        = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    onPrimary        = Color.White,
    primaryContainer  = Color(0xFFDDF4F1),
    onPrimaryContainer = Color(0xFF0B3D39),
    outline          = Divider,
    error            = Error,
    onError          = Color.White
)

@Composable
fun LoorveTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor     = Background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = LoorveLightColorScheme,
        typography  = LoorveTypography,
        shapes      = LoorveShapes,
        content     = content
    )
}