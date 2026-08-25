package com.loorve.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

            // ── 1) 모티베이션 헤더 ──
            item {
                HomeMotivationHeader()
            }

            // ── 2) 복습률 카드 ──
            item {
                HomeReviewRateCard(
                    rate = uiState.weeklyCompletionRate,
                    completed = uiState.weeklyCompleted,
                    total = uiState.weeklyTotal
                )
            }

            // ── 3) 미니 달력 ──
            item {
                SectionLabel("${LocalDate.now().monthValue}월 복습 스케줄")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "점이 있는 날짜를 선택해 상세 일정을 확인하세요",
                    style = LoorveTypography.labelSmall,
                    color = OnSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                HomeMiniCalendar(
                    selectedDate = selectedDate,
                    scheduledDates = uiState.scheduledDates,
                    onDateSelected = { selectedDate = it }
                )
            }

            // ── 4) 선택 날짜의 복습 일정 카드 ──
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

            // ── 5) 오늘의 학습 입력 섹션 (기존 기능 유지) ──
            item {
                SectionLabel("오늘의 학습")
                ProgressInputSection(
                    exams = uiState.exams,
                    onSave = { examId, content, completedCount, totalCount ->
                        viewModel.addProgress(examId, content, completedCount, totalCount)
                    }
                )
            }

            // ── 6) 내 시험 목록 (기존 기능 유지) ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("내 시험")
                    TextButton(onClick = onNavigateToExamSetting) {
                        Text("+ 추가", color = Primary, style = LoorveTypography.bodyMedium)
                    }
                }
            }

            if (uiState.exams.isEmpty()) {
                item {
                    EmptyStateView(
                        message = "등록된 시험이 없습니다",
                        subMessage = "시험을 추가해 D-Day를 관리해보세요",
                        actionLabel = "+ 시험 추가",
                        onActionClick = onNavigateToExamSetting
                    )
                }
            } else {
                items(uiState.exams, key = { it.id }) { exam ->
                    LoorveCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = exam.subjectName,
                                    style = LoorveTypography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = OnBackground
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = run {
                                        try {
                                            val date = java.time.Instant.ofEpochMilli(exam.examDate)
                                                .atZone(java.time.ZoneId.of("Asia/Seoul"))
                                                .toLocalDate()
                                            date.format(DateTimeFormatter.ofPattern("M월 d일"))
                                        } catch (e: Exception) { "" }
                                    },
                                    style = LoorveTypography.labelMedium,
                                    color = OnSurfaceVariant
                                )
                            }
                            val daysLeft = run {
                                try {
                                    val examDate = java.time.Instant.ofEpochMilli(exam.examDate)
                                        .atZone(java.time.ZoneId.of("Asia/Seoul"))
                                        .toLocalDate()
                                    java.time.temporal.ChronoUnit.DAYS
                                        .between(LocalDate.now(), examDate).toInt()
                                } catch (e: Exception) { -1 }
                            }
                            if (daysLeft >= 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = if (daysLeft == 0) "D-Day" else "D-$daysLeft",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = LoorveTypography.labelMedium,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 7) 학습 진도 기록 (기존 기능 유지) ──
            item { SectionLabel("학습 진도 기록") }

            if (uiState.progressList.isEmpty()) {
                item {
                    EmptyStateView(
                        message = "아직 학습 기록이 없습니다",
                        subMessage = "위 섹션에서 오늘의 학습을 기록해보세요"
                    )
                }
            } else {
                items(uiState.progressList, key = { it.id }) { progress ->
                    val isCompleted = progress.completed > 0 && progress.completed >= progress.total
                    LoorveCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToProgressDetail(progress.id) }
                    ) {
                        Column {
                            Text(
                                text = progress.content,
                                style = LoorveTypography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = OnBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            LoorveProgressBar(
                                completed = progress.completed,
                                total = progress.total,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = progress.dateFormatted,
                                    style = LoorveTypography.labelMedium,
                                    color = OnSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCompleted)
                                        Success.copy(alpha = 0.15f)
                                    else
                                        Tertiary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isCompleted) "완료" else "진행 중",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = LoorveTypography.labelMedium,
                                        color = if (isCompleted) Success else Tertiary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 8) 하단 배너 광고 플레이스홀더 ──
            item {
                HomeBannerAd()
            }
        }
    }
}

/** 모티베이션 헤더 */
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

/** 이번 주 복습률 카드 */
@Composable
private fun HomeReviewRateCard(
    rate: Float,
    completed: Int,
    total: Int
) {
    val safeRate = if (rate.isNaN() || rate < 0f) 0f else rate.coerceAtMost(1f)
    val percent = (safeRate * 100).toInt()

    LoorveCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "오늘의 문장",
                    style = LoorveTypography.labelMedium,
                    color = OnSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "이번 주 ${total}개 중 ${completed}개 완료",
                    style = LoorveTypography.bodySmall,
                    color = OnSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LoorveProgressBar(
                    completed = completed,
                    total = if (total == 0) 1 else total,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
            // ✅ 수정: Box → Column으로 교체하여 레이블 겹침 버그 해결
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "전체 복습률",
                    style = LoorveTypography.labelSmall,
                    color = OnSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$percent%",
                    style = LoorveTypography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    fontSize = 32.sp
                )
            }
        }
    }
}

/** 미니 달력 */
@Composable
private fun HomeMiniCalendar(
    selectedDate: LocalDate,
    scheduledDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val yearMonth = YearMonth.of(selectedDate.year, selectedDate.month)
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

    LoorveCard(modifier = Modifier.fillMaxWidth()) {
        Column {
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
                            val date = yearMonth.atDay(currentDay)
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
                                                    if (isSelected) Color.White
                                                    else Primary
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
}

/** 오늘 복습 일정 카드 */
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
                // ✅ 수정: 예상 시간 텍스트 추가
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

/** 배너 광고 플레이스홀더 */
@Composable
private fun HomeBannerAd() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface.copy(alpha = 0.5f))
            .border(1.dp, OnSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "배너 광고 320 × 50",
            style = LoorveTypography.labelSmall,
            color = OnSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}