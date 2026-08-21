package com.loorve.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack        // ← 추가
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.ReviewSchedule
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)    // ← TopAppBar 사용을 위해 추가
@Composable
fun ReviewCalendarScreen(
    onNavigateBack: () -> Unit = {},                     // ← 작업 5: 파라미터 추가
    viewModel: ReviewCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
        topBar = {                                       // ← 작업 5: TopAppBar 추가
            TopAppBar(
                title = { Text("복습 캘린더") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
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
                }
            )
        }
    }
}

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
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "이전 달"
            )
        }
        Text(
            text = "${yearMonth.year}년 ${yearMonth.monthValue}월",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "다음 달"
            )
        }
    }
}

@Composable
private fun WeekDayHeader() {
    val days = listOf("일", "월", "화", "수", "목", "금", "토")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = when (day) {
                    "일" -> MaterialTheme.colorScheme.error
                    "토" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    schedulesMap: Map<LocalDate, List<ReviewSchedule>>,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { colIndex ->
                    val cellIndex = rowIndex * 7 + colIndex
                    val dayNumber = cellIndex - startOffset + 1

                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = yearMonth.atDay(dayNumber)
                        DateCell(
                            day         = dayNumber,
                            isSelected  = date == selectedDate,
                            isToday     = date == today,
                            hasSchedule = schedulesMap.containsKey(date),
                            isSunday    = colIndex == 0,
                            isSaturday  = colIndex == 6,
                            onClick     = { onDateSelected(date) },
                            modifier    = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasSchedule: Boolean,
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
        isSelected  -> MaterialTheme.colorScheme.onPrimary
        isSunday    -> MaterialTheme.colorScheme.error
        isSaturday  -> MaterialTheme.colorScheme.primary
        else        -> MaterialTheme.colorScheme.onSurface
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
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = textColor
        )
        if (hasSchedule) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.tertiary
                    )
            )
        }
    }
}

@Composable
private fun SelectedDateSchedulePanel(
    selectedDate: LocalDate?,
    schedules: List<ReviewSchedule>,
    onCompleteSchedule: (String) -> Unit,
    onToggleCompletion: (String, Boolean) -> Unit
) {
    val title = if (selectedDate != null) {
        "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일 복습 일정"
    } else {
        "날짜를 선택하세요"
    }

    Text(
        text     = title,
        style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(bottom = 8.dp)
    )

    if (selectedDate == null) return

    if (schedules.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "이 날은 복습이 없어요 \uD83D\uDE0A",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(schedules) { schedule ->
                ReviewScheduleItem(
                    schedule   = schedule,
                    onComplete = { onCompleteSchedule(schedule.reviewScheduleId) },
                    onToggle   = { onToggleCompletion(schedule.reviewScheduleId, schedule.isCompleted) }
                )
            }
        }
    }
}

@Composable
private fun ReviewScheduleItem(
    schedule: ReviewSchedule,
    onComplete: () -> Unit,
    onToggle: () -> Unit
) {
    val titleStyle = if (schedule.isCompleted) {
        MaterialTheme.typography.bodyLarge.copy(
            fontWeight      = FontWeight.SemiBold,
            textDecoration  = TextDecoration.LineThrough,
            color           = MaterialTheme.colorScheme.onSurfaceVariant
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("완료", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}