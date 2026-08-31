package com.loorve.presentation.home

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.ui.component.*
import com.loorve.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMyPage: () -> Unit,
    onNavigateToExamSetting: () -> Unit,
    onNavigateToProgressDetail: (String) -> Unit,
    onNavigateToReviewBlockDetail: (String) -> Unit,  // ✅ 추가
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var displayYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var showCreateBlockSheet by remember { mutableStateOf(false) }  // ✅ BottomSheet 표시 상태

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
        // ✅ FAB 추가
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateBlockSheet = true },
                containerColor = Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "새 복습 블록 만들기")
            }
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

            // ── 2) 전체 복습률 카드 ──
            item {
                HomeReviewRateCard(
                    rate = uiState.weeklyCompletionRate,
                    completed = uiState.weeklyCompleted,
                    total = uiState.weeklyTotal
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
                            IconButton(onClick = { displayYearMonth = displayYearMonth.minusMonths(1) }) {
                                Icon(Icons.Outlined.ChevronLeft, contentDescription = "이전 달", tint = Primary)
                            }
                            Text(
                                text = "${displayYearMonth.year}년 ${displayYearMonth.monthValue}월 복습 스케줄",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            IconButton(onClick = { displayYearMonth = displayYearMonth.plusMonths(1) }) {
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
                            scheduledDates = uiState.scheduledDates,
                            onDateSelected = { selectedDate = it }
                        )
                    }
                }
            }

            // ── 3.5) 복습 블록 목록 ✅ 추가 ──
            if (uiState.reviewBlocks.isNotEmpty()) {
                item {
                    Text(
                        text = "복습 블록",
                        style = LoorveTypography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                items(uiState.reviewBlocks, key = { "block_${it.blockId}" }) { block ->
                    ReviewBlockCard(
                        block = block,
                        onClick = { onNavigateToReviewBlockDetail(block.blockId) }
                    )
                }
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
                        subjectName = uiState.exams.find { it.id == progress.examId }?.subjectName ?: "",
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
        }
    }

    // ✅ 복습 블록 생성 BottomSheet
    if (showCreateBlockSheet) {
        CreateReviewBlockBottomSheet(
            isCreating = uiState.isCreatingBlock,
            onDismiss = { showCreateBlockSheet = false },
            onConfirm = { examName, examDateMillis, prepStartMillis, dailyCap ->
                viewModel.createReviewBlock(examName, examDateMillis, prepStartMillis, dailyCap)
                showCreateBlockSheet = false
            }
        )
    }
}

// ✅ 복습 블록 생성 BottomSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReviewBlockBottomSheet(
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (examName: String, examDateMillis: Long, prepStartMillis: Long, dailyCap: Int) -> Unit
) {
    val context = LocalContext.current
    val seoulZone = ZoneId.of("Asia/Seoul")
    val today = LocalDate.now()

    var examName by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf<LocalDate?>(null) }
    var prepStartDate by remember { mutableStateOf(today) }
    var dailyCap by remember { mutableStateOf(5) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "＋ 새 복습 블록 만들기",
                style = LoorveTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )

            // 시험명 입력
            OutlinedTextField(
                value = examName,
                onValueChange = { examName = it },
                label = { Text("시험명") },
                placeholder = { Text("예: 정보처리기사, 수능 수학") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating
            )

            // 시험 목표일 DatePicker
            val examDateText = examDate?.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")) ?: "날짜 선택"
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> examDate = LocalDate.of(y, m + 1, d) },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreating
            ) {
                Text(
                    text = "📅 시험 목표일: $examDateText",
                    color = if (examDate == null) OnSurfaceVariant else Primary
                )
            }

            // 준비 시작일 DatePicker
            val prepText = prepStartDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> prepStartDate = LocalDate.of(y, m + 1, d) },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreating
            ) {
                Text(text = "🗓 준비 시작일: $prepText", color = Primary)
            }

            // 하루 최대 복습 수 Slider
            Column {
                Text(
                    text = "하루 최대 복습 수: ${dailyCap}회",
                    style = LoorveTypography.labelMedium,
                    color = OnBackground
                )
                Slider(
                    value = dailyCap.toFloat(),
                    onValueChange = { dailyCap = it.toInt() },
                    valueRange = 1f..20f,
                    steps = 18,
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary),
                    enabled = !isCreating
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1회", style = LoorveTypography.labelSmall, color = OnSurfaceVariant)
                    Text("20회", style = LoorveTypography.labelSmall, color = OnSurfaceVariant)
                }
            }

            // 저장 버튼
            Button(
                onClick = {
                    val examMillis = examDate?.atStartOfDay(seoulZone)?.toInstant()?.toEpochMilli() ?: return@Button
                    val prepMillis = prepStartDate.atStartOfDay(seoulZone).toInstant().toEpochMilli()
                    onConfirm(examName.trim(), examMillis, prepMillis, dailyCap)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = examName.isNotBlank() && examDate != null && !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("저장", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ✅ 복습 블록 카드
@Composable
private fun ReviewBlockCard(
    block: ReviewBlockUiModel,
    onClick: () -> Unit
) {
    val dDayText = when {
        block.dDay > 0 -> "D-${block.dDay}"
        block.dDay == 0 -> "D-Day"
        else -> "D+${-block.dDay}"
    }
    val completionPercent = (block.completionRate * 100).toInt()

    LoorveCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.examName,
                    style = LoorveTypography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "복습 완료율 $completionPercent%",
                        style = LoorveTypography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            // D-Day 뱃지
            Surface(
                color = if (block.dDay in 0..7) Color(0xFFFFEBEE) else Primary.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = dDayText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = LoorveTypography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (block.dDay in 0..7) Color(0xFFC62828) else Primary
                )
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

/** 전체 복습률 카드 */
@Composable
private fun HomeReviewRateCard(rate: Float, completed: Int, total: Int) {
    val safeRate = if (rate.isNaN() || rate < 0f) 0f else rate.coerceAtMost(1f)
    val percent = (safeRate * 100).toInt()
    val animatedRate by animateFloatAsState(targetValue = safeRate, animationSpec = tween(800), label = "reviewRateAnim")

    LoorveCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "전체 복습률", style = LoorveTypography.labelMedium, color = OnSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    drawArc(color = Primary.copy(alpha = 0.15f), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    if (animatedRate > 0f) {
                        val gradientBrush = Brush.sweepGradient(colors = listOf(GradientStart, GradientEnd), center = Offset(size.width / 2f, size.height / 2f))
                        drawArc(brush = gradientBrush, startAngle = -90f, sweepAngle = 360f * animatedRate, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    }
                }
                Text(text = "$percent%", fontWeight = FontWeight.Bold, color = Primary, fontSize = 28.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(text = "전체 복습률", style = LoorveTypography.labelSmall, color = OnSurfaceVariant)
        }
    }
}

/** 미니 달력 */
@Composable
private fun HomeMiniCalendar(displayYearMonth: YearMonth, selectedDate: LocalDate, scheduledDates: Set<LocalDate>, onDateSelected: (LocalDate) -> Unit) {
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
                            modifier = Modifier.weight(1f).height(36.dp).clip(CircleShape)
                                .background(when { isSelected -> Primary; isToday -> Primary.copy(alpha = 0.15f); else -> Color.Transparent })
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$currentDay", style = LoorveTypography.labelMedium, color = when { isSelected -> Color.White; isToday -> Primary; else -> OnBackground }, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)
                                if (hasSchedule) {
                                    Spacer(Modifier.height(1.dp))
                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isSelected) Color.White else Primary))
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
private fun HomeScheduleCard(subjectName: String, content: String, onStart: () -> Unit) {
    LoorveCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "오늘 · $subjectName", style = LoorveTypography.labelMedium, color = Primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(text = content, style = LoorveTypography.bodyMedium, color = OnBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "예상 20분", style = LoorveTypography.labelSmall, color = Primary.copy(alpha = 0.8f))
                    Text(text = "·", style = LoorveTypography.labelSmall, color = OnSurfaceVariant)
                    Text(text = "지금 바로 시작할 수 있어요", style = LoorveTypography.labelSmall, color = OnSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            FilledIconButton(onClick = onStart, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary)) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "시작", tint = Color.White)
            }
        }
    }
}