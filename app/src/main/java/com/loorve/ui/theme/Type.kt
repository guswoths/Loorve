package com.loorve.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.loorve.R

val PretendardFamily = FontFamily(
    Font(R.font.pretendard_regular,   FontWeight.Normal),
    Font(R.font.pretendard_medium,    FontWeight.Medium),
    Font(R.font.pretendard_semibold,  FontWeight.SemiBold),
    Font(R.font.pretendard_bold,      FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold)
)

val LoorveTypography = Typography(
    displayLarge = TextStyle(
        fontFamily   = PretendardFamily,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 32.sp,
        lineHeight   = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily   = PretendardFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 22.sp,
        lineHeight   = 30.sp,       // 추가: 목업 헤더 줄간격 반영
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily   = PretendardFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 18.sp,
        lineHeight   = 26.sp,
        letterSpacing = (-0.5).sp
    ),
    bodyLarge = TextStyle(
        fontFamily   = PretendardFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = (-0.5).sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = PretendardFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = (-0.5).sp
    ),
    bodySmall = TextStyle(              // 신규 추가: 날짜·설명 보조 텍스트
        fontFamily   = PretendardFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 18.sp,
        letterSpacing = (-0.3).sp
    ),
    labelMedium = TextStyle(
        fontFamily   = PretendardFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = (-0.5).sp
    ),
    labelSmall = TextStyle(             // 신규 추가: 뱃지·칩·광고 라벨
        fontFamily   = PretendardFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 10.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.sp
    )
)