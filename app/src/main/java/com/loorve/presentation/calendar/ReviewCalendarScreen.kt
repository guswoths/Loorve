// 경로: app/src/main/java/com/loorve/presentation/calendar/ReviewCalendarScreen.kt
package com.loorve.presentation.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.ReviewSchedule
import com.loorve.ui.component.BannerAdView
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCalendarScreen(
    // nullable로 변경: null이면 탭 진입, non-null이면 독립 라우트 진입
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToAddBlock: () -> Unit = {},
    viewModel: ReviewCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isBannerVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentMonth()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message     = msg,
                actionLabel = "닫기",
                duration    = SnackbarDuration.Long
            )
            viewModel.onDismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("복습") },  // "복습 캘린더" → "복습"으로 변경
                navigationIcon = {
                    // onNavigateBack이 null이 아닐 때만 뒤로가기 아이콘 표시
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            MonthNavigationHeader(
                yearMonth       = uiState.displayYearMonth,
                onPreviousMonth = {
                    viewModel.onMonthChanged(uiState.displayYearMonth.minusMonths(1))
                },
                onNextMonth = {
                    viewModel.onMonthChanged(uiState.displayYearMonth.plusMonths(1))
                }
            )

            WeekDayHeader()

            CalendarGrid(
                yearMonth      = uiState.displayYearMonth,
                schedulesMap   = uiState.schedulesMap,
                selectedDate   = uiState.selectedDate,
                today          = LocalDate.now(),
                onDateSelected = { viewModel.onDateSelected(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SelectedDateSchedulePanel(
                selectedDate       = uiState.selectedDate,
                schedules          = uiState.selectedDateSchedules,
                onCompleteSchedule = { viewModel.onCompleteSchedule(it) },
                onToggleCompletion = { scheduleId, current ->
                    viewModel.toggleReviewCompletion(scheduleId, current)
                },
                isBannerVisible    = isBannerVisible,
                onNavigateToAddBlock = onNavigateToAddBlock,
                modifier           = Modifier.weight(1f)
            )

            BannerAdView(
                modifier   = Modifier.fillMaxWidth(),
                onAdFailed = { isBannerVisible = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AddBlockDashedCard — 복습 블록이 없거나 추가 버튼으로 사용되는 점선 카드
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AddBlockDashedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dashWidth: Dp = 8.dp,
    gapWidth: Dp = 6.dp,
    strokeWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 12.dp
) {
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .drawBehind {
                val dash = dashWidth.toPx()
                val gap  = gapWidth.toPx()
                val sw   = strokeWidth.toPx()
                val cr   = cornerRadius.toPx()
                drawRoundRect(
                    color       = borderColor,
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(
                        width      = sw,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f)
                    )
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.Add,
                contentDescription = "복습블록 추가",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier           = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = "복습블록 추가",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MonthNavigationHeader
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MonthNavigationHeader(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "이전 달"
            )
        }
        Text(
            text  = "${yearMonth.year}년 ${yearMonth.monthValue}월",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "다음 달"
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WeekDayHeader
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeekDayHeader() {
    val days = listOf("일", "월", "화", "수", "목", "금", "토")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text      = day,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color     = when (day) {
                    "일"  -> MaterialTheme.colorScheme.error
                    "토"  -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

// ─────────────────────────────────────────────────────────────────────────────
// CalendarGrid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    schedulesMap: Map<LocalDate, List<ReviewSchedule>>,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset     = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth     = yearMonth.lengthOfMonth()
    val totalCells      = startOffset + daysInMonth
    val rows            = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { colIndex ->
                    val cellIndex = rowIndex * 7 + colIndex
                    val dayNumber = cellIndex - startOffset + 1

                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Box(modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f))
                    } else {
                        val date = yearMonth.atDay(dayNumber)
                        DateCell(
                            day        = dayNumber,
                            isSelected = date == selectedDate,
                            isToday    = date == today,
                            schedules  = schedulesMap[date] ?: emptyList(),
                            isSunday   = colIndex == 0,
                            isSaturday = colIndex == 6,
                            onClick    = { onDateSelected(date) },
                            modifier   = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DateCell
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DateCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    schedules: List<ReviewSchedule>,
    isSunday: Boolean,
    isSaturday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday    -> MaterialTheme.colorScheme.primaryContainer
        else       -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isSunday   -> MaterialTheme.colorScheme.error
        isSaturday -> MaterialTheme.colorScheme.primary
        else       -> MaterialTheme.colorScheme.onSurface
    }

    @Composable
    fun roundColor(roundIndex: Int): Color {
        if (isSelected) return MaterialTheme.colorScheme.onPrimary
        return when (roundIndex) {
            0    -> MaterialTheme.colorScheme.error
            1    -> MaterialTheme.colorScheme.tertiary
            2    -> MaterialTheme.colorScheme.primary
            3    -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.secondary
        }
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text  = day.toString(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize   = 13.sp
            ),
            color = textColor
        )

        if (schedules.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))

            val sortedSchedules  = schedules.sortedBy { it.reviewRound }
            val displaySchedules = sortedSchedules.take(3)
            val hasMore          = sortedSchedules.size > 3

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                displaySchedules.forEach { schedule ->
                    val roundIndex = (schedule.reviewRound - 1).coerceIn(0, 4)
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(roundColor(roundIndex))
                    )
                }
                if (hasMore) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(roundColor(4))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SelectedDateSchedulePanel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SelectedDateSchedulePanel(
    selectedDate: LocalDate?,
    schedules: List<ReviewSchedule>,
    onCompleteSchedule: (String) -> Unit,
    onToggleCompletion: (String, Boolean) -> Unit,
    isBannerVisible: Boolean,
    onNavigateToAddBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (selectedDate != null) {
        "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일 복습 일정"
    } else {
        "날짜를 선택하세요"
    }

    Column(modifier = modifier) {
        Text(
            text     = title,
            style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (selectedDate == null) return@Column

        if (schedules.isEmpty()) {
            // 복습 블록이 없을 때 점선 추가 블록 표시
            AddBlockDashedCard(
                onClick  = onNavigateToAddBlock,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    bottom = if (isBannerVisible) 66.dp else 16.dp
                )
            ) {
                // 생성된 복습 블록 목록 (요구사항 5: 항상 목록 먼저)
                items(schedules) { schedule ->
                    ReviewScheduleItem(
                        schedule   = schedule,
                        onComplete = { onCompleteSchedule(schedule.reviewScheduleId) },
                        onToggle   = { onToggleCompletion(schedule.reviewScheduleId, schedule.isCompleted) }
                    )
                }
                // 항상 목록 마지막에 점선 추가 블록 (요구사항 5)
                item {
                    AddBlockDashedCard(
                        onClick  = onNavigateToAddBlock,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ReviewScheduleItem
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ReviewScheduleItem(
    schedule: ReviewSchedule,
    onComplete: () -> Unit,
    onToggle: () -> Unit
) {
    val titleStyle = if (schedule.isCompleted) {
        MaterialTheme.typography.bodyLarge.copy(
            fontWeight     = FontWeight.SemiBold,
            textDecoration = TextDecoration.LineThrough,
            color          = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (schedule.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked         = schedule.isCompleted,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text  = "${schedule.reviewRound}회차 복습",
                        style = titleStyle
                    )
                    if (schedule.isCompleted) {
                        Text(
                            text  = "완료됨 ✅",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (!schedule.isCompleted) {
                Button(
                    onClick        = onComplete,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    Text("완료", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}