package com.loorve.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loorve.ui.theme.*

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "buttonScale"
    )
    val gradient = Brush.linearGradient(
        colors = listOf(GradientStart, GradientMiddle, GradientEnd)
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(CircleShape)
            .scale(scale)
            .background(
                if (enabled) gradient else Brush.linearGradient(
                    listOf(TertiaryText.copy(alpha = 0.55f), TertiaryText.copy(alpha = 0.55f))
                )
            )
            .clickable(enabled = enabled && !isLoading) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = OnGradient,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = OnGradient,
                style = LoorveTypography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PretendardFamily
            )
        }
    }
}