package com.loorve.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val LoorveShapes = Shapes(
    small      = RoundedCornerShape(8.dp),    // 버튼, 입력 필드
    medium     = RoundedCornerShape(16.dp),   // 카드
    large      = RoundedCornerShape(24.dp),   // 바텀 시트, 모달
    extraLarge = RoundedCornerShape(32.dp)    // 풀스크린 카드
)

// 칩·뱃지 전용 pill 모양 (Shape 시스템 외부 상수로 정의)
val ChipShape = RoundedCornerShape(50.dp)
val BadgeShape = RoundedCornerShape(6.dp)