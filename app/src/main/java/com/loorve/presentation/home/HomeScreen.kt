package com.loorve.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val isProSubscribed by subscriptionViewModel.isProSubscribed.collectAsState()
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Background,
                        Background,
                        Background,
                        CanvasWarm
                    )
                )
            )
    ) {
        AmbientAura()

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
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Home",
                                style = LoorveTypography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "홈",
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
            bottomBar = {
                val showBanner = !isProSubscribed &&
                    subscriptionState.entitlement !is SubscriptionEntitlement.Pro
                if (showBanner) {
                    key(showBanner) {
                        BannerAdView(modifier = Modifier.fillMaxWidth())
                    }
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
                    top = 8.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HomeMotivationHeader()
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
                        modifier = Modifier.fillMaxWidth()
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
private fun AmbientAura() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = 180.dp, y = (-48).dp)
            .size(220.dp)
            .blur(80.dp)
            .background(
                color = com.loorve.ui.theme.SkyTint.copy(alpha = 0.52f),
                shape = CircleShape
            )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = (-120).dp, y = 360.dp)
            .size(240.dp)
            .blur(80.dp)
            .background(
                color = com.loorve.ui.theme.LavenderTint.copy(alpha = 0.48f),
                shape = CircleShape
            )
    )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = "오늘도 한 칸씩 오래 남기기",
            style = LoorveTypography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OnBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "작게 자주 복습하면, 마지막에 덜 불안해집니다.",
            style = LoorveTypography.bodyMedium,
            color = OnSurfaceVariant
        )
    }
}

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
        color = SurfaceSolid,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
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
                Text(
                    text = "기억이 흐려지기 전에 확인하세요",
                    style = LoorveTypography.titleSmall,
                    color = Warning,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${overdueSchedules.size}개의 복습 일정이 지연되었습니다.",
                    style = LoorveTypography.bodySmall,
                    color = OnSurfaceVariant
                )
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

    Column(modifier = Modifier.fillMaxWidth()) {
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
                                    val dotColor = if (isSelected) OnGradient else Active
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
                        if (checked) Active else Color.Transparent
                    )
                    .border(
                        BorderStroke(
                            width = 1.5.dp,
                            color = if (checked) Active else TertiaryText
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
                    color = Active,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = content,
                    style = LoorveTypography.bodyMedium,
                    color = OnBackground,
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
