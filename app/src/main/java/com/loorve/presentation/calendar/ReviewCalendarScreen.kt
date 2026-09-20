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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.loorve.presentation.subscription.ProPaywallDialog
import com.loorve.presentation.subscription.SubscriptionViewModel
import com.loorve.domain.subscription.ReviewBlockAccessPolicy
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.ui.component.BannerAdView
import com.loorve.ui.theme.LoorveTypography
import com.loorve.ui.theme.Active
import com.loorve.ui.theme.ActiveContainer
import com.loorve.ui.theme.Background
import com.loorve.ui.theme.CanvasWarm
import com.loorve.ui.theme.Divider
import com.loorve.ui.theme.GradientEnd
import com.loorve.ui.theme.GradientMiddle
import com.loorve.ui.theme.GradientStart
import com.loorve.ui.theme.Notice
import com.loorve.ui.theme.NoticeContainer
import com.loorve.ui.theme.OnBackground
import com.loorve.ui.theme.OnSurfaceVariant
import com.loorve.ui.theme.Primary
import com.loorve.ui.theme.Surface
import com.loorve.ui.theme.SurfaceSolid
import com.loorve.ui.theme.Success
import com.loorve.ui.theme.SuccessContainer
import com.loorve.ui.theme.TertiaryText
import com.loorve.ui.theme.Warning
import com.loorve.ui.theme.WarningContainer
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
    reviewCalendarViewModel: ReviewCalendarViewModel = hiltViewModel(),
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by reviewCalendarViewModel.uiState.collectAsState()
    var showProDialog by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(Background, CanvasWarm))
            )
    ) {
        ReviewDashboardAura()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "GEMINI REVIEW HUB",
                                style = LoorveTypography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "복습",
                                style = LoorveTypography.titleLarge,
                                color = OnBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GradientStart, GradientMiddle, Active)
                            )
                        ),
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 12.dp
                ) {
                    androidx.compose.material3.FloatingActionButton(
                        onClick = {
                            if (ReviewBlockAccessPolicy.canCreate(
                                    uiState.reviewBlocks,
                                    uiState.subscriptionEntitlement
                                )
                            ) {
                                onNavigateToAddReviewBlock()
                            } else {
                                showProDialog = true
                            }
                        },
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "복습 블록 생성"
                        )
                    }
                }
            },
            bottomBar = if (uiState.subscriptionEntitlement is SubscriptionEntitlement.Pro) {
                {}
            } else {
                {
                    BannerAdView(modifier = Modifier.fillMaxWidth())
                }
            },
            containerColor = Color.Transparent
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
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                    ) {
                    // ── 섹션 1: 날짜별 복습 일정
                    item {
                        Text(
                            text = "이번 주 복습 페이스",
                            modifier = Modifier.padding(
                                start = 4.dp,
                                top = 4.dp,
                                bottom = 2.dp
                            ),
                            style = LoorveTypography.headlineSmall,
                            color = OnBackground,
                            fontWeight = FontWeight.Bold
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
                            modifier = Modifier.padding(
                                start = 4.dp,
                                top = 8.dp,
                                bottom = 4.dp
                            ),
                            style = LoorveTypography.headlineSmall,
                            color = OnBackground,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(
                            color = Divider,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
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
                                locked = block.blockId in uiState.lockedBlockIds,
                                onClick = {
                                    if (block.blockId in uiState.lockedBlockIds) {
                                        showProDialog = true
                                    } else {
                                        onNavigateToReviewBlockDetail(block.blockId)
                                    }
                                }
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

                            if (showProDialog) {
                                ProPaywallDialog(
                                    viewModel = subscriptionViewModel,
                                    onDismiss = { showProDialog = false }
                                )
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
    }
    // ✅ ReviewBlockDetailBottomSheet 제거 — ReviewBlockDetailScreen으로 대체
}

@Composable
private fun ReviewDashboardAura() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 220.dp, top = 12.dp)
            .size(180.dp)
            .blur(72.dp)
            .background(
                color = com.loorve.ui.theme.SkyTint.copy(alpha = 0.52f),
                shape = CircleShape
            )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 220.dp, top = 360.dp)
            .size(220.dp)
            .blur(80.dp)
            .background(
                color = com.loorve.ui.theme.LavenderTint.copy(alpha = 0.48f),
                shape = CircleShape
            )
    )
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

    val totalDue = stats.sumOf { it.dueCount.coerceAtLeast(0) }
    val totalCompleted = stats.sumOf { it.completedCount.coerceIn(0, it.dueCount) }
    val totalRemaining = (totalDue - totalCompleted).coerceAtLeast(0)
    val completionRate = if (totalDue > 0) {
        (totalCompleted.toFloat() / totalDue.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = summary },
        shape = RoundedCornerShape(24.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Black.copy(alpha = 0.06f)
        ),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "주간 복습 완료율",
                        style = LoorveTypography.labelMedium,
                        color = OnSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${(completionRate * 100).toInt()}%",
                        style = LoorveTypography.displayMedium,
                        color = OnBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (completionRate >= 0.8f) {
                        SuccessContainer
                    } else {
                        NoticeContainer
                    }
                ) {
                    Text(
                        text = if (completionRate >= 0.8f) "최적 페이스" else "목표 진행 중",
                        style = LoorveTypography.labelSmall,
                        color = if (completionRate >= 0.8f) Success else Notice,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            Text(
                text = "최근 7일 · 전체 ${totalDue}개 중 ${totalCompleted}개 완료",
                style = LoorveTypography.bodySmall,
                color = OnSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEach { stat ->
                    ReviewWorkloadBar(
                        stat = stat,
                        latestDate = latestDate,
                        selected = selectedStat?.date == stat.date,
                        modifier = Modifier.weight(1f),
                        onClick = { onStatSelected(stat) }
                    )
                }
            }
            ChartLegend()
            selectedStat?.let { stat ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = NoticeContainer
                ) {
                    Text(
                        text = if (stat.dueCount <= 0) {
                            "${stat.date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} · 복습 일정 없음"
                        } else {
                            val completed = stat.completedCount.coerceIn(0, stat.dueCount)
                            "${stat.date.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))} · " +
                                "전체 ${stat.dueCount}개 중 ${completed}개 완료"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = LoorveTypography.bodySmall,
                        color = OnBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = "미완료 ${totalRemaining}개",
                style = LoorveTypography.bodySmall,
                color = OnSurfaceVariant
            )
            }
    }
}

@Composable
private fun ReviewWorkloadBar(
    stat: DailyReviewCompletionStat,
    latestDate: LocalDate?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val completedCount = stat.completedCount.coerceIn(0, stat.dueCount)
    val remainingCount = (stat.dueCount - completedCount).coerceAtLeast(0)
    val totalHeight = 126.dp
    val completionRate = if (stat.dueCount > 0) {
        completedCount.toFloat() / stat.dueCount.toFloat()
    } else {
        0f
    }
    val barHeight = totalHeight * completionRate
    val isToday = stat.date == latestDate
    val label = stat.date.chartDateLabel(latestDate)
    val description = if (stat.dueCount <= 0) {
        "$label, 복습 일정 없음"
    } else {
        "$label, 예정 ${stat.dueCount}개, 완료 ${completedCount}개, 미완료 ${remainingCount}개"
    }
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 34.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) NoticeContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 8.dp)
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
                if (completionRate > 0f) {
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(barHeight)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Notice, Active)
                                )
                            )
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isToday || selected) OnBackground else OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ChartLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        LegendItem(color = Active, label = "완료율")
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
                    .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
                text = label,
                style = LoorveTypography.labelSmall,
                color = OnSurfaceVariant
        )
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
    locked: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Black.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.title,
                        style = LoorveTypography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    if (block.date.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "시험 종료일: ${block.date}",
                            style = LoorveTypography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                val badgeColor = when {
                    locked -> WarningContainer
                    block.isCompleted -> SuccessContainer
                    else -> NoticeContainer
                }
                val badgeTextColor = when {
                    locked -> Warning
                    block.isCompleted -> Success
                    else -> Notice
                }
                Surface(
                    shape = CircleShape,
                    color = badgeColor
                ) {
                    Text(
                        text = when {
                            locked -> "높은 우선순위"
                            block.isCompleted -> "최적 페이스"
                            else -> "목표 진행 중"
                        },
                        style = LoorveTypography.labelSmall,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            if (block.description.isNotBlank()) {
                Text(
                    text = block.description,
                    style = LoorveTypography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 2
                )
            }
            val progress = if (block.isCompleted) 1f else 0.55f
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(NoticeContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Notice, Active)
                                )
                            )
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (block.isCompleted) "100%" else "진행 중",
                    style = LoorveTypography.labelSmall,
                    color = if (block.isCompleted) Success else Active
                )
            }
            if (locked) {
                Text(
                    text = "Pro에서 전체 복습 블록을 이용할 수 있습니다.",
                    style = LoorveTypography.bodySmall,
                    color = Warning
                )
            } else {
                Text(
                    text = "탭하여 복습 기록과 일정을 확인하세요",
                    style = LoorveTypography.bodySmall,
                    color = TertiaryText
                )
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