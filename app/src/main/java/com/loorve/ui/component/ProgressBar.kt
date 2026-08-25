package com.loorve.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.loorve.ui.theme.*

@Composable
fun LoorveProgressBar(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (total > 0) completed.toFloat() / total.toFloat() else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progressAnim"
    )
    val percentage = (animatedFraction * 100).toInt()
    val fillGradient = Brush.linearGradient(listOf(Tertiary, Primary))
    val trackShape = RoundedCornerShape(4.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(trackShape)
                .background(SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(trackShape)
                    .background(fillGradient)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$percentage%",
            style = LoorveTypography.labelMedium,
            color = OnSurface
        )
    }
}