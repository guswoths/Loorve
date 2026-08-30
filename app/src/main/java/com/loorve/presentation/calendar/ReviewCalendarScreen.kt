package com.loorve.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.model.ReviewSchedule
import com.loorve.ui.component.BannerAdView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddReviewBlock: () -> Unit,
    reviewCalendarViewModel: ReviewCalendarViewModel = hiltViewModel()
) {
    val uiState by reviewCalendarViewModel.uiState.collectAsState()

    // refreshUid()가 suspend fun이므로 순차 실행 보장
    LaunchedEffect(Unit) {
        reviewCalendarViewModel.refreshUid()          // suspend — 완료될 때까지 대기
        reviewCalendarViewModel.loadCurrentMonth()    // uid 세팅 후 안전하게 호출
        reviewCalendarViewModel.onDateSelected(LocalDate.now())
        // loadReviewBlocks는 refreshUid() 내부에서 자동 호출됨
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = reviewCalendarViewModel::onDismissError,
            title = { Text("오류") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = reviewCalendarViewModel::onDismissError) {
                    Text("확인")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("복습 캘린더") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddReviewBlock) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "복습 블록 생성"
                )
            }
        },
        bottomBar = {
            BannerAdView(modifier = Modifier.fillMaxWidth())
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 기존 날짜별 복습 일정 + 복습 블록 목록을 하나의 LazyColumn으로 통합
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    )
                ) {
                    // ── 섹션 1: 날짜별 복습 일정 ─────────────────────────
                    item {
                        Text(
                            text = uiState.selectedDate?.format(
                                DateTimeFormatter.ofPattern("M월 d일 복습", Locale.KOREAN)
                            ) ?: "날짜 정보 없음",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    if (uiState.selectedDateSchedules.isEmpty()) {
                        item {
                            EmptyScheduleMessage("선택한 날짜에는 예정된 복습이 없습니다.")
                        }
                    } else {
                        items(
                            items = uiState.selectedDateSchedules,
                            key = { it.scheduleId }
                        ) { schedule ->
                            ReviewScheduleItem(
                                schedule = schedule,
                                onToggleCompleted = {
                                    reviewCalendarViewModel.toggleReviewCompletion(
                                        scheduleId = schedule.scheduleId,
                                        currentState = schedule.isCompleted
                                    )
                                }
                            )
                        }
                    }

                    // ── 섹션 2: 복습 블록 목록 ───────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "복습 블록 목록",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    }

                    if (uiState.isBlocksLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    } else if (uiState.reviewBlocks.isEmpty()) {
                        item {
                            EmptyScheduleMessage("아직 생성된 복습 블록이 없습니다.")
                        }
                    } else {
                        items(
                            items = uiState.reviewBlocks,
                            key = { it.blockId }
                        ) { block ->
                            ReviewBlockCard(block = block)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Private Composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReviewScheduleItem(
    schedule: ReviewSchedule,
    onToggleCompleted: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleCompleted),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (schedule.isCompleted) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = if (schedule.isCompleted) "복습 완료" else "복습 미완료",
                tint = if (schedule.isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.title.ifBlank { "복습 일정" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${schedule.reviewOrder}회차 · ${schedule.scheduleType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReviewBlockCard(block: ReviewBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (block.date.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "시험 종료일: ${block.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (block.description.isNotBlank()) {
                    Text(
                        text = block.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (block.isCompleted) {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(
                        text = "완료",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = "진행중",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScheduleMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}