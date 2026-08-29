package com.loorve.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.ui.component.*
import com.loorve.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMyPage: () -> Unit,
    onNavigateToExamSetting: () -> Unit,
    onNavigateToProgressDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // ── [수정 ④] 캘린더 표시 월을 별도 state로 분리 ──
    var displayYearMonth by remember { mutableStateOf(YearMonth.now()) }

    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = LocalDate.now().format(
                                DateTimeFormatter.ofPattern("yyyy년 M월 d일 · EEEE",
                                    java.util.Locale.KOREAN)
                            ),
                            style = LoorveTypography.labelMedium,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = "HOME",
                            style = LoorveTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* 알림 */ }) {
                        Icon(Icons.Outlined.Notifications, null, tint = OnBackground)
                    }
                    IconButton(onClick = onNavigateToMyPage) {
                        Icon(Icons.Outlined.AccountCircle, null, tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 1) 모티베이션 헤더 (유지) ──
            item {
                HomeMotivationHeader()
            }

            // ── 2) 전체 복습률 카드 [수정 ①②: 오늘의 문장/주간 진행 바 제거 → 전체 복습률로 교체] ──
            item {
                HomeReviewRateCard(
                    rate = uiState.weeklyCompletionRate,
                    completed = uiState.weeklyCompleted,
                    total = uiState.weeklyTotal
                )
            }

            // ── 3) 복습 스케줄 블록 [수정 ③④: 헤더 강조 + 스와이프 월 이동] ──
            item {
                LoorveCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        // ── [수정 ③] 캘린더 헤더 강조 텍스트 ──
                        Text(
                            text = "${displayYearMonth.monthValue}월 복습 스케줄",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "점이 있는 날짜를 선택해 상세 일정을 확인하세요",
                            style = LoorveTypography.labelSmall,
                            color = OnSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        // ── [수정 ④] 스와이프 제스처로 월 이동 ──
                        HomeMiniCalendar(
                            displayYearMonth = displayYearMonth,
                            selectedDate = selectedDate,
                            scheduledDates = uiState.scheduledDates,
                            onDateSelected = { selectedDate = it },
                            onSwipeLeft = {
                                displayYearMonth = displayYearMonth.plusMonths(1)
                            },
                            onSwipeRight = {
                                displayYearMonth = displayYearMonth.minusMonths(1)
                            }
                        )
                    }
                }
            }

            // ── 4) 선택 날짜의 복습 일정 카드 (유지) ──
            val todaySchedules = uiState.progressList.filter { progress ->
                try {
                    val d = java.time.Instant.ofEpochMilli(progress.createdAt)
                        .atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate()
                    d == selectedDate
                } catch (e: Exception) { false }
            }

            if (todaySchedules.isNotEmpty()) {
                item {
                    Text(
                        text = "${selectedDate.format(DateTimeFormatter.ofPattern("M월 d일"))} · 복습 일정",
                        style = LoorveTypography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(todaySchedules, key = { "sched_${it.id}" }) { progress ->
                    HomeScheduleCard(
                        subjectName = uiState.exams
                            .find { it.id == progress.examId }?.subjectName ?: "",
                        content = progress.content,
                        onStart = { onNavigateToProgressDetail(progress.id) }
                    )
                }
            } else {
                item {
                    Text(
                        text = "${selectedDate.format(DateTimeFormatter.ofPattern("M월 d일"))} · 복습 일정 없음",
                        style = LoorveTypography.bodySmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ── 5) 오늘의 학습 입력 섹션 (유지) ──
            item {
                SectionLabel("오늘의 학습")
                ProgressInputSection(
                    exams = uiState.exams,
                    onSave = { examId, content, completedCount, totalCount ->
                        viewModel.addProgress(examId, content, completedCount, totalCount)
                    }
                )
            }
        }
    }
}

/** 모티베이션 헤더 (유지) */
@Composable
private fun HomeMotivationHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "오늘도 한 칸씩 오래 남기기",
            style = LoorveTypography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OnBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "작게 자주 복습하면, 마지막에 덜 불안해집니다.",
            style = LoorveTypography.bodyMedium,
            color = OnSurfaceVariant
        )
    }
}

/**
 * [수정 ①②] 전체 복습률 카드
 * - "오늘의 문장" 레이블 및 문장 텍스트 제거
 * - "이번 주 N개 중 N개 완료" 텍스트 및 LoorveProgressBar 제거
 * - 원형 차트 + "전체 복습률" 퍼센트만 표시
 */
@Composable
private fun HomeReviewRateCard(
    rate: Float,
    completed: Int,
    total: Int
) {
    val safeRate = if (rate.isNaN() || rate < 0f) 0f else rate.coerceAtMost(1f)
    val percent = (safeRate * 100).toInt()

    val animatedRate by animateFloatAsState(
        targetValue = safeRate,
        animationSpec = tween(durationMillis = 800),
        label = "reviewRateAnim"
    )

    LoorveCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── [수정 ①] "전체 복습률" 헤더 텍스트 ──
            Text(
                text = "전체 복습률",
                style = LoorveTypography.labelMedium,
                color = OnSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // ── [수정 ②] 원형 차트 ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                    drawArc(
                        color = Primary.copy(alpha = 0.15f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    if (animatedRate > 0f) {
                        val gradientBrush = Brush.sweepGradient(
                            colors = listOf(GradientStart, GradientEnd),
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                        drawArc(
                            brush = gradientBrush,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedRate,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // ── [수정 ①] 퍼센트 숫자 표시 ──
                Text(
                    text = "$percent%",
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    fontSize = 28.sp
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "전체 복습률",
                style = LoorveTypography.labelSmall,
                color = OnSurfaceVariant
            )
        }
    }
}

/**
 * [수정 ③④] 미니 달력
 * - displayYearMonth 파라미터 추가 → 표시 월을 외부에서 제어
 * - onSwipeLeft / onSwipeRight 콜백으로 월 이동
 * - 헤더 강조는 호출부(LoorveCard Column)에서 처리
 */
@Composable
private fun HomeMiniCalendar(
    displayYearMonth: YearMonth,
    selectedDate: LocalDate,
    scheduledDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val today = LocalDate.now()
    val firstDayOfWeek = displayYearMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = displayYearMonth.lengthOfMonth()
    val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

    // 스와이프 감지 임계값 (px)
    val swipeThreshold = 50f
    var dragAccumulator by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(displayYearMonth) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        when {
                            dragAccumulator < -swipeThreshold -> onSwipeLeft()
                            dragAccumulator > swipeThreshold  -> onSwipeRight()
                        }
                        dragAccumulator = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    }
                )
            }
    ) {
        // 요일 헤더
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = LoorveTypography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        var day = 1
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    if (cellIndex < firstDayOfWeek || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).height(36.dp))
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
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> Primary
                                        isToday -> Primary.copy(alpha = 0.15f)
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
                                        isSelected -> Color.White
                                        isToday -> Primary
                                        else -> OnBackground
                                    },
                                    fontWeight = if (isSelected || isToday)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasSchedule) {
                                    Spacer(Modifier.height(1.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color.White else Primary
                                            )
                                    )
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

/** 복습 일정 카드 (유지) */
@Composable
private fun HomeScheduleCard(
    subjectName: String,
    content: String,
    onStart: () -> Unit
) {
    LoorveCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "오늘 · $subjectName",
                    style = LoorveTypography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = content,
                    style = LoorveTypography.bodyMedium,
                    color = OnBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "예상 20분",
                        style = LoorveTypography.labelSmall,
                        color = Primary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "·",
                        style = LoorveTypography.labelSmall,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = "지금 바로 시작할 수 있어요",
                        style = LoorveTypography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            FilledIconButton(
                onClick = onStart,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Primary
                )
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "시작", tint = Color.White)
            }
        }
    }
}