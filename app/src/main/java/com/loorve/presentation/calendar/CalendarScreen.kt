package com.loorve.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.ui.component.*
import com.loorve.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("복습 캘린더",
                        style = LoorveTypography.titleLarge,
                        color = OnBackground)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // 월 선택 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, null, tint = Primary)
                }
                Text(
                    text = currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy년 M월")),
                    style = LoorveTypography.titleMedium,
                    color = OnBackground
                )
                IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, null, tint = Primary)
                }
            }

            // 요일 헤더
            val dayHeaders = listOf("일", "월", "화", "수", "목", "금", "토")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEachIndexed { index, day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = LoorveTypography.labelMedium,
                        color = when (index) {
                            0 -> Secondary
                            6 -> Tertiary
                            else -> OnSurfaceVariant
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 날짜 그리드
            val firstDayOfMonth = currentYearMonth.atDay(1)
            val daysInMonth = currentYearMonth.lengthOfMonth()
            val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

            val calendarCells = buildList {
                repeat(startDayOfWeek) { add(null) }
                for (day in 1..daysInMonth) { add(currentYearMonth.atDay(day)) }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                userScrollEnabled = false
            ) {
                items(calendarCells) { date ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val isToday = date == today
                            val isSelected = date == selectedDate
                            val hasReview = uiState.reviewDates.contains(date)
                            val isExamDay = uiState.examDates.contains(date)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .then(
                                        when {
                                            isToday -> Modifier.background(Primary)
                                            isExamDay -> Modifier.background(Secondary)
                                            hasReview -> Modifier.background(Tertiary.copy(alpha = 0.3f))
                                            isSelected -> Modifier.border(2.dp, Primary, CircleShape)
                                            else -> Modifier
                                        }
                                    )
                                    .clickable {
                                        selectedDate = date
                                        viewModel.selectDate(date)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = LoorveTypography.bodyMedium,
                                    color = when {
                                        isToday || isExamDay -> Color.White
                                        else -> OnBackground
                                    },
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = SurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // 선택 날짜 일정 목록
            if (uiState.selectedDateItems.isEmpty()) {
                EmptyStateView(
                    message = "이 날의 일정이 없습니다",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    lazyItems(uiState.selectedDateItems) { item ->
                        LoorveCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(item.title, style = LoorveTypography.bodyLarge, color = OnBackground)
                                Text(item.subtitle, style = LoorveTypography.labelMedium, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}