package com.loorve.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.loorve.ui.theme.Error
import com.loorve.ui.theme.GradientEnd
import com.loorve.ui.theme.GradientMiddle
import com.loorve.ui.theme.GradientStart
import com.loorve.ui.theme.Notice
import com.loorve.ui.theme.NoticeContainer
import com.loorve.ui.theme.OnBackground
import com.loorve.ui.theme.OnSurfaceVariant
import com.loorve.ui.theme.Primary
import com.loorve.ui.theme.Surface
import com.loorve.ui.theme.SurfaceVariant
import com.loorve.ui.theme.SurfaceSolid
import com.loorve.ui.theme.Success
import com.loorve.ui.theme.SuccessContainer
import com.loorve.ui.theme.TertiaryText
import com.loorve.ui.theme.Warning
import com.loorve.ui.theme.WarningContainer
import java.time.LocalDate
import java.time.ZoneId
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
            .background(Color(0xFFFCF9F8))
    ) {
        ReviewDashboardAura()
        Scaffold(
            topBar = {},
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
            bottomBar = {
                val showBanner = uiState.subscriptionEntitlement is SubscriptionEntitlement.Free
                if (showBanner) {
                    key(showBanner) {
                        BannerAdView(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
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
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = "REVIEW",
                                style = LoorveTypography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "복습",
                                style = LoorveTypography.titleLarge.copy(
                                    fontSize = 26.sp,
                                    letterSpacing = (-0.6).sp
                                ),
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    // ── 섹션 1: 날짜별 복습 일정
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "이번 주 복습 페이스",
                                style = LoorveTypography.titleSmall.copy(fontSize = 18.sp),
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "복습 블록 목록",
                                style = LoorveTypography.titleSmall.copy(fontSize = 17.sp),
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                                isDelayed = block.blockId in uiState.delayedBlockIds,
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
    val transition = rememberInfiniteTransition(label = "reviewAmbientOrbs")
    val blueDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20_000), RepeatMode.Reverse),
        label = "reviewBlueDrift"
    )
    val violetDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22_000), RepeatMode.Reverse),
        label = "reviewVioletDrift"
    )
    val cyanDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000), RepeatMode.Reverse),
        label = "reviewCyanDrift"
    )
    val magentaDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000), RepeatMode.Reverse),
        label = "reviewMagentaDrift"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCF9F8))
    ) {
        fun orb(
            center: Offset,
            radius: Float,
            color: Color
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        val blueRadius = 340.dp.toPx()
        val violetRadius = 320.dp.toPx()
        val cyanRadius = 300.dp.toPx()
        val magentaRadius = 280.dp.toPx()

        orb(
            center = Offset(
                x = (-0.25f * size.width) + (30.dp.toPx() * blueDrift),
                y = (-0.05f * size.height) + (40.dp.toPx() * blueDrift)
            ),
            radius = blueRadius,
            color = Color(0xFF2563EB).copy(alpha = 0.22f)
        )
        orb(
            center = Offset(
                x = size.width + (0.25f * size.width) - (35.dp.toPx() * violetDrift),
                y = (0.35f * size.height) - (30.dp.toPx() * violetDrift)
            ),
            radius = violetRadius,
            color = Color(0xFF9333EA).copy(alpha = 0.18f)
        )
        orb(
            center = Offset(
                x = (-0.20f * size.width) + (35.dp.toPx() * cyanDrift),
                y = size.height * 0.85f - (35.dp.toPx() * cyanDrift)
            ),
            radius = cyanRadius,
            color = Color(0xFF38BDF8).copy(alpha = 0.20f)
        )
        orb(
            center = Offset(
                x = size.width * 0.88f - (30.dp.toPx() * magentaDrift),
                y = size.height * 1.05f - (25.dp.toPx() * magentaDrift)
            ),
            radius = magentaRadius,
            color = Color(0xFFEC4899).copy(alpha = 0.15f)
        )
    }
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
    val latestDate = stats.lastOrNull()?.date
    val maxDueCount = stats.maxOfOrNull { it.dueCount }?.coerceAtLeast(1) ?: 1
    val peakDate = stats
        .filter { it.dueCount > 0 }
        .maxByOrNull { it.completedCount.toFloat() / it.dueCount }
        ?.date
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
        color = Color.White.copy(alpha = 0.95f),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "• WEEKLY GRAPH",
                        style = LoorveTypography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 1.1.sp
                        ),
                        color = Color(0xFF4F46E5),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = latestDate?.format(
                            DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)
                        )?.let { dateLabel ->
                            val today = stats.lastOrNull()
                            val completed = today?.completedCount?.coerceIn(0, today.dueCount) ?: 0
                            val due = today?.dueCount ?: 0
                            "$dateLabel · 전체 ${due}개 중 ${completed}개 완료"
                        } ?: "최근 7일 복습 현황",
                        style = LoorveTypography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        ),
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Text(
                        text = "미완료 ${totalRemaining}개",
                        style = LoorveTypography.labelSmall.copy(fontSize = 12.sp),
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEach { stat ->
                    ReviewWorkloadBar(
                        stat = stat,
                        latestDate = latestDate,
                        maxDueCount = maxDueCount,
                        peakDate = peakDate,
                        selected = selectedStat?.date == stat.date,
                        modifier = Modifier.weight(1f),
                        onClick = { onStatSelected(stat) }
                    )
                }
            }
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
            }
    }
}

@Composable
private fun ReviewWorkloadBar(
    stat: DailyReviewCompletionStat,
    latestDate: LocalDate?,
    maxDueCount: Int,
    peakDate: LocalDate?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val completedCount = stat.completedCount.coerceIn(0, stat.dueCount)
    val remainingCount = (stat.dueCount - completedCount).coerceAtLeast(0)
    val totalHeight = 126.dp
    val barHeight = (totalHeight * stat.dueCount.toFloat() / maxDueCount)
        .coerceIn(1.dp, totalHeight)
    val completionRate = if (stat.dueCount > 0) {
        completedCount.toFloat() / stat.dueCount.toFloat()
    } else 0f
    val isToday = stat.date == latestDate
    val isPeak = stat.date == peakDate
    val label = stat.date.chartDateLabel(latestDate)
    val description = if (stat.dueCount <= 0) {
        "$label, 복습 일정 없음"
    } else {
        "$label, 예정 ${stat.dueCount}개, 완료 ${completedCount}개, 미완료 ${remainingCount}개"
    }
    val pulseTransition = rememberInfiniteTransition(label = "purpleBarPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = if (completedCount > 0) 1.02f else 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "purpleBarPulseValue"
    )
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
        Column(
            modifier = Modifier.height(126.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (stat.dueCount > 0) {
                Box(
                    modifier = Modifier
                        .width(if (isToday || selected) 24.dp else 20.dp)
                        .height(barHeight)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (completedCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight * completionRate)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = if (isPeak) {
                                            listOf(
                                                Color(0xFFA21CAF),
                                                Color(0xFF6D28D9),
                                                Color(0xFF6366F1),
                                                Color(0xFF3B82F6),
                                                Color(0xFFA21CAF)
                                            )
                                        } else {
                                            listOf(
                                                Color(0xFF7E22CE),
                                                Color(0xFF4338CA),
                                                Color(0xFF1D4ED8)
                                            )
                                        }
                                    )
                                )
                                .graphicsLayer {
                                    scaleX = pulse
                                    scaleY = pulse
                                    alpha = 0.97f + (pulse - 0.98f) * 0.75f
                                },
                            contentAlignment = Alignment.TopCenter
                        ) {
                        }
                    }
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isToday || isPeak) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isPeak) Color(0xFF7C3AED) else if (isToday || selected) OnBackground else OnSurfaceVariant,
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
    isDelayed: Boolean = false,
    locked: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.92f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Black.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.title.ifBlank { block.examName },
                        style = LoorveTypography.titleSmall.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    if (block.date.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "시험 종료일: ${block.date}",
                            style = LoorveTypography.bodySmall.copy(fontSize = 12.5.sp),
                            color = OnSurfaceVariant
                        )
                    }
                }
                val isEnded = runCatching {
                    LocalDate.now(ZoneId.of("Asia/Seoul")).isAfter(LocalDate.parse(block.date))
                }.getOrDefault(false)
                val badgeColor = when {
                    isEnded -> SurfaceVariant
                    isDelayed -> WarningContainer
                    else -> NoticeContainer
                }
                val badgeTextColor = when {
                    isEnded -> OnSurfaceVariant
                    isDelayed -> Error
                    else -> Primary
                }
                Surface(
                    shape = CircleShape,
                    color = badgeColor
                ) {
                    Text(
                        text = when {
                            isEnded -> "종료"
                            isDelayed -> "지연됨"
                            else -> "진행 중"
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
            Text(
                text = if (block.customIntervalDays != null) {
                    "직접 세팅 복습 주기"
                } else {
                    "에빙하우스 복습 주기"
                },
                style = LoorveTypography.labelMedium.copy(fontSize = 12.5.sp),
                color = if (block.customIntervalDays != null) {
                    OnSurfaceVariant
                } else {
                    Color(0xFF4F46E5)
                },
                fontWeight = FontWeight.SemiBold
            )
            if (locked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFDF2F8),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFFCE7F3)
                    )
                ) {
                    Text(
                        text = "Pro에서 전체 복습 블록을 이용할 수 있습니다.",
                        style = LoorveTypography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFFDB2777),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Text(
                    text = "탭하여 복습 기록과 일정을 확인하세요",
                    style = LoorveTypography.bodySmall,
                    color = TertiaryText
                )
                }
                if (locked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Pro 전용 복습 블록",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
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