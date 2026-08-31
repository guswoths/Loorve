package com.loorve.presentation.reviewblock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loorve.domain.model.CompletionResult
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.model.StudyRecord
import java.text.SimpleDateFormat
import java.util.*

// ── 1회독 권장일 카드 ─────────────────────────────────────────
@Composable
fun RecommendedCompletionCard(
    recommendedDateMillis: Long,
    deadlineBufferDays: Long,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA) }
    val dateText = remember(recommendedDateMillis) {
        sdf.format(Date(recommendedDateMillis))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription =
                    "시험 ${deadlineBufferDays}일 전까지 1회독 완료를 권장합니다. 권장 완료일: $dateText"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📚 1회독 완료 권장일",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "시험 ${deadlineBufferDays}일 전까지 1회독 완료를 권장합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "권장 완료일: $dateText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ── 진도 입력 섹션 ─────────────────────────────────────────────
@Composable
fun StudyProgressInputSection(
    onSave: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "오늘 학습 진도",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "오늘 학습한 진도를 입력하세요" },
            placeholder = { Text("예: 1장~3장, 수학 미분 단원") },
            label = { Text("학습 내용") },
            maxLines = 4,
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    onSave(inputText.trim())
                    inputText = ""
                }
            },
            modifier = Modifier
                .align(Alignment.End)
                .semantics { contentDescription = "학습 진도 저장 버튼" },
            enabled = inputText.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("저장하고 복습 일정 생성")
            }
        }
    }
}

// ── 복습 일정 리스트 ──────────────────────────────────────────
@Composable
fun ReviewScheduleList(
    items: List<ReviewScheduleItem>,
    overdueItems: List<ReviewScheduleItem>,
    reviewOverloadWarning: Boolean,
    onComplete: (ReviewScheduleItem, CompletionResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("MM/dd (E)", Locale.KOREA) }

    LazyColumn(modifier = modifier) {
        // 과부하 경고
        if (reviewOverloadWarning) {
            item {
                OverloadWarningBanner()
            }
        }

        // OVERDUE 항목 먼저
        if (overdueItems.isNotEmpty()) {
            item {
                Text(
                    text = "⚠️ 누락된 복습 (오래된 순)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(overdueItems, key = { it.id }) { item ->
                ReviewScheduleItemCard(
                    item = item,
                    dateText = sdf.format(Date(item.reviewDate)),
                    onComplete = onComplete
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        }

        // 일반 예정 일정
        val pendingItems = items.filter {
            it.status == ReviewStatus.PENDING || it.status == ReviewStatus.FINAL_URGENT_REVIEW
        }
        items(pendingItems, key = { it.id }) { item ->
            ReviewScheduleItemCard(
                item = item,
                dateText = sdf.format(Date(item.reviewDate)),
                onComplete = onComplete
            )
        }
    }
}

@Composable
fun ReviewScheduleItemCard(
    item: ReviewScheduleItem,
    dateText: String,
    onComplete: (ReviewScheduleItem, CompletionResult) -> Unit
) {
    val (bgColor, statusLabel, statusDesc) = when (item.status) {
        ReviewStatus.OVERDUE -> Triple(
            Color(0xFFFFF3E0),
            "• 누락 ${item.overdueDays}일 경과",
            "누락된 복습 항목"
        )
        ReviewStatus.FINAL_URGENT_REVIEW -> Triple(
            Color(0xFFFFEBEE),
            "🔴 긴급 복습",
            "시험 임박 긴급 복습 항목"
        )
        ReviewStatus.COMPLETED -> Triple(
            Color(0xFFF1F8E9),
            "✅ 완료",
            "완료된 복습 항목"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surface,
            "",
            "예정된 복습 항목"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { contentDescription = "$statusDesc: ${item.title}, 날짜: $dateText" },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dateText, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (statusLabel.isNotBlank()) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.status == ReviewStatus.OVERDUE ||
                            item.status == ReviewStatus.FINAL_URGENT_REVIEW)
                            MaterialTheme.colorScheme.error
                        else Color.Unspecified
                    )
                }
            }

            if (item.status == ReviewStatus.OVERDUE ||
                item.status == ReviewStatus.FINAL_URGENT_REVIEW
            ) {
                Column {
                    TextButton(
                        onClick = { onComplete(item, CompletionResult.REMEMBERED) },
                        modifier = Modifier.semantics {
                            contentDescription = "기억함 버튼 - ${item.title}"
                        }
                    ) { Text("기억함", color = Color(0xFF2E7D32)) }
                    TextButton(
                        onClick = { onComplete(item, CompletionResult.FORGOT) },
                        modifier = Modifier.semantics {
                            contentDescription = "잊어버림 버튼 - ${item.title}"
                        }
                    ) { Text("잊어버림", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun OverloadWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEBEE))
            .padding(12.dp)
            .semantics { contentDescription = "경고: 시험 전 일정이 초과되었습니다." },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "시험 전 남은 기간에 모든 누락 복습을 배치하기 어렵습니다. 중요도 순으로 우선 복습하세요.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFC62828)
        )
    }
}