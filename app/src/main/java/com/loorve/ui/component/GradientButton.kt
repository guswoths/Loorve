package com.loorve.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val shape = RoundedCornerShape(26.dp)
    val gradient = Brush.linearGradient(listOf(GradientStart, GradientEnd))

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .scale(scale)
            .background(if (enabled) gradient else Brush.linearGradient(
                listOf(OnSurfaceVariant, OnSurfaceVariant)
            ))
            .clickable(enabled = enabled && !isLoading) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = OnBackground,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = OnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = PretendardFamily
            )
        }
    }
}