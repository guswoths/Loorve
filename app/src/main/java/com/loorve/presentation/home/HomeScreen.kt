package com.loorve.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.ui.component.*
import com.loorve.ui.theme.*
// ✅ [원인3 수정] java.time 패키지를 명시적으로 import — Firebase DataConnect의 LocalDate와 충돌 방지
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
// ❌ import com.google.firebase.dataconnect.LocalDate  ← 이 줄이 있다면 반드시 제거

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
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 1) 모티베이션 헤더 ──
            item {
                HomeMotivationHeader()
            }

            // ── 2) 지연된 복습 영역 ──
            item {
                HomeOverdueReviewSection(
                    schedules = uiState.reviewSchedules,
                    exams = uiState.exams,
                    reviewBlocks = uiState.reviewBlocks,
                    isLoaded = uiState.isReviewSchedulesLoaded,
                    onCheckedChange = viewModel::toggleScheduleCompletion
                )
            }

            // ── 3) 복습 스케줄 블록 (화살표 버튼으로 월 이동) ──
            item {
                LoorveCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = {
                                viewModel.setDisplayYearMonth(displayYearMonth.minusMonths(1))
                            }) {
                                Icon(Icons.Outlined.ChevronLeft, contentDescription = "이전 달", tint = Primary)
                            }
                            Text(
                                text = "${displayYearMonth.year}년 ${displayYearMonth.monthValue}월 복습 스케줄",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            IconButton(onClick = {
                                viewModel.setDisplayYearMonth(displayYearMonth.plusMonths(1))
                            }) {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = "다음 달", tint = Primary)
                            }
                        }
                        Text(
                            text = "점이 있는 날짜를 선택해 상세 일정을 확인하세요",
                            style = LoorveTypography.labelSmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        HomeMiniCalendar(
                            displayYearMonth = displayYearMonth,
                            selectedDate = selectedDate,
                            scheduledDates = uiState.reviewScheduleDates,
                            completedDates = completedDates,
                            onDateSelected = { selectedDate = it }
                        )

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Divider, thickness = 0.5.dp)
                        Spacer(Modifier.height(16.dp))

                        // 선택 날짜의 복습 일정 인라인 표시
                        val todaySchedules = uiState.reviewSchedules
                            .filter { it.reviewDate == selectedDate }
                            .distinctBy { schedule ->
                                schedule.scheduleId.ifBlank {
                                    "${schedule.reviewDate}_${schedule.examId}_${schedule.reviewOrder}"
                                }
                            }

                        if (todaySchedules.isNotEmpty()) {
                            Text(
                                text = "${selectedDate.format(DateTimeFormatter.ofPattern("M월 d일"))} · 복습 일정",
                                style = LoorveTypography.labelMedium,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                todaySchedules.forEach { schedule ->
                                    val subjectName = schedule.subjectName.ifBlank {
                                        uiState.exams.find { it.id == schedule.examId }?.subjectName
                                            ?: uiState.reviewBlocks.find { it.blockId == schedule.examId }?.examName
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
                                text = "${selectedDate.format(DateTimeFormatter.ofPattern("M월 d일"))} · 복습 일정 없음",
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

/** 모티베이션 헤더 */
@Composable
private fun HomeMotivationHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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

/** 지연된 복습 일정 */
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

    LoorveCard(modifier = Modifier.fillMaxWidth()) {
        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 176.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (overdueSchedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 176.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "지연된 복습이 없습니다 👍",
                    style = LoorveTypography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "지연된 복습이 존재합니다! 🚨",
                    style = LoorveTypography.titleMedium,
                    color = Warning,
                    fontWeight = FontWeight.Bold
                )
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
                        dateLabel = schedule.reviewDate.format(DateTimeFormatter.ofPattern("M월 d일")),
                        checked = schedule.isCompleted,
                        onCheckedChange = { onCheckedChange(scheduleKey, it) }
                    )
                }
            }
        }
    }
}

/** 미니 달력 */
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(text = label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = LoorveTypography.labelSmall, color = OnSurfaceVariant)
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
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasSchedule) {
                                    Spacer(Modifier.height(1.dp))
                                    val isCompleted = completedDates.contains(date)
                                    val dotColor = if (isSelected) Color.White else Primary
                                    if (isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .border(BorderStroke(1.5.dp, dotColor), CircleShape)
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

/** 복습 일정 카드 */
@Composable
private fun HomeScheduleCard(
    subjectName: String,
    content: String,
    dateLabel: String = "오늘",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    LoorveCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = Primary)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                val headerTitle = if (subjectName.isNotBlank()) "$dateLabel · $subjectName" else "$dateLabel · 복습 일정"
                Text(text = headerTitle, style = LoorveTypography.labelMedium, color = Primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = content,
                    style = LoorveTypography.bodyMedium,
                    color = OnBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                )
            }
        }
    }
}