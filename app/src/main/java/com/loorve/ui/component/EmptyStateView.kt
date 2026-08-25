package com.loorve.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loorve.ui.theme.*

@Composable
fun EmptyStateView(
    message: String,
    subMessage: String = "",
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = OnSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = LoorveTypography.bodyLarge,
            color = OnSurface,
            textAlign = TextAlign.Center
        )
        if (subMessage.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subMessage,
                style = LoorveTypography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onActionClick != null) {
            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = actionLabel,
                onClick = onActionClick,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}