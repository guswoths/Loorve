package com.loorve.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    onPrimary        = OnGradient,
    primaryContainer  = NoticeContainer,
    onPrimaryContainer = PrimaryVariant,
    secondaryContainer = ActiveContainer,
    onSecondaryContainer = PrimaryVariant,
    tertiaryContainer = SuccessContainer,
    onTertiaryContainer = Success,
    outline          = Divider,
    error            = Error,
    onError          = OnGradient,
    errorContainer   = WarningContainer,
    onErrorContainer = Error
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
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Background, CanvasWarm)
                        )
                    )
            ) {
                content()
            }
        }
    )
}