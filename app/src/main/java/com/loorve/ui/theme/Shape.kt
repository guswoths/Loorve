// app/src/main/java/com/loorve/ui/theme/Shape.kt
package com.loorve.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================================
// Loorve Shape Tokens — Quiet Premium radius 계층
// ============================================================
// extraSmall : 소형 요소 (chip, badge) — 12dp
// small      : 입력 필드, 소형 카드    — 14dp
// medium     : 일반 카드               — 18dp
// large      : 대형 카드               — 22dp
// extraLarge : 대형 컨테이너, 바텀시트 — 28dp
// ============================================================

val LoorveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small      = RoundedCornerShape(14.dp),
    medium     = RoundedCornerShape(18.dp),
    large      = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)