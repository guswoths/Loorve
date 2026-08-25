package com.loorve.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.loorve.ui.theme.Primary
import com.loorve.ui.theme.Surface as SurfaceColor
import com.loorve.ui.theme.SurfaceVariant

@Composable
fun LoorveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .border(1.dp, SurfaceVariant, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = SurfaceColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = shape
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            content()
        }
    }
}