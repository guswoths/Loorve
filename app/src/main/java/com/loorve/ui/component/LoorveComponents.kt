// app/src/main/java/com/loorve/ui/component/LoorveComponents.kt
package com.loorve.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================
// LoorveCard — surface layering 기반 카드 컨테이너
// ============================================================
@Composable
fun LoorveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium // 18dp
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .then(
                if (onClick != null)
                    Modifier.clickable(role = Role.Button) { onClick() }
                else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(0.5.dp, borderColor),
        content = content
    )
}

// ============================================================
// LorvePrimaryButton — Solid 버튼 (올리브 배경)
// ============================================================
@Composable
fun LorvePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(50.dp), // pill
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color       = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text       = text,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ============================================================
// LoorveOutlineButton — Outline 버튼 (ghost 스타일)
// ============================================================
@Composable
fun LoorveOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) MaterialTheme.colorScheme.outline
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color       = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text  = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ============================================================
// DdayCard — 홈 상단 시험 D-Day 요약 카드
// ============================================================
@Composable
fun DdayCard(
    subjectName: String,
    daysLeft: Long,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val softAccent  = MaterialTheme.colorScheme.primaryContainer

    LoorveCard(
        modifier  = modifier.fillMaxWidth(),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(softAccent)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text  = subjectName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = "시험까지",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text  = "D-${daysLeft}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = accentColor
                )
            }
        }
    }
}

// ============================================================
// SectionLabel — 섹션 제목
// ============================================================
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color    = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

// ============================================================
// EmptyStateView — 빈 상태 공통 컴포넌트
// ============================================================
@Composable
fun EmptyStateView(
    message: String,
    subMessage: String = "",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = subMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            LoorveOutlineButton(
                text     = actionLabel,
                onClick  = onAction,
                modifier = Modifier.widthIn(min = 160.dp)
            )
        }
    }
}