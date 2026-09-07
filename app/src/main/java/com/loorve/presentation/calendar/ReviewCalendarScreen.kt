package com.loorve.presentation.calendar

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.presentation.reviewblock.ReviewRecordMiniCard
import com.loorve.presentation.home.HomeViewModel
import com.loorve.ui.component.BannerAdView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material3.Card

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddReviewBlock: () -> Unit,
    onNavigateToReviewBlockDetail: (blockId: String) -> Unit = {},  // ✅ 신규 파라미터
    reviewCalendarViewModel: ReviewCalendarViewModel = hiltViewModel()
) {
    val uiState by reviewCalendarViewModel.uiState.collectAsState()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val selectedDateSchedules = homeUiState.reviewSchedules
        .filter { it.reviewDate == uiState.selectedDate }
        .map { schedule ->
            ReviewSchedule(
                scheduleId = schedule.scheduleId,
                originProgressId = schedule.originProgressId,
                blockId = schedule.examId,
                title = schedule.content,
                reviewDate = schedule.reviewDate
                    .atStartOfDay(java.time.ZoneId.of("Asia/Seoul"))
                    .toInstant()
                    .toEpochMilli(),
                reviewOrder = schedule.reviewOrder,
                isCompleted = schedule.isCompleted
            )
        }

    LaunchedEffect(Unit) {
        reviewCalendarViewModel.refreshUid()
        reviewCalendarViewModel.onDateSelected(LocalDate.now())
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
                    // ── 섹션 1: 날짜별 복습 일정
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

                    if (selectedDateSchedules.isEmpty()) {
                        item {
                            EmptyScheduleMessage("금일 예정된 복습은 없습니다")
                        }
                    } else {
                        items(
                            items = selectedDateSchedules,
                            key = {
                                it.scheduleId.ifBlank {
                                    "${it.reviewDate}_${it.blockId}_${it.reviewOrder}"
                                }
                            }
                        ) { schedule ->
                            ReviewScheduleItem(
                                schedule = schedule,
                                onToggleCompleted = {
                                    reviewCalendarViewModel.toggleReviewCompletion(
                                        scheduleId = schedule.scheduleId.ifBlank {
                                            "${schedule.reviewDate}_${schedule.blockId}_${schedule.reviewOrder}"
                                        },
                                        currentState = schedule.isCompleted
                                    )
                                }
                            )
                        }
                    }

                    // ── 섹션 2: 복습 블록 목록
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
                            ReviewBlockCard(
                                block = block,
                                // ✅ 핵심 수정: 네비게이션으로 변경
                                onClick = { onNavigateToReviewBlockDetail(block.blockId) }
                            )
                            val blockSchedules = uiState.selectedDateSchedules.filter {
                                it.blockId == block.blockId
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                blockSchedules.forEach { schedule ->
                                    var notificationTime by remember(schedule.scheduleId) {
                                        mutableStateOf("")
                                    }
                                    ReviewRecordMiniCard(
                                        item = schedule.toReviewScheduleItem(),
                                        savedTime = notificationTime,
                                        onTimeSave = { notificationTime = it },
                                        onCheckedChange = {
                                            reviewCalendarViewModel.toggleReviewCompletion(
                                                scheduleId = schedule.scheduleId.ifBlank {
                                                    "${schedule.reviewDate}_${schedule.blockId}_${schedule.reviewOrder}"
                                                },
                                                currentState = schedule.isCompleted
                                            )
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
    // ✅ ReviewBlockDetailBottomSheet 제거 — ReviewBlockDetailScreen으로 대체
}

// ── Private Composables ────────────────────────────────────────────────────────

private fun ReviewSchedule.toReviewScheduleItem(): ReviewScheduleItem =
    ReviewScheduleItem(
        id = scheduleId,
        blockId = blockId,
        uid = userId,
        title = title,
        reviewDate = reviewDate,
        reviewOrder = reviewOrder,
        status = if (isCompleted) ReviewStatus.COMPLETED else ReviewStatus.PENDING,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

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
private fun ReviewBlockCard(
    block: ReviewBlock,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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