package com.loorve.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================
// LoorveTheme — Loorve 앱 메인 Compose 테마
// ============================================================
// 의존성: Color.kt (LightColorScheme, DarkColorScheme)
//         Type.kt  (LoorveTypography)
// ============================================================

/**
 * Loorve 앱 전역 Compose 테마.
 *
 * @param darkTheme       다크 모드 여부 (기본값: 시스템 설정 따름)
 * @param dynamicColor    Android 12+ Dynamic Color 활성화 여부
 *                        (기본값: SDK >= S 인 경우 true)
 * @param content         테마가 적용될 컴포저블 콘텐츠 슬롯
 */
@Composable
fun LoorveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        // Android 12+ Dynamic Color: 사용자 월페이퍼 기반 컬러 추출
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Loorve 브랜드 컬러 스킴 적용
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 상태바 색상을 컬러 스킴에 맞게 동기화
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LoorveTypography,
        content = content,
    )
}
