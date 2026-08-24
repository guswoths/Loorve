// app/src/main/java/com/loorve/ui/theme/Color.kt
package com.loorve.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ============================================================
// Loorve Brand Colors — Quiet Premium Palette (모바일 3안)
// ============================================================

// -- 전체 배경 --
val QP_Background       = Color(0xFFF3F0EA)   // #F3F0EA 웜 베이지
val QP_Surface          = Color(0xFFFCFBF8)   // #FCFBF8 메인 surface
val QP_SurfaceVariant   = Color(0xFFF6F3EE)   // #F6F3EE 보조 surface
val QP_CardSurface      = Color(0xFFFFFFFF)   // #FFFFFF 카드 white

// -- 텍스트 --
val QP_OnBackground     = Color(0xFF201C17)   // #201C17 primary text
val QP_OnSurface        = Color(0xFF201C17)
val QP_OnSurfaceVariant = Color(0xFF736C63)   // #736C63 secondary text
val QP_TextFaint        = Color(0xFFA69F96)   // #A69F96 faint text

// -- 포인트 (올리브) --
val QP_Primary          = Color(0xFF4F5A3F)   // #4F5A3F 딥 올리브
val QP_OnPrimary        = Color(0xFFFFFFFF)
val QP_PrimaryContainer = Color(0x1A4F5A3F)   // soft accent ~10% 올리브
val QP_OnPrimaryContainer = Color(0xFF1E2718)

// -- Secondary (뉴트럴 지원색) --
val QP_Secondary        = Color(0xFF736C63)
val QP_OnSecondary      = Color(0xFFFFFFFF)
val QP_SecondaryContainer = Color(0xFFF6F3EE)
val QP_OnSecondaryContainer = Color(0xFF201C17)

// -- Tertiary (복습 완료 강조 — 절제된 초록) --
val QP_Tertiary         = Color(0xFF4F5A3F)   // 동일 올리브 계열 유지
val QP_OnTertiary       = Color(0xFFFFFFFF)
val QP_TertiaryContainer = Color(0xFFDAE0CF)
val QP_OnTertiaryContainer = Color(0xFF1E2718)

// -- Error --
val QP_Error            = Color(0xFFB3261E)
val QP_OnError          = Color(0xFFFFFFFF)
val QP_ErrorContainer   = Color(0xFFF9DEDC)
val QP_OnErrorContainer = Color(0xFF410E0B)

// -- 보더 --
val QP_Outline          = Color(0x33201C17)   // ~20% #201C17
val QP_OutlineVariant   = Color(0x14241F18)   // rgba(36,31,24,0.08)

// -- Inverse --
val QP_InverseSurface   = Color(0xFF2E2B27)
val QP_InverseOnSurface = Color(0xFFF6F3EE)
val QP_InversePrimary   = Color(0xFFB5C2A3)

// -- 복습 상태 색 --
val ReviewCompleteColor         = Color(0xFF4F5A3F)   // 올리브 계열로 통일
val ReviewCompleteContainerColor = Color(0xFFDAE0CF)
val ReviewPendingColor          = Color(0xFF8A6E45)   // 웜 브라운 (앰버 대신)
val ReviewPendingContainerColor  = Color(0xFFF5EBD9)

// -- 광고 배너 배경 --
val AdBannerBackgroundColor     = Color(0xFFF3F0EA)   // 앱 배경과 동일하게 자연스럽게
val AdBannerBackgroundDarkColor = Color(0xFF2C2A27)

// ============================================================
// ColorScheme
// ============================================================

val LightColorScheme = lightColorScheme(
    primary             = QP_Primary,
    onPrimary           = QP_OnPrimary,
    primaryContainer    = QP_PrimaryContainer,
    onPrimaryContainer  = QP_OnPrimaryContainer,
    secondary           = QP_Secondary,
    onSecondary         = QP_OnSecondary,
    secondaryContainer  = QP_SecondaryContainer,
    onSecondaryContainer = QP_OnSecondaryContainer,
    tertiary            = QP_Tertiary,
    onTertiary          = QP_OnTertiary,
    tertiaryContainer   = QP_TertiaryContainer,
    onTertiaryContainer = QP_OnTertiaryContainer,
    error               = QP_Error,
    onError             = QP_OnError,
    errorContainer      = QP_ErrorContainer,
    onErrorContainer    = QP_OnErrorContainer,
    background          = QP_Background,
    onBackground        = QP_OnBackground,
    surface             = QP_Surface,
    onSurface           = QP_OnSurface,
    surfaceVariant      = QP_SurfaceVariant,
    onSurfaceVariant    = QP_OnSurfaceVariant,
    outline             = QP_Outline,
    outlineVariant      = QP_OutlineVariant,
    scrim               = Color(0xFF000000),
    inverseSurface      = QP_InverseSurface,
    inverseOnSurface    = QP_InverseOnSurface,
    inversePrimary      = QP_InversePrimary,
    surfaceTint         = QP_Primary,
)

// 다크모드 — 구조상 확장 가능하게 정의 (현재 요구사항 외 준비용)
val DarkColorScheme = darkColorScheme(
    primary             = Color(0xFFB5C2A3),
    onPrimary           = Color(0xFF1E2718),
    primaryContainer    = Color(0xFF374430),
    onPrimaryContainer  = Color(0xFFDAE0CF),
    secondary           = Color(0xFFBFB8AE),
    onSecondary         = Color(0xFF2E2B27),
    secondaryContainer  = Color(0xFF44403C),
    onSecondaryContainer = Color(0xFFF6F3EE),
    tertiary            = Color(0xFFB5C2A3),
    onTertiary          = Color(0xFF1E2718),
    tertiaryContainer   = Color(0xFF374430),
    onTertiaryContainer = Color(0xFFDAE0CF),
    error               = Color(0xFFF2B8B5),
    onError             = Color(0xFF601410),
    errorContainer      = Color(0xFF8C1D18),
    onErrorContainer    = Color(0xFFF9DEDC),
    background          = Color(0xFF171512),
    onBackground        = Color(0xFFEDE8E2),
    surface             = Color(0xFF1C1A17),
    onSurface           = Color(0xFFEDE8E2),
    surfaceVariant      = Color(0xFF3A3731),
    onSurfaceVariant    = Color(0xFFBFB8AE),
    outline             = Color(0xFF8A847C),
    outlineVariant      = Color(0xFF3A3731),
    scrim               = Color(0xFF000000),
    inverseSurface      = Color(0xFFEDE8E2),
    inverseOnSurface    = Color(0xFF2E2B27),
    inversePrimary      = Color(0xFF4F5A3F),
    surfaceTint         = Color(0xFFB5C2A3),
)