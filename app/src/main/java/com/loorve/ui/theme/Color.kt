package com.loorve.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ============================================================
// Loorve Brand Colors — Deep Indigo Palette
// Material3 컬러 시스템 기반 (https://m3.material.io/styles/color)
// ============================================================

// -- Primary: Deep Indigo --
val md_theme_light_primary = Color(0xFF3949AB)          // 인디고 600
val md_theme_light_onPrimary = Color(0xFF1A1A1A)
val md_theme_light_primaryContainer = Color(0xFFDDE1FF)  // 인디고 50
val md_theme_light_onPrimaryContainer = Color(0xFF001082)

// -- Secondary: Soft Teal --
val md_theme_light_secondary = Color(0xFF00796B)         // 틸 700
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFB2DFDB)
val md_theme_light_onSecondaryContainer = Color(0xFF002019)

// -- Tertiary: Warm Amber (포인트/알림 강조) --
val md_theme_light_tertiary = Color(0xFFE65100)          // 딥 오렌지 900
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFDBCB)
val md_theme_light_onTertiaryContainer = Color(0xFF3A0900)

// -- Error --
val md_theme_light_error = Color(0xFFB3261E)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onErrorContainer = Color(0xFF410E0B)

// -- Background / Surface --
val md_theme_light_background = Color(0xFFFFFBFE)
val md_theme_light_onBackground = Color(0xFF1C1B1F)
val md_theme_light_surface = Color(0xFFFFFBFE)
val md_theme_light_onSurface = Color(0xFF1C1B1F)
val md_theme_light_surfaceVariant = Color(0xFFE7E0EC)
val md_theme_light_onSurfaceVariant = Color(0xFF49454F)
val md_theme_light_outline = Color(0xFF79747E)
val md_theme_light_outlineVariant = Color(0xFFCAC4D0)
val md_theme_light_scrim = Color(0xFF000000)
val md_theme_light_inverseSurface = Color(0xFF313033)
val md_theme_light_inverseOnSurface = Color(0xFFF4EFF4)
val md_theme_light_inversePrimary = Color(0xFFBEC2FF)
val md_theme_light_surfaceTint = Color(0xFF3949AB)

// -- Dark Theme --
val md_theme_dark_primary = Color(0xFFBEC2FF)            // 인디고 100
val md_theme_dark_onPrimary = Color(0xFF001A96)
val md_theme_dark_primaryContainer = Color(0xFF1A2F9E)
val md_theme_dark_onPrimaryContainer = Color(0xFFDDE1FF)

val md_theme_dark_secondary = Color(0xFF80CBC4)
val md_theme_dark_onSecondary = Color(0xFF003733)
val md_theme_dark_secondaryContainer = Color(0xFF00504A)
val md_theme_dark_onSecondaryContainer = Color(0xFFB2DFDB)

val md_theme_dark_tertiary = Color(0xFFFFB599)
val md_theme_dark_onTertiary = Color(0xFF5C1700)
val md_theme_dark_tertiaryContainer = Color(0xFF832500)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFDBCB)

val md_theme_dark_error = Color(0xFFF2B8B5)
val md_theme_dark_onError = Color(0xFF601410)
val md_theme_dark_errorContainer = Color(0xFF8C1D18)
val md_theme_dark_onErrorContainer = Color(0xFFF9DEDC)

val md_theme_dark_background = Color(0xFF1C1B1F)
val md_theme_dark_onBackground = Color(0xFFE6E1E5)
val md_theme_dark_surface = Color(0xFF1C1B1F)
val md_theme_dark_onSurface = Color(0xFFE6E1E5)
val md_theme_dark_surfaceVariant = Color(0xFF49454F)
val md_theme_dark_onSurfaceVariant = Color(0xFFCAC4D0)
val md_theme_dark_outline = Color(0xFF938F99)
val md_theme_dark_outlineVariant = Color(0xFF49454F)
val md_theme_dark_scrim = Color(0xFF000000)
val md_theme_dark_inverseSurface = Color(0xFFE6E1E5)
val md_theme_dark_inverseOnSurface = Color(0xFF313033)
val md_theme_dark_inversePrimary = Color(0xFF3949AB)
val md_theme_dark_surfaceTint = Color(0xFFBEC2FF)

// ============================================================
// 커스텀 확장 색상 (Custom Extension Colors)
// 필요 시 MaterialTheme Extension으로 노출 가능
// ============================================================

/** 복습 완료 상태 — 초록 계열 */
val ReviewCompleteColor = Color(0xFF388E3C)              // 그린 700
val ReviewCompleteContainerColor = Color(0xFFC8E6C9)    // 그린 100

/** 복습 예정 상태 — 앰버 계열 */
val ReviewPendingColor = Color(0xFFF57F17)               // 앰버 900
val ReviewPendingContainerColor = Color(0xFFFFF8E1)      // 앰버 50

/** 광고 배너 배경색 — 중립 그레이 (앱 컬러와 충돌 최소화) */
val AdBannerBackgroundColor = Color(0xFFF5F5F5)         // 그레이 100 (라이트)
val AdBannerBackgroundDarkColor = Color(0xFF2C2C2C)     // 다크 모드용

// ============================================================
// ColorScheme 정의
// ============================================================

val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
)

val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
)
