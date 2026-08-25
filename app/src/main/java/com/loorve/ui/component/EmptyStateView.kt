package com.loorve.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyStateView(
    message: String,
    subMessage: String = "",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,        // ✅ onActionClick → onAction 으로 통일
    onActionClick: (() -> Unit)? = null,   // ✅ 하위 호환을 위해 onActionClick도 추가 (내부에서 onAction으로 위임)
    modifier: Modifier = Modifier
) {
    // onActionClick이 전달된 경우 onAction으로 위임
    val resolvedAction = onAction ?: onActionClick

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (subMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && resolvedAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = resolvedAction) {
                Text(text = actionLabel)
            }
        }
    }
}