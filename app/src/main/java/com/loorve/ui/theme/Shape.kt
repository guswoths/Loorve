package com.loorve.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val LoorveShapes = Shapes(
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(24.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val InputShape = RoundedCornerShape(14.dp)
val NestedShape = RoundedCornerShape(16.dp)
val ContainerShape = RoundedCornerShape(24.dp)
val OuterShellShape = RoundedCornerShape(32.dp)

// 칩·뱃지·버튼 전용 pill 모양
val ChipShape = RoundedCornerShape(50.dp)
val BadgeShape = RoundedCornerShape(50.dp)