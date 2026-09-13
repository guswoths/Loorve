package com.loorve.presentation.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.usecase.ReviewCompletionOutcome
import com.loorve.ui.theme.Background
import com.loorve.ui.theme.Warning
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayReviewsScreen(
    onOpenCalendar: () -> Unit,
    viewModel: TodayReviewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.errorMessage) {
        (state.message ?: state.errorMessage)?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("오늘의 복습", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("우선순위가 높은 일정부터 표시합니다.", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = onOpenCalendar) { Text("전체 캘린더") }
                }
            }
            if (state.overloadedCount > 0) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Warning, contentDescription = "일일 과부하 경고", tint = Warning)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "일일 과부하 확인 필요: ${state.overloadedCount}개 일정은 이동 가능한 날짜가 부족합니다.",
                                color = Warning
                            )
                        }
                    }
                }
            }
            if (state.isLoading) {
                item { CircularProgressIndicator(Modifier.padding(24.dp)) }
            } else if (state.items.isEmpty()) {
                item { Text("오늘 예정된 복습이 없습니다.", style = MaterialTheme.typography.bodyLarge) }
            } else {
                items(state.items, key = { it.item.id }) { review ->
                    TodayReviewCard(
                        review = review,
                        completing = state.completingId == review.item.id,
                        onComplete = { outcome -> viewModel.complete(review.item.id, outcome) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayReviewCard(
    review: TodayReviewItemUi,
    completing: Boolean,
    onComplete: (ReviewCompletionOutcome) -> Unit
) {
    var showOutcomes by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().semantics {
        contentDescription = "오늘의 복습: ${review.item.title}"
    }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = review.subjectName.ifBlank { "복습 일정" },
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = review.item.title.ifBlank { "학습기록" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(review.item.recommendedMethod.ifBlank { "문제풀이·퀴즈·빈 종이 회상" })
            review.daysUntilExam?.let { Text("시험까지 ${it}일") }
            Text("상태: ${review.item.planStatus.name}")
            if (showOutcomes) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        ReviewCompletionOutcome.EASY to "쉬움",
                        ReviewCompletionOutcome.SUCCESS to "성공",
                        ReviewCompletionOutcome.HARD to "어려움",
                        ReviewCompletionOutcome.FAILED to "실패"
                    ).forEach { (outcome, label) ->
                        FilterChip(
                            selected = false,
                            onClick = { showOutcomes = false; onComplete(outcome) },
                            enabled = !completing,
                            label = { Text(label) }
                        )
                    }
                }
            } else {
                Button(onClick = { showOutcomes = true }, enabled = !completing) {
                    Text(if (completing) "저장 중..." else "복습 결과 입력")
                }
            }
        }
    }
}
