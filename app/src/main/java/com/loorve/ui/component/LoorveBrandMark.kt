package com.loorve.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.loorve.ui.theme.PretendardFamily

@Composable
fun LoorveBrandMark(
    modifier: Modifier = Modifier,
    size: Dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFFF8FAFF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loorve",
            style = TextStyle(
                fontFamily = PretendardFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 116f / 512f).sp,
                letterSpacing = (-0.035f * (size.value * 116f / 512f)).sp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF2563EB),
                        Color(0xFF4F46E5),
                        Color(0xFF7C3AED),
                        Color(0xFFC026D3)
                    )
                )
            ),
            textAlign = TextAlign.Center
        )
    }
}
