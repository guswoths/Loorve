// 경로: app/src/main/java/com/loorve/presentation/reviewblock/ReviewBlockDetailScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.loorve.presentation.reviewblock

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.loorve.ui.theme.DeepBlueLightBlueGradient
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.subscription.SubscriptionEntitlement
import com.loorve.presentation.home.HomeViewModel
import com.loorve.presentation.subscription.ProPaywallDialog
import com.loorve.presentation.subscription.SubscriptionViewModel
import com.loorve.ui.component.BannerAdView
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────────────────
// Google Stitch MCP: "학습 진도 기입 및 복습 블록 (블루 테마)" 메인 화면
// ──────────────────────────────────────────────────────────
@Composable
fun ReviewBlockDetailScreen(
    blockId: String,
    block: ReviewBlock?,
    onNavigateBack: () -> Unit,
    viewModel: ReviewBlockDetailViewModel = hiltViewModel(),
    @Suppress("UNUSED_PARAMETER") homeViewModel: HomeViewModel = hiltViewModel(),
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val subscriptionState by subscriptionViewModel.state.collectAsState()
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val snackbarHostState = remember { SnackbarHostState() }
    var showProDialog by remember { mutableStateOf(false) }
    var selectedStudyRecord by remember { mutableStateOf<StudyRecord?>(null) }
    var selectedReviewSchedule by remember { mutableStateOf<ReviewScheduleItem?>(null) }

    LaunchedEffect(blockId) {
        viewModel.loadBlockData(uid, blockId, externalBlock = block)
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }
    LaunchedEffect(uiState.requiresPro) {
        if (uiState.requiresPro) showProDialog = true
    }
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.resetDeleteSuccess()
            onNavigateBack()
        }
    }

    val resolvedBlock = uiState.reviewBlock
    val title = resolvedBlock?.examName?.ifBlank { resolvedBlock.title }
        ?: resolvedBlock?.title
        ?: blockId
    val totalReviews = uiState.reviewScheduleRecords.size
    val completedReviews = uiState.reviewScheduleRecords.count {
        it.status == ReviewStatus.COMPLETED
    }
    val completionRate = if (totalReviews == 0) 0f
    else (completedReviews.toFloat() / totalReviews).coerceIn(0f, 1f)

    val dDay = resolvedBlock?.examDate?.let { millis ->
        if (millis == 0L) "D-?" else {
            val examDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.of("Asia/Seoul")).toLocalDate()
            val days = ChronoUnit.DAYS.between(LocalDate.now(ZoneId.of("Asia/Seoul")), examDate)
            when {
                days > 0 -> "D-$days"
                days == 0L -> "D-Day"
                else -> "D+${-days}"
            }
        }
    } ?: ""

    if (showProDialog) {
        ProPaywallDialog(
            viewModel = subscriptionViewModel,
            onDismiss = {
                showProDialog = false
                onNavigateBack()
            }
        )
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowDeleteConfirm(false) },
            title = { Text("블록 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("이 복습 블록과 모든 학습 기록, 복습 일정이 삭제됩니다. 계속할까요?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteBlock(uid, blockId) },
                    enabled = !uiState.isLoading
                ) {
                    Text("삭제", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowDeleteConfirm(false) }) {
                    Text("취소")
                }
            }
        )
    }

    uiState.recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { viewModel.setRecordToDelete(null) },
            title = { Text("학습기록 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("\"${record.title.ifBlank { "이 학습기록" }}\"을 삭제할까요?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteStudyRecord(uid, blockId, record) },
                    enabled = !uiState.isLoading
                ) {
                    Text("삭제", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setRecordToDelete(null) }) {
                    Text("취소")
                }
            }
        )
    }

    selectedStudyRecord?.let {
        StudyRecordDetailDialog(it) { selectedStudyRecord = null }
    }
    selectedReviewSchedule?.let {
        ReviewScheduleDetailDialog(it) { selectedReviewSchedule = null }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = Color(0xFF475569)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setShowDeleteConfirm(true) },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        },
        bottomBar = {
            val showBanner = subscriptionState.entitlement is SubscriptionEntitlement.Free
            if (showBanner) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    key(showBanner) {
                        BannerAdView(modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ──────────────────────────────────────────────────────────
            // 1. ReviewProgressCard: 복습 블록 진행률 및 통계 카드
            // ──────────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.8f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title & D-Day Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (dDay.isNotBlank()) {
                                StitchDDayBadge(dDay = dDay)
                            }
                        }

                        // Metric Display (33% 복습 완료율)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${(completionRate * 100).toInt()}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "%",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                )
                            }
                            Text(
                                text = "복습 완료율",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Progress Bar Track & Fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .padding(1.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(completionRate)
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF2563EB),
                                                Color(0xFF3B82F6),
                                                Color(0xFF38BDF8)
                                            )
                                        )
                                    )
                            )
                        }

                        // Status Summary Note
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                            )
                            Text(
                                text = "$completedReviews / $totalReviews 복습 일정 완료",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }
            }

            // ──────────────────────────────────────────────────────────
            // 2. DailyStudyProgressForm: 오늘 학습 진도 기입 폼
            // ──────────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.8f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    StudyProgressInputSection(
                        onSave = { learningDateMillis, recordTitle, content ->
                            viewModel.saveProgress(
                                uid = uid,
                                blockId = blockId,
                                examId = resolvedBlock?.blockId ?: blockId,
                                title = recordTitle,
                                content = content,
                                learningDateMillis = learningDateMillis,
                                dailyCap = resolvedBlock?.dailyCap ?: 5
                            )
                        },
                        isLoading = uiState.isLoading,
                        isSaveEnabled = resolvedBlock != null && resolvedBlock.examDate != 0L
                    )
                }
            }

            // ──────────────────────────────────────────────────────────
            // 3. SegmentedTabNavigation: 학습 기록 / 복습 일정 탭 전환
            // ──────────────────────────────────────────────────────────
            item {
                StitchSegmentedTabNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        viewModel.selectTab(tab, uid = uid, blockId = blockId)
                    }
                )
            }

            // ──────────────────────────────────────────────────────────
            // 4. Records List Section (선택된 탭에 따라 분기)
            // ──────────────────────────────────────────────────────────
            if (selectedTab == ReviewBlockTab.STUDY_RECORD) {
                item {
                    StudyRecordListSection(
                        records = uiState.studyRecords,
                        reviewSchedules = uiState.reviewScheduleRecords,
                        isLoading = uiState.isLoading,
                        onDeleteRecord = viewModel::setRecordToDelete,
                        onRecordClick = { selectedStudyRecord = it }
                    )
                }
            } else {
                item {
                    ReviewRecordListSection(
                        scheduleItems = uiState.reviewScheduleRecords,
                        defaultAlarmTime = uiState.defaultAlarmTime,
                        onTimeSave = { item, hour, minute ->
                            viewModel.saveCustomAlarmTime(uid, item, hour, minute)
                        },
                        onCheckedChange = { item, checked ->
                            viewModel.toggleReviewCompletion(uid, item, checked)
                        },
                        onScheduleClick = { selectedReviewSchedule = it }
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Stitch 앰비언트 펄스 D-Day 뱃지
// ──────────────────────────────────────────────────────────
@Composable
private fun StitchDDayBadge(dDay: String) {
    val pulseTransition = rememberInfiniteTransition(label = "stitchDDayPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dDayPulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dDayPulseAlpha"
    )

    Surface(
        color = Color(0xFFEFF6FF),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE).copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB))
            )
            Text(
                text = dDay,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────
// DailyStudyProgressForm 입력 컴포넌트
// ──────────────────────────────────────────────────────────
@Composable
fun StudyProgressInputSection(
    onSave: (learningDateMillis: Long, title: String, content: String) -> Unit,
    isLoading: Boolean,
    isSaveEnabled: Boolean = true
) {
    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }

    val kstZone = remember { ZoneId.of("Asia/Seoul") }
    val todayLocalDate = remember { LocalDate.now(kstZone) }
    val todayMillis = remember {
        todayLocalDate.atStartOfDay(kstZone).toInstant().toEpochMilli()
    }
    var selectedDateMillis by remember { mutableLongStateOf(todayMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedLocalDate = remember(selectedDateMillis) {
        Instant.ofEpochMilli(selectedDateMillis).atZone(kstZone).toLocalDate()
    }
    val isToday = remember(selectedLocalDate, todayLocalDate) {
        selectedLocalDate == todayLocalDate
    }

    val displayDateText = remember(selectedDateMillis) {
        SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREA).format(Date(selectedDateMillis))
    }

    val canSave = (titleText.isNotBlank() || contentText.isNotBlank()) && !isLoading && isSaveEnabled

    if (showDatePicker) {
        val initialUtcMillis = remember(selectedLocalDate) {
            selectedLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMs ->
                        val pickedLocalDate = Instant.ofEpochMilli(utcMs)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        selectedDateMillis = pickedLocalDate
                            .atStartOfDay(kstZone)
                            .toInstant()
                            .toEpochMilli()
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isToday) "오늘 학습 진도" else "학습 진도 기입",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            if (!isToday) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text(
                        text = "기록일: ${selectedLocalDate.monthValue}월 ${selectedLocalDate.dayOfMonth}일",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // 1. Date Selector Display Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isToday) Color(0xFFF8FAFC) else Color(0xFFF0F9FF))
                .border(
                    1.dp,
                    if (isToday) Color(0xFFE2E8F0) else Color(0xFF93C5FD),
                    RoundedCornerShape(14.dp)
                )
                .clickable(enabled = !isLoading) { showDatePicker = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "학습일자 선택",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayDateText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isToday) Color(0xFF334155) else Color(0xFF1D4ED8)
            )
            if (!isToday) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(선택됨)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )
            }
        }

        // 2. Study Subject Title Input
        BasicTextField(
            value = titleText,
            onValueChange = { titleText = it },
            enabled = !isLoading,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B)
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (titleText.isEmpty()) {
                            Text(
                                text = "학습 제목 (제목 또는 내용 중 하나 필수)",
                                fontSize = 13.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        innerTextField()
                    }
                    if (titleText.isNotEmpty() && !isLoading) {
                        IconButton(
                            onClick = { titleText = "" },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "제목 지우기",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        )

        // 3. Study Content Textarea
        BasicTextField(
            value = contentText,
            onValueChange = { contentText = it },
            enabled = !isLoading,
            minLines = 3,
            maxLines = 5,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFF1E293B),
                lineHeight = 20.sp
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (contentText.isEmpty()) {
                        Text(
                            text = "학습 내용",
                            fontSize = 13.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    innerTextField()
                }
            }
        )

        // 4. Submit / Action Button (Gradient)
        Button(
            onClick = {
                if (canSave) {
                    onSave(
                        selectedDateMillis,
                        titleText.trim(),
                        contentText.trim()
                    )
                    titleText = ""
                    contentText = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = canSave,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color(0xFFE2E8F0)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (canSave) {
                            DeepBlueLightBlueGradient
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0))
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "학습 기록 및 복습 일정 생성",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// SegmentedTabNavigation (학습 기록 / 복습 일정 탭)
// ──────────────────────────────────────────────────────────
@Composable
fun StitchSegmentedTabNavigation(
    selectedTab: ReviewBlockTab,
    onTabSelected: (ReviewBlockTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE2E8F0).copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isStudyRecord = selectedTab == ReviewBlockTab.STUDY_RECORD
            // 학습 기록 탭
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(ReviewBlockTab.STUDY_RECORD) }
                    ),
                shape = RoundedCornerShape(12.dp),
                color = if (isStudyRecord) Color.White else Color.Transparent,
                shadowElevation = if (isStudyRecord) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "학습 기록",
                        fontSize = 13.5.sp,
                        fontWeight = if (isStudyRecord) FontWeight.Bold else FontWeight.Medium,
                        color = if (isStudyRecord) Color(0xFF2563EB) else Color(0xFF64748B)
                    )
                    if (isStudyRecord) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        )
                    }
                }
            }

            // 복습 일정 탭
            val isReviewRecord = selectedTab == ReviewBlockTab.REVIEW_RECORD
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(ReviewBlockTab.REVIEW_RECORD) }
                    ),
                shape = RoundedCornerShape(12.dp),
                color = if (isReviewRecord) Color.White else Color.Transparent,
                shadowElevation = if (isReviewRecord) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "복습 일정",
                        fontSize = 13.5.sp,
                        fontWeight = if (isReviewRecord) FontWeight.Bold else FontWeight.Medium,
                        color = if (isReviewRecord) Color(0xFF2563EB) else Color(0xFF64748B)
                    )
                    if (isReviewRecord) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// ReviewRecordListSection (복습 일정 카드 리스트)
// ──────────────────────────────────────────────────────────
@Composable
fun ReviewRecordListSection(
    scheduleItems: List<ReviewScheduleItem>,
    defaultAlarmTime: Pair<Int, Int> = 9 to 0,
    onTimeSave: ((ReviewScheduleItem, Int, Int) -> Unit)? = null,
    onCheckedChange: ((ReviewScheduleItem, Boolean) -> Unit)? = null,
    onScheduleClick: (ReviewScheduleItem) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (scheduleItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "아직 복습 일정이 없어요.\n상단에서 학습 진도를 기입해보세요!",
                    fontSize = 13.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            val timeMap = remember { mutableStateMapOf<String, String>() }
            scheduleItems.forEach { item ->
                val itemKey = item.id.ifBlank {
                    "${item.reviewDate}_${item.blockId}_${item.reviewOrder}"
                }
                ReviewRecordMiniCard(
                    item = item,
                    savedTime = timeMap[itemKey] ?: item.customAlarmTime
                        ?.let { (hour, minute) -> "%02d:%02d".format(hour, minute) }
                        ?: "%02d:%02d".format(defaultAlarmTime.first, defaultAlarmTime.second),
                    onTimeSave = { hour, minute ->
                        timeMap[itemKey] = "%02d:%02d".format(hour, minute)
                        onTimeSave?.invoke(item, hour, minute)
                    },
                    onCheckedChange = { checked ->
                        onCheckedChange?.invoke(item, checked)
                    },
                    onClick = { onScheduleClick(item) }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Stitch 스타일 복습 일정 개별 카드
// ──────────────────────────────────────────────────────────
@Composable
fun ReviewRecordMiniCard(
    item: ReviewScheduleItem,
    savedTime: String? = null,
    onTimeSave: ((Int, Int) -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val dateText = remember(item.reviewDate) {
        if (item.reviewDate > 0L)
            SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(item.reviewDate))
        else "-"
    }
    var showTimePicker by remember { mutableStateOf(false) }
    val isCompleted = item.status == ReviewStatus.COMPLETED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "복습일정: ${item.title}, 날짜: $dateText, ${item.status}"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                // Header Row: Date & Title & Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$dateText (${item.reviewOrder}회차)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCompleted) Color(0xFF2563EB) else Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.title.ifBlank { "복습 일정" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Round Toggle Checkbox Button
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) Color(0xFF2563EB) else Color.Transparent)
                            .border(
                                BorderStroke(
                                    width = 1.5.dp,
                                    color = if (isCompleted) Color(0xFF2563EB) else Color(0xFFCBD5E1)
                                ),
                                CircleShape
                            )
                            .clickable {
                                onCheckedChange?.invoke(!isCompleted)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "완료됨",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "탭하여 상세정보 확인",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )

                // Footer Metadata Row: Status Badge & Alarm notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge
                    val badgeBg: Color
                    val badgeBorder: Color
                    val badgeText: Color
                    val label: String
                    when (item.status) {
                        ReviewStatus.COMPLETED -> {
                            badgeBg = Color(0xFFECFDF5)
                            badgeBorder = Color(0xFFA7F3D0)
                            badgeText = Color(0xFF059669)
                            label = "완료"
                        }
                        ReviewStatus.OVERDUE -> {
                            badgeBg = Color(0xFFFEF2F2)
                            badgeBorder = Color(0xFFFECACA)
                            badgeText = Color(0xFFDC2626)
                            label = "지연"
                        }
                        ReviewStatus.FINAL_URGENT_REVIEW -> {
                            badgeBg = Color(0xFFFFFBEB)
                            badgeBorder = Color(0xFFFDE68A)
                            badgeText = Color(0xFFD97706)
                            label = "최종 복습"
                        }
                        else -> {
                            badgeBg = Color(0xFFEFF6FF)
                            badgeBorder = Color(0xFFBFDBFE)
                            badgeText = Color(0xFF2563EB)
                            label = "진행 예정"
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg,
                        border = BorderStroke(1.dp, badgeBorder)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                        )
                    }

                    // Alarm Button
                    Surface(
                        modifier = Modifier.clickable { showTimePicker = true },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🔔", fontSize = 11.sp)
                            Text(
                                text = "Alarm ${savedTime.orEmpty().ifBlank { "09:00" }}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        if (showTimePicker) {
        val (initialHour, initialMinute) = savedTime.orEmpty()
            .split(":")
            .takeIf { it.size == 2 }
            ?.let { parts ->
                val hour = parts[0].toIntOrNull()
                val minute = parts[1].toIntOrNull()
                if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                    hour to minute
                } else {
                    9 to 0
                }
            } ?: (9 to 0)

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "알림 시간 설정",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimeInput(state = timePickerState)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("취소")
                        }
                        TextButton(
                            onClick = {
                                onTimeSave?.invoke(timePickerState.hour, timePickerState.minute)
                                showTimePicker = false
                            }
                        ) {
                            Text("확인", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// StudyRecordListSection (학습 기록 카드 리스트)
// ──────────────────────────────────────────────────────────
@Composable
fun StudyRecordListSection(
    records: List<StudyRecord>,
    reviewSchedules: List<ReviewScheduleItem> = emptyList(),
    isLoading: Boolean = false,
    onDeleteRecord: (StudyRecord) -> Unit = {},
    onRecordClick: (StudyRecord) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "아직 기록된 학습이 없어요.\n첫 학습 진도를 기입해보세요!",
                    fontSize = 13.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            val sortedRecords = remember(records) {
                records.sortedWith(compareBy<StudyRecord> { it.learningDate }.thenBy { it.createdAt })
            }
            sortedRecords.forEach { record ->
                val associated = reviewSchedules.filter { it.studyRecordId == record.id }
                val isResolved = associated.isNotEmpty() &&
                    associated.all { it.status == ReviewStatus.COMPLETED }
                val completedCount = associated.count { it.status == ReviewStatus.COMPLETED }

                StudyRecordMiniCard(
                    record = record,
                    isResolved = isResolved,
                    completedCount = completedCount,
                    totalCount = associated.size,
                    isLoading = isLoading,
                    onDeleteClick = { onDeleteRecord(record) },
                    onClick = { onRecordClick(record) }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────
// Stitch 스타일 학습 기록 개별 카드
// ──────────────────────────────────────────────────────────
@Composable
fun StudyRecordMiniCard(
    record: StudyRecord,
    isResolved: Boolean = false,
    completedCount: Int = 0,
    totalCount: Int = 0,
    isLoading: Boolean = false,
    onDeleteClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val dateText = remember(record.learningDate) {
        if (record.learningDate > 0L)
            SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(record.learningDate))
        else "-"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "학습기록: ${record.title}, 날짜: $dateText"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                // Header: Date & Title & Delete Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dateText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = record.title.ifBlank { "학습 기록" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        enabled = !isLoading,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "학습기록 삭제",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Content snippet
                if (record.content.isNotBlank()) {
                    Text(
                        text = record.content,
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                Text(
                    text = "탭하여 상세정보 확인",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isResolved) Color(0xFFECFDF5) else Color(0xFFEFF6FF),
                        border = BorderStroke(
                            1.dp,
                            if (isResolved) Color(0xFFA7F3D0) else Color(0xFFBFDBFE)
                        )
                    ) {
                        Text(
                            text = if (isResolved) "완료" else "복습 진행 ($completedCount/$totalCount)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isResolved) Color(0xFF059669) else Color(0xFF2563EB),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                        )
                    }

                    Text(
                        text = "예정 복습: ${record.plannedReviewCount}회",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }

// ──────────────────────────────────────────────────────────
// 상세 모달 다이얼로그 (학습 기록 상세 & 복습 일정 상세)
// ──────────────────────────────────────────────────────────
@Composable
private fun StudyRecordDetailDialog(
    record: StudyRecord,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "학습 기록 상세",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                DetailTextBlock("제목", record.title.ifBlank { "제목 없음" })
                DetailTextBlock("학습 내용 및 메모", record.content.ifBlank { "내용 없음" })
                DetailTextBlock(
                    "학습일",
                    if (record.learningDate > 0L) {
                        SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
                            .format(Date(record.learningDate))
                    } else {
                        "-"
                    }
                )
                DetailTextBlock("예정 복습 횟수", "${record.plannedReviewCount}회")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("닫기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewScheduleDetailDialog(
    item: ReviewScheduleItem,
    onDismiss: () -> Unit
) {
    val dateText = if (item.reviewDate > 0L) {
        SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date(item.reviewDate))
    } else {
        "-"
    }
    val guide = item.recommendedMethod.ifBlank {
        when {
            item.isFinalReview -> "시험 범위의 핵심 구조와 오답 포인트를 인출 점검하세요. 새 내용 학습은 피하세요."
            item.reviewOrder <= 1 -> "노트 없이 핵심 개념을 먼저 회상한 뒤, 틀린 부분만 확인하세요."
            item.reviewOrder == 2 -> "플래시카드 또는 짧은 퀴즈로 인출 연습을 하세요."
            else -> "문제풀이·서술형 회상·오답 설명 중 하나를 수행하세요."
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "복습 일정 ${item.reviewOrder}회차",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                DetailTextBlock("학습 내용", item.title.ifBlank { "내용 없음" })
                DetailTextBlock("복습 예정일", dateText)
                DetailTextBlock("상태", item.status.detailLabel())
                DetailTextBlock("회차별 학습가이드", guide)
                if (item.rescheduleReason.orEmpty().isNotBlank()) {
                    DetailTextBlock("일정 변경 사유", item.rescheduleReason.orEmpty())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("닫기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTextBlock(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF0F172A),
            lineHeight = 20.sp
        )
    }
}

private fun ReviewStatus.detailLabel(): String = when (this) {
    ReviewStatus.PENDING -> "대기 중"
    ReviewStatus.COMPLETED -> "완료"
    ReviewStatus.OVERDUE -> "지연"
    ReviewStatus.FINAL_URGENT_REVIEW -> "최종 복습"
    ReviewStatus.CRAM_MODE_REQUIRED -> "압축 복습 필요"
    ReviewStatus.OVERLOADED_UNRESOLVED -> "일일 과부하 확인 필요"
}