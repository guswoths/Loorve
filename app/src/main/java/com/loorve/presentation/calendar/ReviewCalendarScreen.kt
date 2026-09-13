package com.loorve.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.review.DailyReviewCompletionStat
import com.loorve.presentation.reviewblock.ReviewRecordMiniCard
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
                            text = "최근 7일 복습 현황",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    item {
                        ReviewWorkloadBarChart(
                            stats = uiState.completionStats,
                            selectedStat = uiState.selectedCompletionStat,
                            isLoading = uiState.isCompletionStatsLoading,
                            onStatSelected = reviewCalendarViewModel::onCompletionStatSelected
                        )
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
                                        onTimeSave = { hour, minute ->
                                            notificationTime = "%02d:%02d".format(hour, minute)
                                        },
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

@Composable
private fun ReviewWorkloadBarChart(
    stats: List<DailyReviewCompletionStat>,
    selectedStat: DailyReviewCompletionStat?,
    isLoading: Boolean,
    onStatSelected: (DailyReviewCompletionStat) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    if (stats.isEmpty() || stats.none { it.dueCount > 0 }) {
        EmptyScheduleMessage("최근 7일 동안 복습 일정이 없습니다.")
        return
    }

    val primary = MaterialTheme.colorScheme.primary
    val totalDue = stats.sumOf { it.dueCount.coerceAtLeast(0) }
    val totalCompleted = stats.sumOf { it.completedCount.coerceIn(0, it.dueCount) }
    val totalRemaining = (totalDue - totalCompleted).coerceAtLeast(0)
    val latestDate = stats.lastOrNull()?.date
    val summary = buildString {
        append("최근 7일 복습 현황. 예정 ${totalDue}개, 완료 ${totalCompleted}개, 미완료 ${totalRemaining}개.")
        stats.forEach { stat ->
            append(" ")
            append(stat.date.chartDateLabel(latestDate))
            if (stat.dueCount <= 0) {
                append(" 복습 일정 없음.")
            } else {
                val completed = stat.completedCount.coerceIn(0, stat.dueCount)
                append(" 전체 ${stat.dueCount}개 중 완료 ${completed}개, 미완료 ${stat.dueCount - completed}개.")
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = summary }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            stats.forEach { stat ->
                ReviewWorkloadBar(
                    stat = stat,
                    latestDate = latestDate,
                    maxDueCount = stats.maxOfOrNull { it.dueCount.coerceAtLeast(0) }
                        ?.coerceAtLeast(1) ?: 1,
                    selected = selectedStat?.date == stat.date,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatSelected(stat) }
                )
            }
        }
        ChartLegend()
        selectedStat?.let { stat ->
            Text(
                text = if (stat.dueCount <= 0) {
                    "${stat.date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} · 복습 일정 없음"
                } else {
                    val completed = stat.completedCount.coerceIn(0, stat.dueCount)
                    "${stat.date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} · " +
                        "완료 ${completed}개 / 전체 ${stat.dueCount}개 · " +
                        "미완료 ${stat.dueCount - completed}개 · 완료율 ${stat.completionRatePercent ?: 0}%"
                },
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "최근 7일 · 예정 ${totalDue}개 · 완료 ${totalCompleted}개 · 미완료 ${totalRemaining}개",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReviewWorkloadBar(
    stat: DailyReviewCompletionStat,
    latestDate: LocalDate?,
    maxDueCount: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val completedCount = stat.completedCount.coerceIn(0, stat.dueCount)
    val remainingCount = (stat.dueCount - completedCount).coerceAtLeast(0)
    val totalHeight = 126.dp
    val totalRatio = stat.dueCount.toFloat() / maxDueCount.toFloat()
    val totalBarHeight = totalHeight * totalRatio
    val completedFractionOfBar = if (stat.dueCount > 0) {
        completedCount.toFloat() / stat.dueCount.toFloat()
    } else {
        0f
    }
    val completedBarHeight = totalBarHeight * completedFractionOfBar
    val remainingBarHeight = (totalBarHeight - completedBarHeight).coerceAtLeast(0.dp)
    val isToday = stat.date == latestDate
    val label = stat.date.chartDateLabel(latestDate)
    val description = if (stat.dueCount <= 0) {
        "$label, 복습 일정 없음"
    } else {
        "$label, 예정 ${stat.dueCount}개, 완료 ${completedCount}개, 미완료 ${remainingCount}개"
    }
    val outline = when {
        isToday -> primary
        selected -> primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val outlineWidth = if (isToday || selected) 2.dp else 1.dp

    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 34.dp)
            .clip(MaterialTheme.shapes.small)
            .border(outlineWidth, outline, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 6.dp)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (stat.dueCount <= 0) {
            Box(
                modifier = Modifier
                    .height(126.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier.height(126.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (remainingCount > 0) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(remainingBarHeight)
                            .background(primary.copy(alpha = 0.22f))
                    )
                }
                if (completedCount > 0) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(completedBarHeight)
                            .background(primary)
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ChartLegend() {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        LegendItem(color = primary, label = "완료")
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(color = primary.copy(alpha = 0.22f), label = "미완료")
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, MaterialTheme.shapes.extraSmall)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun LocalDate.chartDateLabel(latestDate: LocalDate?): String =
    if (this == latestDate) "오늘" else format(DateTimeFormatter.ofPattern("M/d", Locale.KOREAN))

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