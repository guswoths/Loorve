package com.loorve.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LorveDarkColorScheme = darkColorScheme(
    primary          = Primary,
    secondary        = Secondary,
    tertiary         = Tertiary,
    background       = Background,
    surface          = Surface,
    surfaceVariant   = SurfaceVariant,
    onBackground     = OnBackground,
    onSurface        = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error            = Error
)

@Composable
fun LoorveTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor     = Background.toArgb()  // 0xFF0F0F14
            @Suppress("DEPRECATION")
            window.navigationBarColor = Background.toArgb()  // 하단 내비 바도 동일 배경
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = LorveDarkColorScheme,
        typography  = LoorveTypography,
        shapes      = LoorveShapes,
        content     = content
    )
}