package com.loorve.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loorve.ui.theme.PretendardFamily

@Composable
fun LoorveWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 120.sp,
    textColor: List<Color> = listOf(
        Color(0xFF2563EB),
        Color(0xFF4F46E5),
        Color(0xFF7C3AED),
        Color(0xFFC026D3)
    )
) {
    Text(
        text = "Loorve",
        modifier = modifier,
        style = TextStyle(
            fontFamily = PretendardFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            lineHeight = (fontSize.value * 0.9f).sp,
            letterSpacing = (-0.12f * fontSize.value).sp,
            brush = Brush.horizontalGradient(colors = textColor)
        ),
        textAlign = TextAlign.Center
    )
}

@Composable
fun LoorveBrandMark(
    modifier: Modifier = Modifier,
    size: Dp
) {
    LoorveWordmark(
        modifier = modifier,
        fontSize = (size.value * 116f / 512f).sp
    )
}
