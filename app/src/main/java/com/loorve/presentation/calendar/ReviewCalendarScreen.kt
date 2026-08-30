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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.ReviewSchedule
import com.loorve.ui.component.BannerAdView
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
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

    /*
     * 화면 진입 시 현재 월의 reviewSchedules를 읽습니다.
     * AddReviewBlockScreen 성공 후 popBackStack()으로 이 화면에 돌아오면
     * NavBackStackEntry가 다시 활성화되는 구조에서 재조회가 필요할 수 있습니다.
     */
    LaunchedEffect(Unit) {
        reviewCalendarViewModel.refreshUid()
        reviewCalendarViewModel.loadCurrentMonth()
    }

    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = reviewCalendarViewModel::onDismissError,
            title = {
                Text("오류")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = reviewCalendarViewModel::onDismissError
                ) {
                    Text("확인")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("복습 캘린더")
                },
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
            FloatingActionButton(
                onClick = onNavigateToAddReviewBlock
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "복습 블록 생성"
                )
            }
        },
        bottomBar = {
            BannerAdView(
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CalendarHeader(
                yearMonth = uiState.displayYearMonth,
                onPreviousMonth = {
                    reviewCalendarViewModel.onMonthChanged(
                        uiState.displayYearMonth.minusMonths(1)
                    )
                },
                onNextMonth = {
                    reviewCalendarViewModel.onMonthChanged(
                        uiState.displayYearMonth.plusMonths(1)
                    )
                }
            )

            ReviewMonthCalendar(
                yearMonth = uiState.displayYearMonth,
                schedulesMap = uiState.schedulesMap,
                selectedDate = uiState.selectedDate,
                onDateSelected = reviewCalendarViewModel::onDateSelected
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                ReviewScheduleList(
                    selectedDate = uiState.selectedDate,
                    schedules = uiState.selectedDateSchedules,
                    onToggleCompleted = { schedule ->
                        reviewCalendarViewModel.toggleReviewCompletion(
                            scheduleId = schedule.scheduleId,
                            currentState = schedule.isCompleted
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern(
        "yyyy년 M월",
        Locale.KOREAN
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "이전 달"
            )
        }

        Text(
            text = yearMonth.format(formatter),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "다음 달"
            )
        }
    }
}

@Composable
private fun ReviewMonthCalendar(
    yearMonth: YearMonth,
    schedulesMap: Map<LocalDate, List<ReviewSchedule>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay = yearMonth.atDay(1)
    val firstDayOffset = firstDay.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCellCount = firstDayOffset + daysInMonth
    val rowCount = (totalCellCount + 6) / 7

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach { dayLabel ->
                Text(
                    text = dayLabel,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        repeat(rowCount) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                repeat(7) { columnIndex ->
                    val cellIndex = rowIndex * 7 + columnIndex
                    val dayOfMonth = cellIndex - firstDayOffset + 1

                    if (dayOfMonth !in 1..daysInMonth) {
                        Box(
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val date = yearMonth.atDay(dayOfMonth)
                        val schedules = schedulesMap[date].orEmpty()
                        val isSelected = date == selectedDate

                        CalendarDayCell(
                            date = date,
                            scheduleCount = schedules.size,
                            hasIncompleteSchedule = schedules.any { !it.isCompleted },
                            isSelected = isSelected,
                            onClick = {
                                onDateSelected(date)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    scheduleCount: Int,
    hasIncompleteSchedule: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        scheduleCount > 0 -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .padding(2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (scheduleCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .size(6.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            if (hasIncompleteSchedule) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun ReviewScheduleList(
    selectedDate: LocalDate?,
    schedules: List<ReviewSchedule>,
    onToggleCompleted: (ReviewSchedule) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = selectedDate?.format(
                DateTimeFormatter.ofPattern("M월 d일 복습", Locale.KOREAN)
            ) ?: "날짜를 선택해주세요",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        if (selectedDate == null) {
            EmptyScheduleMessage("캘린더에서 날짜를 선택하면 복습 일정을 볼 수 있습니다.")
            return
        }

        if (schedules.isEmpty()) {
            EmptyScheduleMessage("선택한 날짜에는 예정된 복습이 없습니다.")
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 4.dp
            )
        ) {
            items(
                items = schedules,
                key = { it.scheduleId }
            ) { schedule ->
                ReviewScheduleItem(
                    schedule = schedule,
                    onToggleCompleted = {
                        onToggleCompleted(schedule)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

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
                contentDescription = if (schedule.isCompleted) {
                    "복습 완료"
                } else {
                    "복습 미완료"
                },
                tint = if (schedule.isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
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