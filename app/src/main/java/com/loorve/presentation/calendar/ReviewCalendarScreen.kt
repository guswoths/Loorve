package com.loorve.presentation.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.loorve.domain.review.DailyReviewCompletionStat
import com.loorve.domain.review.splitValidReviewCompletionStatSegments
import com.loorve.presentation.reviewblock.ReviewRecordMiniCard
import com.loorve.ui.component.BannerAdView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
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
                            text = "최근 7일 복습 완료율",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    item {
                        ReviewCompletionChart(
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
private fun ReviewCompletionChart(
    stats: List<DailyReviewCompletionStat>,
    selectedStat: DailyReviewCompletionStat?,
    isLoading: Boolean,
    onStatSelected: (DailyReviewCompletionStat) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
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
    val summary = stats.joinToString(separator = ". ") { stat ->
        val label = stat.date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))
        if (stat.completionRatePercent == null) {
            "$label: 복습 일정 없음"
        } else {
            "$label: ${stat.completedCount}/${stat.dueCount}, ${stat.completionRatePercent}%"
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = summary }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(stats) {
                        detectTapGestures { tap ->
                            val left = 44f
                            val right = size.width - 12f
                            val spacing = (right - left) / (stats.size - 1).coerceAtLeast(1)
                            val index = ((tap.x - left) / spacing).toInt()
                                .coerceIn(0, stats.lastIndex)
                            if (abs(tap.x - (left + spacing * index)) <= spacing / 2) {
                                onStatSelected(stats[index])
                            }
                        }
                    }
            ) {
                val top = 16f
                val bottom = size.height - 34f
                val left = 44f
                val right = size.width - 12f
                val plotHeight = (bottom - top).coerceAtLeast(1f)
                val spacing = (right - left) / (stats.size - 1).coerceAtLeast(1)
                val yForRate = { rate: Int ->
                    top + (100f - rate.coerceIn(0, 100)) / 100f * plotHeight
                }

                val labelPaint = android.graphics.Paint().apply {
                    color = primary.toArgb()
                    textSize = 11.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                listOf(0, 25, 50, 75, 100).forEach { rate ->
                    val y = yForRate(rate)
                    drawLine(
                        color = primary.copy(alpha = 0.16f),
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = 1f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "$rate%",
                        left - 8f,
                        y + 4f,
                        labelPaint
                    )
                }

                splitValidReviewCompletionStatSegments(stats)
                    .forEach { segment ->
                        val points = segment.map { (index, stat) ->
                            index to Offset(
                                left + spacing * index,
                                yForRate(stat.completionRatePercent ?: 0)
                            )
                        }
                        val linePath = Path().apply {
                            var previous = points.first().second
                            moveTo(previous.x, previous.y)
                            points.drop(1).forEach { (_, point) ->
                                val midpoint = (previous.x + point.x) / 2f
                                quadraticTo(midpoint, previous.y, midpoint, (previous.y + point.y) / 2f)
                                quadraticTo(point.x, point.y, point.x, point.y)
                                previous = point
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(points.last().second.x, bottom)
                            lineTo(points.first().second.x, bottom)
                            close()
                        }
                        drawPath(fillPath, primary.copy(alpha = 0.1f))
                        drawPath(linePath, primary, style = Stroke(width = 3.dp.toPx()))
                        points.forEach { (index, point) ->
                            val isToday = index == stats.lastIndex
                            if (isToday) {
                                drawCircle(
                                    color = androidx.compose.ui.graphics.Color.White,
                                    radius = 8.dp.toPx(),
                                    center = point
                                )
                            }
                            drawCircle(
                                color = primary,
                                radius = if (isToday) 6.dp.toPx() else 5.dp.toPx(),
                                center = point
                            )
                        }
                    }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stats.forEach { stat ->
                Text(
                    text = stat.date.format(DateTimeFormatter.ofPattern("M/d", Locale.KOREAN)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        selectedStat?.let { stat ->
            Text(
                text = if (stat.completionRatePercent == null) {
                    "${stat.date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))}: No review scheduled"
                } else {
                    "${stat.date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))}: " +
                        "${stat.completedCount} / ${stat.dueCount} · ${stat.completionRatePercent}%"
                },
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
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