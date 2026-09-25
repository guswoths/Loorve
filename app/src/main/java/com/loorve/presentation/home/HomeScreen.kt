package com.loorve.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.presentation.subscription.SubscriptionViewModel
import com.loorve.ui.component.BannerAdView
import com.loorve.ui.component.LoorveCard
import com.loorve.ui.theme.Active
import com.loorve.ui.theme.AiSurface
import com.loorve.ui.theme.Background
import com.loorve.ui.theme.CanvasWarm
import com.loorve.ui.theme.Divider
import com.loorve.ui.theme.GradientEnd
import com.loorve.ui.theme.GradientMiddle
import com.loorve.ui.theme.GradientStart
import com.loorve.ui.theme.LoorveTypography
import com.loorve.ui.theme.Notice
import com.loorve.ui.theme.NoticeContainer
import com.loorve.ui.theme.OnBackground
import com.loorve.ui.theme.OnGradient
import com.loorve.ui.theme.OnSurfaceVariant
import com.loorve.ui.theme.Primary
import com.loorve.ui.theme.Surface
import com.loorve.ui.theme.SurfaceSolid
import com.loorve.ui.theme.Success
import com.loorve.ui.theme.SuccessContainer
import com.loorve.ui.theme.TertiaryText
import com.loorve.ui.theme.UrgentSurface
import com.loorve.ui.theme.Warning
import com.loorve.ui.theme.WarningContainer
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExamSetting: () -> Unit,
    onNavigateToProgressDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subscriptionState by subscriptionViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val completedDates = uiState.reviewSchedules
        .groupBy { it.reviewDate }
        .filter { (_, schedules) ->
            schedules.isNotEmpty() && schedules.all { schedule ->
                schedule.isCompleted
            }
        }
        .keys

    val displayYearMonth by viewModel.displayYearMonth.collectAsState()

    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = OnBackground,
                        contentColor = OnGradient
                    )
                }
            },
            topBar = {},
            bottomBar = {
                val showBanner = subscriptionState.entitlement is SubscriptionEntitlement.Free
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 20.dp,
                    bottom = 144.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "HOME",
                            style = LoorveTypography.labelSmall.copy(
                                fontSize = 12.sp,
                                letterSpacing = 1.5.sp
                            ),
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "홈",
                            style = LoorveTypography.titleLarge.copy(
                                fontSize = 26.sp,
                                letterSpacing = (-0.6).sp
                            ),
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                item {
                    HomeMotivationHeader()
                }

                item {
                    TotalCumulativeReviewCountBlock(
                        points = uiState.cumulativeReviewCounts,
                        completedReviewBlocks = uiState.completedReviewBlocks,
                        ongoingReviewBlocks = uiState.ongoingReviewBlocks,
                        isProSubscribed = subscriptionState.entitlement is SubscriptionEntitlement.Pro
                    )
                }

                item {
                    HomeOverdueReviewSection(
                        schedules = uiState.reviewSchedules,
                        exams = uiState.exams,
                        reviewBlocks = uiState.reviewBlocks,
                        isLoaded = uiState.isReviewSchedulesLoaded,
                        onCheckedChange = viewModel::toggleScheduleCompletion
                    )
                }

                item {
                    LoorveCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.White
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = {
                                    viewModel.setDisplayYearMonth(displayYearMonth.minusMonths(1))
                                }) {
                                    Icon(
                                        Icons.Outlined.ChevronLeft,
                                        contentDescription = "이전 달",
                                        tint = Primary
                                    )
                                }
                                Text(
                                    text = "${displayYearMonth.year}년 ${displayYearMonth.monthValue}월",
                                    style = LoorveTypography.titleMedium,
                                    color = OnBackground,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    viewModel.setDisplayYearMonth(displayYearMonth.plusMonths(1))
                                }) {
                                    Icon(
                                        Icons.Outlined.ChevronRight,
                                        contentDescription = "다음 달",
                                        tint = Primary
                                    )
                                }
                            }
                            Text(
                                text = "복습 일정이 있는 날짜를 선택해 오늘의 계획을 확인하세요",
                                style = LoorveTypography.bodySmall,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    bottom = 12.dp
                                )
                            )
                            HomeMiniCalendar(
                                displayYearMonth = displayYearMonth,
                                selectedDate = selectedDate,
                                scheduledDates = uiState.reviewScheduleDates,
                                completedDates = completedDates,
                                onDateSelected = { selectedDate = it }
                            )

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Divider, thickness = 1.dp)
                            Spacer(Modifier.height(16.dp))

                            val todaySchedules = uiState.reviewSchedules
                                .filter { it.reviewDate == selectedDate }
                                .distinctBy { schedule ->
                                    schedule.scheduleId.ifBlank {
                                        "${schedule.reviewDate}_${schedule.examId}_${schedule.reviewOrder}"
                                    }
                                }

                            Text(
                                text = "${selectedDate.format(DateTimeFormatter.ofPattern("M월 d일"))} · 복습 일정",
                                style = LoorveTypography.titleSmall,
                                color = OnBackground,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (todaySchedules.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    todaySchedules.forEach { schedule ->
                                        val subjectName = schedule.subjectName.ifBlank {
                                            uiState.exams.find { it.id == schedule.examId }?.subjectName
                                                ?: uiState.reviewBlocks.find {
                                                    it.blockId == schedule.examId
                                                }?.examName
                                                ?: ""
                                        }
                                        val scheduleKey = schedule.scheduleId.ifBlank {
                                            "${schedule.reviewDate}_${schedule.examId}_${schedule.reviewOrder}"
                                        }
                                        HomeScheduleCard(
                                            subjectName = subjectName,
                                            content = schedule.content,
                                            checked = schedule.isCompleted,
                                            onCheckedChange = {
                                                viewModel.toggleScheduleCompletion(scheduleKey, it)
                                            }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "선택한 날짜에는 복습 일정이 없습니다.",
                                    style = LoorveTypography.bodySmall,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalCumulativeReviewCountBlock(
    points: List<CumulativeReviewCountPoint>,
    completedReviewBlocks: Int,
    ongoingReviewBlocks: Int,
    isProSubscribed: Boolean
) {
    val dates = if (points.isEmpty()) {
        val today = LocalDate.now()
        (0..6).map { today.minusDays((6 - it).toLong()) }
    } else {
        points.map { it.date }
    }
    val counts = if (points.isEmpty()) List(7) { 0 } else points.map { it.count }
    val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val totalCount = counts.lastOrNull() ?: 0
    val weeklyDiff = totalCount - (counts.firstOrNull() ?: 0)
    var selectedPointIndex by remember(counts) { mutableStateOf(counts.lastIndex) }
    var pulseRequest by remember(counts) { mutableStateOf(0) }
    val pulseProgress = remember { Animatable(0f) }
    val linePulseTransition = rememberInfiniteTransition(label = "reviewChartLinePulse")
    val linePulse by linePulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reviewChartLinePulseValue"
    )

    LaunchedEffect(pulseRequest) {
        if (pulseRequest > 0) {
            pulseProgress.snapTo(0f)
            pulseProgress.animateTo(1f, animationSpec = tween(500))
            pulseProgress.animateTo(0f, animationSpec = tween(500))
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "• TOTAL REVIEWS",
                        style = LoorveTypography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.2.sp),
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "총 누적 복습 횟수",
                        style = LoorveTypography.titleSmall.copy(fontSize = 18.sp),
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        if (isProSubscribed) {
                            ProBadgeLiquid()
                        } else {
                            Surface(shape = CircleShape, color = Color(0xFFF1F5F9)) {
                                Text(
                                    text = "BASIC",
                                    style = LoorveTypography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalCount",
                            style = LoorveTypography.displayMedium.copy(fontSize = 26.sp, lineHeight = 28.sp),
                            color = Primary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "회",
                            style = LoorveTypography.labelLarge,
                            color = Primary,
                            modifier = Modifier.padding(start = 3.dp, bottom = 2.dp)
                        )
                        Text(
                            text = "(+$weeklyDiff 이번 주)",
                            style = LoorveTypography.labelSmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFF6366F1),
                            modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "누적 복습 추이 (최근 7일)",
                    style = LoorveTypography.labelSmall.copy(fontSize = 11.sp),
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "기준: ${dates.first().format(DateTimeFormatter.ofPattern("M/d"))} ~ ${dates.last().format(DateTimeFormatter.ofPattern("M/d"))}",
                    style = LoorveTypography.labelSmall.copy(fontSize = 11.sp),
                    color = Color(0xFF94A3B8)
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                val chartPoints = remember(counts, maxCount) {
                    counts.mapIndexed { index, count ->
                        index to count
                    }
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .pointerInput(chartPoints) {
                            detectTapGestures { tapOffset ->
                                val horizontalPadding = 8.dp.toPx()
                                val chartWidth = size.width - horizontalPadding * 2
                                val xStep = chartWidth / (chartPoints.size - 1).coerceAtLeast(1)
                                val nearestIndex = ((tapOffset.x - horizontalPadding) / xStep)
                                    .roundToInt()
                                    .coerceIn(0, chartPoints.lastIndex)
                                selectedPointIndex = nearestIndex
                                pulseRequest++
                            }
                        }
                ) {
                    val horizontalPadding = 8.dp.toPx()
                    val verticalPadding = 8.dp.toPx()
                    val chartWidth = size.width - horizontalPadding * 2
                    val chartHeight = size.height - verticalPadding * 2
                    val xStep = chartWidth / (chartPoints.size - 1).coerceAtLeast(1)
                    val pointsInChart = chartPoints.map { (index, count) ->
                        androidx.compose.ui.geometry.Offset(
                            x = horizontalPadding + xStep * index,
                            y = verticalPadding + chartHeight -
                                (count.toFloat() / maxCount) * (chartHeight - 4.dp.toPx())
                        )
                    }

                    if (pointsInChart.size > 1) {
                        val linePath = Path().apply {
                            moveTo(pointsInChart.first().x, pointsInChart.first().y)
                            pointsInChart.windowed(2).forEach { (start, end) ->
                                val midpointX = (start.x + end.x) / 2f
                                cubicTo(
                                    midpointX,
                                    start.y,
                                    midpointX,
                                    end.y,
                                    end.x,
                                    end.y
                                )
                            }
                        }
                        val areaPath = Path().apply {
                            addPath(linePath)
                            lineTo(pointsInChart.last().x, verticalPadding + chartHeight)
                            lineTo(pointsInChart.first().x, verticalPadding + chartHeight)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                listOf(Color(0x383B82F6), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF3B82F6), Color(0xFF6366F1))
                            ),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawPath(
                            path = linePath,
                            color = Color(0xFF60A5FA).copy(alpha = 0.08f + linePulse * 0.08f),
                            style = Stroke(width = (8f + linePulse * 2f).dp.toPx())
                        )
                    }
                    pointsInChart.forEachIndexed { index, point ->
                        if (index == selectedPointIndex) {
                            drawCircle(
                                color = Color(0xFF6366F1).copy(
                                    alpha = 0.18f * (1f - pulseProgress.value)
                                ),
                                radius = (8f + pulseProgress.value * 12f).dp.toPx(),
                                center = point
                            )
                        }
                        drawCircle(
                            color = Color.White,
                            radius = if (index == pointsInChart.lastIndex) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = if (index == pointsInChart.lastIndex) {
                                Color(0xFF6366F1)
                            } else {
                                Color(0xFF3B82F6)
                            },
                            radius = if (index == pointsInChart.lastIndex) {
                                4.dp.toPx()
                            } else {
                                2.dp.toPx()
                            },
                            center = point
                        )
                    }
                }

                selectedPointIndex.let { index ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (maxWidth * index / (dates.lastIndex.coerceAtLeast(1))) - 20.dp,
                                y = 0.dp
                            ),
                        shape = CircleShape,
                        color = Color(0xFF0F172A),
                        shadowElevation = 3.dp
                    ) {
                        Text(
                            text = "${counts[index]}회",
                            style = LoorveTypography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dates.forEach { date ->
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("M/d")),
                            style = LoorveTypography.labelSmall.copy(
                                fontSize = 11.sp,
                                letterSpacing = 0.sp,
                                fontWeight = if (date == dates.last()) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                }
                            ),
                            color = if (date == dates.last()) {
                                Color(0xFF1A73E8)
                            } else {
                                Color(0xFF94A3B8)
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(18.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReviewBlockCount(
                    label = "완료된 복습 블록",
                    count = completedReviewBlocks,
                    detail = "(시험종료일 경과)",
                    modifier = Modifier.weight(1f)
                )
                ReviewBlockCount(
                    label = "진행 중인 복습 블록",
                    count = ongoingReviewBlocks,
                    detail = "(시험종료일 미도달)",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReviewBlockCount(
    label: String,
    count: Int,
    detail: String,
    modifier: Modifier = Modifier
) {
    val isOngoing = label.startsWith("진행")
    val pulseTransition = rememberInfiniteTransition(label = "ongoingReviewDotPulse")
    val dotScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOngoing) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ongoingReviewDotScale"
    )
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOngoing) 0.55f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ongoingReviewDotAlpha"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(
                1.dp,
                if (label.startsWith("진행")) Color(0x66E9D5FF) else Color(0x99F1F5F9),
                RoundedCornerShape(14.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isOngoing) 18.dp else 8.dp)
                    .drawBehind {
                        if (isOngoing) {
                            drawCircle(
                                color = Color(0xFF2563EB).copy(alpha = 0.22f * dotAlpha),
                                radius = size.minDimension * 0.48f * dotScale
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOngoing) Color(0xFF2563EB) else Color(0xFF8B5CF6)
                        )
                )
            }
            Text(
                text = label,
                style = LoorveTypography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.sp),
                color = Color(0xFF475569),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = count.toString(),
                style = LoorveTypography.titleMedium.copy(fontSize = 20.sp, lineHeight = 20.sp),
                color = if (isOngoing) Color(0xFF1D4ED8) else Color(0xFF0F172A),
                fontWeight = FontWeight.Black
            )
            Text(
                text = "개",
                style = LoorveTypography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 0.sp),
                color = if (isOngoing) Color(0xFF1D4ED8) else Color(0xFF475569),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            Text(
                text = detail,
                style = LoorveTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun ProBadgeLiquid() {
    val transition = rememberInfiniteTransition(label = "proBadge")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(6000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "proBadgeShift"
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF7DD3FC)),
                    start = androidx.compose.ui.geometry.Offset(shift * 80f, 0f),
                    end = androidx.compose.ui.geometry.Offset(120f + shift * 80f, 40f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .drawWithCache {
                val sheenX = size.width * (-0.8f + shift * 2.4f)
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(sheenX - size.width, 0f),
                            end = androidx.compose.ui.geometry.Offset(sheenX, size.height)
                        )
                    )
                }
            }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = "PRO",
            style = LoorveTypography.labelSmall.copy(
                fontSize = 10.5.sp,
                letterSpacing = 0.8.sp
            ),
            color = Color.White
        )
    }
}

@Suppress("unused")
@Composable
private fun HomeAmbientBackground() {
    val transition = rememberInfiniteTransition(label = "homeAmbient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20_000), RepeatMode.Reverse),
        label = "homeAmbientDrift"
    )
    Canvas(Modifier.fillMaxSize()) {
        fun orb(center: androidx.compose.ui.geometry.Offset, radius: Float, color: Color) {
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
        val r1 = 180.dp.toPx()
        val r2 = 170.dp.toPx()
        val r3 = 160.dp.toPx()
        val r4 = 150.dp.toPx()
        // 02 Vivid Sky Blue (Top Left)
        orb(
            androidx.compose.ui.geometry.Offset((-70 + 30 * drift).dp.toPx() + r1, -40.dp.toPx() + r1),
            r1,
            Color(0x3838BDF8)
        )
        // 03 Aero Cyan Tint (Right Mid)
        orb(
            androidx.compose.ui.geometry.Offset(size.width + 70.dp.toPx() - r2, size.height * .36f),
            r2,
            Color(0x337DD3FC)
        )
        // 01 Electric Sky Azure (Bottom Left)
        orb(
            androidx.compose.ui.geometry.Offset((-30 + 25 * drift).dp.toPx() + r3, size.height - 40.dp.toPx()),
            r3,
            Color(0x2E0284C7)
        )
        // 04 Glacier Ice Mist (Bottom Right)
        orb(
            androidx.compose.ui.geometry.Offset(size.width - 20.dp.toPx() - r4, size.height * 0.82f),
            r4,
            Color(0x3DBAE6FD)
        )
    }
}

@Composable
private fun HomeHeroCard(
    weeklyCompletionRate: Float,
    weeklyCompleted: Int,
    weeklyTotal: Int,
    nearestExam: NearestExamUiModel?,
    onOpenSettings: () -> Unit
) {
    val percentage = (weeklyCompletionRate.coerceIn(0f, 1f) * 100).toInt()
    val gradient = Brush.linearGradient(
        colors = listOf(GradientStart, GradientMiddle, GradientEnd)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Surface,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
        tonalElevation = 1.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "이번 주 기억 유지율",
                        style = LoorveTypography.labelMedium,
                        color = OnSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$percentage%",
                        style = LoorveTypography.displayMedium,
                        color = OnBackground
                    )
                    Text(
                        text = "$weeklyCompleted / $weeklyTotal 복습 완료",
                        style = LoorveTypography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = NoticeContainer
                ) {
                    Text(
                        text = if (percentage >= 80) "최적 페이스" else "페이스 확인",
                        style = LoorveTypography.labelSmall,
                        color = Notice,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(NoticeContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(weeklyCompletionRate.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(gradient)
                    )
                }
                Text(
                    text = "7일",
                    style = LoorveTypography.labelSmall,
                    color = OnSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(gradient)
                            .clickable(onClick = onOpenSettings)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "복습 계획 관리",
                            style = LoorveTypography.labelLarge,
                            color = OnGradient,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                nearestExam?.let { exam ->
                    Surface(
                        shape = CircleShape,
                        color = SurfaceSolid,
                        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = if (exam.daysLeft == 0) "D-Day" else "D-${exam.daysLeft}",
                            style = LoorveTypography.labelMedium,
                            color = Active,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMotivationHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFFEFF6FF)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FormatQuote,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column {
                Text(
                    text = "반복은 기억을 단단하게 다지는 망치질과 같다.",
                    style = LoorveTypography.bodyLarge.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "- 퀸틸리아누스",
                    style = LoorveTypography.labelMedium.copy(fontSize = 12.sp),
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private data class HomeMotivationalQuote(
    val text: String,
    val author: String
)

private val HOME_MOTIVATIONAL_QUOTES = listOf(
    HomeMotivationalQuote("학이시습지 불역열호(배우고 때때로 익히면 또한 기쁘지 아니한가).", "공자"),
    HomeMotivationalQuote("온고이지신 위사가의(옛것을 익혀 새것을 알면 스승이 될 수 있다).", "공자"),
    HomeMotivationalQuote("반복은 모든 학습의 어머니다(Repetitio est mater studiorum).", "라틴 격언"),
    HomeMotivationalQuote("우리가 반복적으로 하는 행동이 바로 우리 자신이다. 따라서 탁월함은 행동이 아니라 습관이다.", "윌 듀란트 (아리스토텔레스 사상 해설)"),
    HomeMotivationalQuote("성공은 매일 반복되는 작은 노력들의 합이다.", "로버트 콜리어"),
    HomeMotivationalQuote("반복은 설득의 유일한 형태다.", "나폴레옹 보나파르트"),
    HomeMotivationalQuote("한 권의 책을 백 번 읽으면 그 뜻이 저절로 드러난다(독서백편의자현).", "주희"),
    HomeMotivationalQuote("배우기만 하고 생각하지 않으면 얻는 것이 없고, 생각하기만 하고 배우지 않으면 위태롭다.", "공자"),
    HomeMotivationalQuote("한 번의 시선은 단지 지각일 뿐이지만, 반복된 시선은 이해가 된다.", "괴테"),
    HomeMotivationalQuote("나는 만 가지 발차기를 한 번씩 연습한 사람은 두렵지 않지만, 한 가지 발차기를 만 번 연습한 사람은 두렵다.", "이소룡"),
    HomeMotivationalQuote("연습은 완벽을 만들지 않는다. 완벽한 연습만이 완벽을 만든다.", "빈스 롬바르디"),
    HomeMotivationalQuote("지식은 적용할 때까지는 단지 잠재적인 힘에 불과하다.", "나폴레온 힐"),
    HomeMotivationalQuote("배움의 과정에서 가장 치명적인 오류는 한 번 이해한 것을 완전히 안다고 착각하는 것이다.", "헤르만 에빙하우스"),
    HomeMotivationalQuote("복습하지 않는 공부는 밑 빠진 독에 물 붓기다.", "율곡 이이"),
    HomeMotivationalQuote("기억을 지속시키는 유일한 도구는 주기적인 회상이다.", "윌리엄 제임스"),
    HomeMotivationalQuote("천재성이란 끊임없이 반복하는 인내력에 불과하다.", "뷔퐁"),
    HomeMotivationalQuote("처음 읽을 때는 배우고, 두 번째 읽을 때는 깊어지며, 세 번째 읽을 때는 비판하게 된다.", "몽테뉴"),
    HomeMotivationalQuote("망각에 대항하는 유일한 무기는 규칙적인 반복이다.", "헤르만 에빙하우스"),
    HomeMotivationalQuote("반복은 기억을 단단하게 다지는 망치질과 같다.", "퀸틸리아누스"),
    HomeMotivationalQuote("새로운 지식을 얻는 가장 좋은 방법은 이미 배운 지식을 다시 검토하는 것이다.", "소크라테스"),
    HomeMotivationalQuote("이미 안다고 생각하는 순간 배움은 멈춘다.", "클로드 베르나르"),
    HomeMotivationalQuote("배움은 끝없는 복습의 연속이다. 익숙함이 통찰로 변하는 순간까지 멈추지 마라.", "다산 정약용"),
    HomeMotivationalQuote("반복되지 않은 정보는 뇌에 머물지 않고 스쳐 지나갈 뿐이다.", "존 메디나"),
    HomeMotivationalQuote("한 번 배운 것을 마음에 새기지 않으면, 아무리 책을 많이 읽어도 빈 껍데기에 불과하다.", "퇴계 이황"),
    HomeMotivationalQuote("반복은 예술의 비밀이자 모든 기술의 열쇠다.", "알브레히트 뒤러"),
    HomeMotivationalQuote("하루를 연습하지 않으면 내가 알고, 이틀을 연습하지 않으면 비평가가 알고, 사흘을 연습하지 않으면 관객이 안다.", "야샤 하이페츠"),
    HomeMotivationalQuote("복습은 지식을 지혜로 바꾸는 연금술이다.", "세네카"),
    HomeMotivationalQuote("우리는 보고 들은 것의 일부만 기억하지만, 스스로 되새기고 행동한 것은 온전히 기억한다.", "벤자민 프랭클린"),
    HomeMotivationalQuote("천 번의 연습이 곧 숙련을 낳는다.", "미야모토 무사시"),
    HomeMotivationalQuote("한 문장을 열 번 읽으면 문자가 보이고, 백 번 읽으면 뜻이 보이며, 천 번 읽으면 삶이 보인다.", "김득신"),
    HomeMotivationalQuote("탁월함은 재능이 아니라 끊임없는 되새김과 훈련의 결과다.", "키케로"),
    HomeMotivationalQuote("기억하려는 노력 없이 머릿속에 들어오는 지식은 쉽게 사라진다.", "존 로크"),
    HomeMotivationalQuote("지식을 소유하는 것과 그것을 자유자재로 꺼내 쓰는 것은 완전히 다른 차원의 일이다.", "아르투어 쇼펜하우어"),
    HomeMotivationalQuote("같은 길을 여러 번 걸어야 비로소 주변의 풍경이 세세히 보인다.", "프리드리히 니체"),
    HomeMotivationalQuote("지혜는 하루아침에 쌓이지 않으며, 어제의 배움을 오늘 다시 확인하는 과정에서 자란다.", "솔론"),
    HomeMotivationalQuote("단련이란 일천 날의 연습을 '단'이라 하고, 사만 날의 연습을 '련'이라 한다.", "미야모토 무사시"),
    HomeMotivationalQuote("한 번 읽은 책은 결코 온전히 읽은 것이 아니다.", "버지니아 울프"),
    HomeMotivationalQuote("숙련은 반복에 지루함을 느끼지 않는 사람에게 주어지는 보상이다.", "콜린 파월"),
    HomeMotivationalQuote("배운 것을 입 밖으로 소리 내어 말해보고 다시 정리하지 않는다면 진짜 지식이 아니다.", "리처드 파인만"),
    HomeMotivationalQuote("우리의 두뇌는 반복을 통해 경로를 다지고 고속도로를 건설한다.", "산티아고 라몬 이 카할"),
    HomeMotivationalQuote("학문이란 강물을 거슬러 올라가는 배와 같아서, 복습하여 나아가지 않으면 곧 퇴보한다.", "한비자"),
    HomeMotivationalQuote("가장 훌륭한 복습은 타인에게 그것을 가르쳐보는 것이다.", "세네카"),
    HomeMotivationalQuote("배운 것을 되새기지 않는 지식인은 씨앗만 뿌려두고 수확하지 않는 농부와 같다.", "페스탈로치"),
    HomeMotivationalQuote("기억은 게으른 하인과 같아서, 끊임없이 부르고 깨우지 않으면 잠들어 버린다.", "새뮤얼 존슨"),
    HomeMotivationalQuote("복습은 과거로 돌아가는 것이 아니라, 더 높은 곳에서 어제의 지식을 내려다보는 것이다.", "앙리 베르그송"),
    HomeMotivationalQuote("한 번의 실천적 복습이 백 번의 맹목적 독서보다 낫다.", "존 듀이"),
    HomeMotivationalQuote("거장은 기초적인 동작을 남들보다 훨씬 더 많이, 더 깊이 반복한 사람일 뿐이다.", "파블로 카잘스"),
    HomeMotivationalQuote("어제 배운 것을 오늘 다시 보지 않는다면 내일은 흔적조차 남지 않는다.", "순자"),
    HomeMotivationalQuote("반복은 평범함을 비범함으로 바꾸는 가장 단순한 공식이다.", "짐 론"),
    HomeMotivationalQuote("배움의 즐거움은 처음 알게 되었을 때가 아니라, 다시 꺼내어 완전히 내 것이 되었을 때 찾아온다.", "에라스뮈스"),
    HomeMotivationalQuote("이해했다고 느끼는 순간이야말로 복습을 시작해야 할 가장 위험하고도 중요한 순간이다.", "바루흐 스피노자"),
    HomeMotivationalQuote("훈련의 고통은 잠시지만, 반복하지 않아 생기는 무지는 평생 간다.", "에픽테토스"),
    HomeMotivationalQuote("글을 쓸 때 고쳐 쓰는 것(퇴고)이 핵심이듯, 공부의 핵심은 다시 보는 것에 있다.", "어니스트 헤밍웨이"),
    HomeMotivationalQuote("지식의 진정한 깊이는 얼마나 많은 것을 새로 접했느냐가 아니라, 배운 것을 얼마나 깊이 되새겼느냐에 달려 있다.", "르네 데카르트"),
    HomeMotivationalQuote("복습하지 않는 학생은 도끼날을 갈지 않고 나무를 베려는 나무꾼과 같다.", "스티븐 코비"),
    HomeMotivationalQuote("우리는 잊어버리기 위해 기억하는 것이 아니므로, 끊임없이 지식을 다듬고 점검해야 한다.", "마르쿠스 아우렐리우스"),
    HomeMotivationalQuote("지속적인 점검과 피드백 없는 학습은 방향타 없는 배와 같다.", "노버트 위너"),
    HomeMotivationalQuote("복습은 이미 완성된 그림에 명암을 더해 입체감을 불어넣는 작업이다.", "레오나르도 다빈치"),
    HomeMotivationalQuote("한 번에 많은 것을 배우려 하지 말고, 적은 분량이라도 완벽히 숙달될 때까지 거듭하라.", "토마스 아퀴나스"),
    HomeMotivationalQuote("반복은 단순한 노동이 아니라 뇌의 구조를 물리적으로 바꾸는 신경학적 건축 작업이다.", "도널드 헵"),
    HomeMotivationalQuote("복습은 자신이 무엇을 모르는지 발견하는 가장 정직한 거울이다.", "미셸 드 몽테뉴"),
    HomeMotivationalQuote("천 번이고 만 번이고 거듭 생각하여 이치에 닿을 때까지 손에서 놓지 마라.", "왕양명"),
    HomeMotivationalQuote("기억의 궁전을 튼튼하게 세우려면 기초 벽돌을 쌓은 뒤 틈틈이 시멘트를 덧발라야 한다.", "마테오 리치"),
    HomeMotivationalQuote("자주 돌아보는 자만이 길을 잃지 않는다.", "노자"),
    HomeMotivationalQuote("연습이란 같은 일을 지루함 없이 새롭게 해내는 능력이다.", "블라디미르 호로비츠"),
    HomeMotivationalQuote("지식은 소화되지 않으면 독이 되며, 지식을 소화시키는 유일한 위장은 복습이다.", "장 자크 루소"),
    HomeMotivationalQuote("어떤 개념을 완벽히 이해했다는 증거는, 그것을 보지 않고도 백지에 처음부터 끝까지 설명해낼 수 있는 상태다.", "리처드 파인만"),
    HomeMotivationalQuote("어제의 나를 넘어서는 공부는 새로운 책을 펼치는 것이 아니라, 어제 덮었던 책의 핵심을 다시 짚어보는 데서 시작한다.", "랄프 왈도 에머슨"),
    HomeMotivationalQuote("배움에 지름길은 없으며, 되풀이해 걷는 길만이 단단한 대로가 된다.", "유클리드"),
    HomeMotivationalQuote("복습이란 흩어진 생각의 구슬을 실로 꿰어 보배로 만드는 일이다.", "이덕무"),
    HomeMotivationalQuote("반복을 두려워하는 사람은 결코 자신의 한계를 넘어설 수 없다.", "에밀 자토펙"),
    HomeMotivationalQuote("공부란 배운 것을 마음에 담아두고 삭여서 마침내 뼈와 살이 되게 하는 것이다.", "박지원"),
    HomeMotivationalQuote("자신의 지식을 주기적으로 점검하지 않는 전문가는 시계를 맞추지 않고 시간을 재는 사람과 같다.", "피터 드러커"),
    HomeMotivationalQuote("지혜로운 자는 이미 배운 기초를 매일 아침 새롭게 다진다.", "달라이 라마"),
    HomeMotivationalQuote("기억의 힘은 머리의 총명함에 있지 않고, 끈질기게 되뇌는 혀끝과 손끝에 있다.", "정조"),
    HomeMotivationalQuote("한 번의 깨달음에 만족하지 말고, 그 깨달음을 일상의 생각으로 끌어내리기 위해 복기하라.", "지눌"),
    HomeMotivationalQuote("학습은 마라톤과 같아서, 앞선 구간의 페이스를 점검하지 않으면 결승선에 도달할 수 없다.", "아베베 비킬라"),
    HomeMotivationalQuote("인간의 지적 능력은 정보를 습득하는 속도가 아니라, 습득한 정보를 회상하고 재조합하는 능력으로 결정된다.", "허버트 사이먼"),
    HomeMotivationalQuote("지식의 나무에 물을 주는 행위가 바로 복습이다. 물을 주지 않으면 아무리 큰 나무라도 말라 죽는다.", "페스탈로치"),
    HomeMotivationalQuote("반복이 없으면 습관이 생기지 않고, 습관이 없으면 성격도, 운명도 바뀌지 않는다.", "윌리엄 제임스"),
    HomeMotivationalQuote("훌륭한 사상가는 끊임없이 자신의 전제를 의심하고 처음부터 다시 생각해보는 사람이다.", "루트비히 비트겐슈타인"),
    HomeMotivationalQuote("익숙한 길도 다시 확인하며 걸어야 돌부리에 걸려 넘어지지 않는다.", "명심보감"),
    HomeMotivationalQuote("학습의 최종 목표는 무의식적인 숙련이며, 무의식적 숙련에 이르는 유일한 다리는 끊임없는 반복이다.", "칼 융"),
    HomeMotivationalQuote("모든 위대한 대가들은 가장 단순한 기본기를 평생 동안 복습한 사람들이다.", "미켈란젤로"),
    HomeMotivationalQuote("단 한 줄의 문장이라도 내 삶의 원칙이 될 때까지 곱씹지 않는다면 책을 읽지 않은 것과 같다.", "헨리 데이비드 소로"),
    HomeMotivationalQuote("기억은 붙잡지 않으면 날아가는 새와 같다. 복습이라는 새장을 만들어 지식을 가두어라.", "프랜시스 베이컨"),
    HomeMotivationalQuote("생각의 회로는 자주 달릴수록 저항이 줄어든다.", "올리버 색스"),
    HomeMotivationalQuote("복습은 지식에 영혼을 불어넣어 그것이 내 언어가 되게 하는 과정이다.", "요한 볼프강 폰 괴테"),
    HomeMotivationalQuote("알고 있는 것을 다시 검토할 때, 우리는 비로소 그 지식의 진정한 한계와 확장을 본다.", "임마누엘 칸트"),
    HomeMotivationalQuote("기초를 거듭 다지는 사람만이 폭풍 속에서도 흔들리지 않는 지적 탑을 쌓을 수 있다.", "아이작 뉴턴"),
    HomeMotivationalQuote("한 번의 정독보다 세 번의 간헐적 복습이 뇌리에 훨씬 더 깊은 자국을 남긴다.", "찰스 다윈"),
    HomeMotivationalQuote("반복은 결코 낭비가 아니다. 그것은 숙련이라는 이름의 조각상을 빚어내는 정질이다.", "오귀스트 로댕"),
    HomeMotivationalQuote("지식을 머릿속에 쌓아두기만 하고 정리하지 않는 것은 창고에 물건을 마구 쑤셔 넣는 것과 같다.", "조지프 애디슨"),
    HomeMotivationalQuote("되돌아보아 틀린 부분을 바로잡는 것, 이것이 배움의 본질이다.", "증자"),
    HomeMotivationalQuote("매일 반복하는 일이야말로 진정으로 우리가 누구인지를 결정짓는다.", "파울로 코엘료"),
    HomeMotivationalQuote("깊이 파기 위해서는 같은 자리를 파고 또 파야 한다. 복습이야말로 지식의 샘을 터뜨리는 곡괭이다.", "바루흐 스피노자"),
    HomeMotivationalQuote("배움의 과정에서 가장 위대한 순간은 이미 안다고 믿었던 것을 복습하며 새로운 깊이를 발견할 때다.", "마틴 부버"),
    HomeMotivationalQuote("숙련에 도달하는 데 기적이 끼어들 자리는 없다. 오직 정직한 반복만이 존재할 뿐이다.", "아르투로 토스카니니"),
    HomeMotivationalQuote("이미 읽은 것을 다시 읽을 줄 모르는 사람은 책을 읽을 자격이 없다.", "오스카 와일드"),
    HomeMotivationalQuote("날마다 복습하여 잊지 않게 하는 것이야말로 둔한 자가 영리한 자를 이기는 유일한 비결이다.", "율곡 이이")
)

@Composable
private fun HomeOverdueReviewSection(
    schedules: List<ReviewScheduleUiModel>,
    exams: List<com.loorve.domain.model.Exam>,
    reviewBlocks: List<ReviewBlockUiModel>,
    isLoaded: Boolean,
    onCheckedChange: (String, Boolean) -> Unit
) {
    val today = LocalDate.now()
    val overdueSchedules = schedules
        .filter { it.reviewDate.isBefore(today) && !it.isCompleted }
        .sortedWith(
            compareBy<ReviewScheduleUiModel> { it.reviewDate }
                .thenBy { it.reviewOrder }
                .thenBy { it.scheduleId }
        )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
        tonalElevation = 1.dp,
        shadowElevation = 6.dp
    ) {
        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 152.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (overdueSchedules.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = SuccessContainer
                ) {
                    Text(
                        text = "✓",
                        style = LoorveTypography.titleMedium,
                        color = Success,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "지연된 복습이 없습니다",
                        style = LoorveTypography.titleSmall,
                        color = OnBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "현재 복습 페이스가 안정적입니다.",
                        style = LoorveTypography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF43F5E), Color(0xFFFBBF24), Color(0xFF6366F1))
                            )
                        )
                )
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Surface(shape = CircleShape, color = Color(0xFFFFF1F2)) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "기억이 흐려지기 전에 확인하세요",
                            style = LoorveTypography.titleSmall.copy(fontSize = 15.5.sp),
                            color = Color(0xFFE11D48),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${overdueSchedules.size}개의 복습 일정이 지연되었습니다.",
                            style = LoorveTypography.bodySmall.copy(fontSize = 12.5.sp),
                            color = OnSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                overdueSchedules.forEach { schedule ->
                    val subjectName = schedule.subjectName.ifBlank {
                        exams.find { it.id == schedule.examId }?.subjectName
                            ?: reviewBlocks.find { it.blockId == schedule.examId }?.examName
                            ?: ""
                    }
                    val scheduleKey = schedule.scheduleId.ifBlank {
                        "${schedule.reviewDate}_${schedule.examId}_${schedule.reviewOrder}"
                    }
                    HomeScheduleCard(
                        subjectName = subjectName,
                        content = schedule.content,
                        dateLabel = schedule.reviewDate.format(
                            DateTimeFormatter.ofPattern("M월 d일")
                        ),
                        checked = schedule.isCompleted,
                        onCheckedChange = { onCheckedChange(scheduleKey, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMiniCalendar(
    displayYearMonth: YearMonth,
    selectedDate: LocalDate,
    scheduledDates: Set<LocalDate>,
    completedDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDayOfWeek = displayYearMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = displayYearMonth.lengthOfMonth()
    val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = LoorveTypography.labelSmall,
                    color = when (index) {
                        0 -> Warning
                        6 -> Primary
                        else -> OnSurfaceVariant
                    }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        var day = 1
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    if (cellIndex < firstDayOfWeek || day > daysInMonth) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        )
                    } else {
                        val currentDay = day
                        val date = displayYearMonth.atDay(currentDay)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val hasSchedule = scheduledDates.contains(date)
                        day++
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> Primary
                                        isToday -> NoticeContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$currentDay",
                                    style = LoorveTypography.labelMedium,
                                    color = when {
                                        isSelected -> OnGradient
                                        isToday -> Primary
                                        else -> OnBackground
                                    },
                                    fontWeight = if (isSelected || isToday) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                                if (hasSchedule) {
                                    Spacer(Modifier.height(2.dp))
                                    val isCompleted = completedDates.contains(date)
                                    val dotColor = if (isSelected) Color(0xFF93C5FD) else Color(0xFF2563EB)
                                    if (isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .border(
                                                    BorderStroke(1.5.dp, dotColor),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun HomeScheduleCard(
    subjectName: String,
    content: String,
    dateLabel: String = "오늘",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceSolid,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) Color(0xFF2563EB) else Color.Transparent
                    )
                    .border(
                        BorderStroke(
                            width = 1.5.dp,
                            color = if (checked) Color(0xFF2563EB) else Color(0xFF60A5FA)
                        ),
                        CircleShape
                    )
                    .clickable { onCheckedChange(!checked) },
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "완료됨",
                        tint = OnGradient,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                val headerTitle = if (subjectName.isNotBlank()) {
                    "$dateLabel · $subjectName"
                } else {
                    "$dateLabel · 복습 일정"
                }
                Text(
                    text = headerTitle,
                    style = LoorveTypography.labelMedium,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = content,
                    style = LoorveTypography.bodyMedium,
                    color = Color(0xFF1D4ED8),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (checked) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    }
                )
            }
        }
    }
}
