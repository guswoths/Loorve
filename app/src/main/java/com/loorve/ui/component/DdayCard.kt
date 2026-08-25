package com.loorve.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loorve.ui.theme.*

@Composable
fun DdayCard(
    subjectName: String,
    daysLeft: Int,
    modifier: Modifier = Modifier
) {
    // 음수(시험 지남)이면 렌더링 안 함
    if (daysLeft < 0) return

    val gradient = Brush.linearGradient(listOf(GradientStart, GradientEnd))
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp)
            .clip(shape)
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = subjectName,
                    style = LoorveTypography.titleMedium,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "시험까지",
                    style = LoorveTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (daysLeft == 0) "D-Day 🎯" else "D-${daysLeft}",
                    fontFamily = PretendardFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = Color.White
                )
                if (daysLeft > 0) {
                    Text(
                        text = "일 남음",
                        style = LoorveTypography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}