package com.loorve.presentation.reviewblock

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.CompletionResult
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.review.ReviewPlanStatus
import com.loorve.domain.review.ScheduleGenerationOutcome
import com.loorve.domain.usecase.CreateStudyRecordResult
import com.loorve.presentation.home.HomeViewModel
import com.loorve.presentation.subscription.ProPaywallDialog
import com.loorve.presentation.subscription.SubscriptionViewModel
import com.loorve.ui.component.LoorveCard
import com.loorve.ui.theme.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

// ── 메인 화면 ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewBlockDetailScreen(
    blockId: String,
    block: ReviewBlock?,
    onNavigateBack: () -> Unit,
    viewModel: ReviewBlockDetailViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),          // ✅ [추가]
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val snackbarHostState = remember { SnackbarHostState() }
    var showProDialog by remember { mutableStateOf(false) }
    var selectedStudyRecord by remember { mutableStateOf<StudyRecord?>(null) }
    var selectedReviewSchedule by remember { mutableStateOf<ReviewScheduleItem?>(null) }

    LaunchedEffect(blockId) {
        viewModel.loadBlockData(uid, blockId, externalBlock = block)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.requiresPro) {
        if (uiState.requiresPro) showProDialog = true
    }

    // 블록 삭제 성공 시 뒤로가기
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.resetDeleteSuccess()
            onNavigateBack()
        }
    }

    val resolvedBlock = uiState.reviewBlock
    val examDateMillis = resolvedBlock?.examDate ?: 0L
    val dailyCap = resolvedBlock?.dailyCap ?: 5
    val examName = resolvedBlock?.examName?.ifBlank { resolvedBlock.title }
        ?: resolvedBlock?.title
        ?: blockId

    if (showProDialog) {
        ProPaywallDialog(
            viewModel = subscriptionViewModel,
            onDismiss = {
                showProDialog = false
                onNavigateBack()
            }
        )
    }

    val dDayText = when {
        resolvedBlock == null -> ""
        examDateMillis == 0L  -> "D-?"
        else -> {
            val examLocal = Instant.ofEpochMilli(examDateMillis)
                .atZone(ZoneId.of("Asia/Seoul")).toLocalDate()
            val days = java.time.temporal.ChronoUnit.DAYS
                .between(LocalDate.now(), examLocal).toInt()
            when {
                days > 0  -> "D-$days"
                days == 0 -> "D-Day"
                else      -> "D+${-days}"
            }
        }
    }
    val totalReviewCount = uiState.reviewScheduleRecords.size
    val completedReviewCount = uiState.reviewScheduleRecords.count {
        it.status == ReviewStatus.COMPLETED
    }
    val completionRate = if (totalReviewCount > 0) {
        (completedReviewCount.toFloat() / totalReviewCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val overdueRatio = if (totalReviewCount > 0) {
        (uiState.overdueItems.size.toFloat() / totalReviewCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val reviewProgress = (1f - overdueRatio).coerceIn(0f, 1f)

    // 블록 삭제 확인 AlertDialog
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowDeleteConfirm(false) },
            title = { Text("블록 삭제") },
            text = {
                Text("이 복습 블록과 모든 학습 기록, 복습 일정이 삭제됩니다. 계속할까요?")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteBlock(uid, blockId) },
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "삭제",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setShowDeleteConfirm(false) },
                    enabled = !uiState.isLoading
                ) {
                    Text("취소")
                }
            }
        )
    }

    // 개별 학습기록 삭제 확인 AlertDialog
    uiState.recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { viewModel.setRecordToDelete(null) },
            title = { Text("학습기록 삭제") },
            text = { Text("\"${record.title.ifBlank { "이 학습기록" }}\"을 삭제할까요? 이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteStudyRecord(uid, blockId, record) },
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        "삭제",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                selectedStudyRecord?.let { record ->
                    StudyRecordDetailDialog(
                        record = record,
                        onDismiss = { selectedStudyRecord = null }
                    )
                }

                selectedReviewSchedule?.let { item ->
                    ReviewScheduleDetailDialog(
                        item = item,
                        onDismiss = { selectedReviewSchedule = null }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setRecordToDelete(null) },
                    enabled = !uiState.isLoading
                ) {
                    Text("취소")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Background, CanvasWarm)
                )
            )
    ) {
        DetailAmbientAura()
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            examName,
                            style = LoorveTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Outlined.ArrowBack,
                                contentDescription = "뒤로",
                                tint = OnBackground
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
                                contentDescription = "블록 삭제",
                                tint = Error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
            // ── 블록 요약 정보 ──
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Surface,
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = examName,
                                    style = LoorveTypography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = OnBackground
                                )
                            }
                            Surface(
                                color = NoticeContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = dDayText,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 7.dp
                                    ),
                                    style = LoorveTypography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Notice
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(completionRate * 100).toInt()}%",
                                style = LoorveTypography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnBackground
                            )
                            Text(
                                text = "복습 완료율",
                                style = LoorveTypography.labelMedium,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(NoticeContainer)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(reviewProgress)
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Notice, Active)
                                        )
                                    )
                            )
                        }
                        Text(
                            text = "$completedReviewCount / $totalReviewCount 복습 일정 완료",
                            style = LoorveTypography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // ── 에러 메시지 ──
            uiState.errorMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        color = Error,
                        style = LoorveTypography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            if (uiState.isLoading && uiState.reviewBlock == null) {
                item {
                    Text(
                        text = "⚠️ 블록 정보를 불러오는 중입니다...",
                        style = LoorveTypography.bodySmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // ── 학습 진도 입력 섹션 ──
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Surface,
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
                    shadowElevation = 6.dp
                ) {
                    StudyProgressInputSection(
                        onSave = { learningDateMillis, title, content ->
                            viewModel.saveProgress(
                                uid = uid,
                                blockId = blockId,
                                examId = resolvedBlock?.blockId ?: blockId,
                                title = title,
                                content = content,
                                learningDateMillis = learningDateMillis,
                                dailyCap = dailyCap,
                            )
                        },
                        isLoading = uiState.isLoading,
                        isSaveEnabled = resolvedBlock != null && resolvedBlock.examDate != 0L,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            // ── 학습기록 / 복습기록 탭 전환 UI ──
            item {
                RecordTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        viewModel.selectTab(tab, uid = uid, blockId = blockId)
                    }
                )
            }

            // ── 기록 리스트 (탭 조건 분기) ──
            if (selectedTab == ReviewBlockTab.STUDY_RECORD) {
                item {
                    StudyRecordListSection(
                        records = uiState.studyRecords,
                        reviewSchedules = uiState.reviewScheduleRecords,
                        isLoading = uiState.isLoading,
                        onDeleteRecord = { record -> viewModel.setRecordToDelete(record) },
                        onRecordClick = { record -> selectedStudyRecord = record }
                    )
                }
            }

            if (selectedTab == ReviewBlockTab.REVIEW_RECORD) {
                item {
                    ReviewRecordListSection(
                        scheduleItems = uiState.reviewScheduleRecords,
                        defaultAlarmTime = uiState.defaultAlarmTime,
                        onTimeSave = { item, hour, minute ->
                            viewModel.saveCustomAlarmTime(uid, item, hour, minute)
                        },
                        onScheduleClick = { item -> selectedReviewSchedule = item },
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun DetailAmbientAura() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 220.dp, top = 8.dp)
            .size(180.dp)
            .blur(72.dp)
            .background(
                color = SkyTint.copy(alpha = 0.5f),
                shape = CircleShape
            )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 220.dp, top = 360.dp)
            .size(220.dp)
            .blur(80.dp)
            .background(
                color = LavenderTint.copy(alpha = 0.46f),
                shape = CircleShape
            )
    )
}

// ── 학습기록 / 복습기록 탭 전환 UI ─────────────────────────────
@Composable
fun RecordTabRow(
    selectedTab: ReviewBlockTab,
    onTabSelected: (ReviewBlockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = CircleShape,
        color = SurfaceVariant,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentTab(
                text = "학습 기록",
                selected = selectedTab == ReviewBlockTab.STUDY_RECORD,
                contentDescription = "학습기록 탭",
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(ReviewBlockTab.STUDY_RECORD) }
            )
            SegmentTab(
                text = "복습 일정",
                selected = selectedTab == ReviewBlockTab.REVIEW_RECORD,
                contentDescription = "복습기록 탭",
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(ReviewBlockTab.REVIEW_RECORD) }
            )
        }
    }
}

@Composable
private fun SegmentTab(
    text: String,
    selected: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = if (selected) SurfaceSolid else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = LoorveTypography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) OnBackground else OnSurfaceVariant
            )
        }
    }
}

// ── 학습 기록 섹션 (헤더 + 목록) ──────────────────────────────
@Composable
fun StudyRecordListSection(
    records: List<StudyRecord>,
    reviewSchedules: List<ReviewScheduleItem> = emptyList(),
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onDeleteRecord: (StudyRecord) -> Unit = {},
    onRecordClick: (StudyRecord) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (records.isEmpty()) {
            Text(
                text = "아직 기록된 학습이 없어요. 첫 학습을 입력해보세요!",
                style = LoorveTypography.bodySmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            records.forEach { record ->
                val associatedSchedules = reviewSchedules.filter {
                    it.studyRecordId == record.id
                }
                StudyRecordMiniCard(
                    record = record,
                    isResolved = associatedSchedules.isNotEmpty() &&
                        associatedSchedules.all { it.status == ReviewStatus.COMPLETED },
                    isLoading = isLoading,
                    onDeleteClick = { onDeleteRecord(record) },
                    onClick = { onRecordClick(record) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── StudyRecordMiniCard ─────────────────────────────────────────
@Composable
fun StudyRecordMiniCard(
    record: StudyRecord,
    isResolved: Boolean = false,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onDeleteClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val dateText = remember(record.learningDate) {
        if (record.learningDate > 0L)
            SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(record.learningDate))
        else "-"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .alpha(if (isResolved) 0.62f else 1f)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "학습기록: ${record.title}, 날짜: $dateText"
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isResolved) SurfaceVariant else Surface,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isResolved) OnSurfaceVariant.copy(alpha = 0.14f) else SurfaceSolid,
                    shape = RoundedCornerShape(16.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Active, shape = RoundedCornerShape(16.dp))
            )

            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📖 $dateText",
                            style = LoorveTypography.labelSmall,
                            color = OnSurfaceVariant
                        )
                        IconButton(
                            onClick = {
                                onDeleteClick()
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .size(24.dp)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false).consume()
                                        waitForUpOrCancellation()?.consume()
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "학습기록 삭제",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (record.title.isNotBlank()) {
                        Text(
                            text = record.title,
                            style = LoorveTypography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    if (record.content.isNotBlank()) {
                        Text(
                            text = record.content,
                            style = LoorveTypography.bodySmall,
                            color = OnSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (record.title.isBlank() && record.content.isBlank()) {
                        Text(
                            text = "내용 없음",
                            style = LoorveTypography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyRecordDetailDialog(
    record: StudyRecord,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceSolid,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "학습기록",
                    style = LoorveTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground
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
                        Text("닫기")
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
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceSolid,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "복습 일정 ${item.reviewOrder}회차",
                    style = LoorveTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground
                )
                DetailTextBlock("학습 내용", item.title.ifBlank { "내용 없음" })
                DetailTextBlock("복습 예정일", dateText)
                DetailTextBlock("상태", item.status.detailLabel())
                DetailTextBlock("회차별 학습가이드", guide)
                DetailTextBlock("예상 소요 시간", "${item.estimatedReviewMinutes}분")
                if (item.rescheduleReason.orEmpty().isNotBlank()) {
                    DetailTextBlock("일정 변경 사유", item.rescheduleReason.orEmpty())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("닫기")
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = LoorveTypography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant
        )
        Text(
            text = value,
            style = LoorveTypography.bodyMedium,
            color = OnBackground
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

// ── 복습 기록 섹션 (헤더 + 목록) ──────────────────────────────
@Composable
fun ReviewRecordListSection(
    scheduleItems: List<ReviewScheduleItem>,
    defaultAlarmTime: Pair<Int, Int> = 9 to 0,
    onTimeSave: ((ReviewScheduleItem, Int, Int) -> Unit)? = null,
    onScheduleClick: (ReviewScheduleItem) -> Unit = {},
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (scheduleItems.isEmpty()) {
            Text(
                text = "아직 복습 일정이 없어요.",
                style = LoorveTypography.bodySmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
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
                    onClick = { onScheduleClick(item) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── ReviewRecordMiniCard ───────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewRecordMiniCard(
    item: ReviewScheduleItem,
    modifier: Modifier = Modifier,
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "복습기록: ${item.title}, 복습 ${item.reviewOrder}회차, ${item.status}, ${dateText}"
            },
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
        shadowElevation = 4.dp
    ) {
        Surface(
            color = AiSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top accent strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(GradientStart, GradientMiddle, GradientEnd)
                            )
                        )
                )

                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateText,
                            style = LoorveTypography.labelSmall,
                            color = Active
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (item.title.isNotBlank()) {
                        Text(
                            text = item.title,
                            style = LoorveTypography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (item.status) {
                                ReviewStatus.COMPLETED -> SuccessContainer
                                ReviewStatus.OVERDUE, ReviewStatus.FINAL_URGENT_REVIEW -> WarningContainer
                                else -> NoticeContainer
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = when (item.status) {
                                    ReviewStatus.PENDING -> "대기 중"
                                    ReviewStatus.COMPLETED -> "완료"
                                    ReviewStatus.OVERDUE -> "지연"
                                    ReviewStatus.FINAL_URGENT_REVIEW -> "최종"
                                    ReviewStatus.CRAM_MODE_REQUIRED -> "압축 필요"
                                    ReviewStatus.OVERLOADED_UNRESOLVED -> "과부하"
                                },
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                style = LoorveTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (item.status) {
                                    ReviewStatus.COMPLETED -> Success
                                    ReviewStatus.OVERDUE, ReviewStatus.FINAL_URGENT_REVIEW -> Warning
                                    else -> Notice
                                }
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            modifier = Modifier.clickable { showTimePicker = true },
                            shape = CircleShape,
                            color = SurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🔔 Alarm",
                                    style = LoorveTypography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = savedTime.orEmpty().ifBlank { "--:--" },
                                    style = LoorveTypography.labelSmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        val isCompleted = item.status == ReviewStatus.COMPLETED
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) Active else Color.Transparent
                                )
                                .border(
                                    BorderStroke(
                                        width = 1.5.dp,
                                        color = if (isCompleted) Active else TertiaryText
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
                                    tint = OnGradient,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
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
                    0 to 0
                }
            }
            ?: (0 to 0)
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Alarm",
                        style = LoorveTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimeInput(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                onTimeSave?.invoke(timePickerState.hour, timePickerState.minute)
                                showTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

// ── 1회독 권장일 카드 ─────────────────────────────────────────
@Composable
fun RecommendedCompletionCard(
    recommendedDateMillis: Long,
    deadlineBufferDays: Long,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA) }
    val dateText = remember(recommendedDateMillis) { sdf.format(Date(recommendedDateMillis)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription =
                    "시험 ${deadlineBufferDays}일 전까지 1회독 완료를 권장합니다. 권장 완료일: $dateText"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📚 1회독 완료 권장일",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "시험 ${deadlineBufferDays}일 전까지 1회독 완료를 권장합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "권장 완료일: $dateText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ScheduleSummaryCard(
    result: CreateStudyRecordResult,
    examDateMillis: Long
) {
    val zone = ZoneId.of("Asia/Seoul")
    val examDate = Instant.ofEpochMilli(examDateMillis).atZone(zone).toLocalDate()
    val schedules = result.schedules.filter { it.reviewOrder > 0 }
    val statusText = when (result.status) {
        ReviewPlanStatus.SCHEDULED -> "정상"
        ReviewPlanStatus.RESCHEDULED -> "일정 압축됨"
        ReviewPlanStatus.CRAM_MODE_REQUIRED,
        ReviewPlanStatus.INSUFFICIENT_WINDOW -> "벼락치기 모드 필요"
        ReviewPlanStatus.OVERLOADED_UNRESOLVED -> "일일 과부하 확인 필요"
        else -> result.status.name
    }
    val statusDescription = if (result.generationOutcome != ScheduleGenerationOutcome.FULL) {
        result.userMessage
    } else when (result.status) {
        ReviewPlanStatus.CRAM_MODE_REQUIRED,
        ReviewPlanStatus.INSUFFICIENT_WINDOW ->
            "시험일까지 정규 분산복습 최소기간이 부족합니다. 가능한 복습 일정을 만들었지만, 핵심 개념을 먼저 인출하고 오답을 빠르게 보완하는 압축 학습이 필요합니다."
        ReviewPlanStatus.RESCHEDULED ->
            "시험일까지의 기간에 맞춰 복습 간격을 압축했습니다. 각 복습에서는 재독보다 문제풀이·퀴즈·빈 종이 회상처럼 기억을 직접 꺼내는 방식으로 점검해 보세요."
        else -> result.userMessage
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = AiSurface,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.06f)),
        shadowElevation = 6.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (result.generationOutcome != ScheduleGenerationOutcome.FULL) "학습기록이 저장되었습니다."
                else "복습 일정 ${schedules.size}개가 생성되었습니다.",
                style = LoorveTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnBackground)
            Text(
                "이 학습기록에 대해 복습 일정 ${schedules.size}개를 만들었습니다. " +
                    "첫 복습은 ${schedules.minOfOrNull { it.reviewDate }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: "-"}, " +
                    "마지막 복습은 ${schedules.maxOfOrNull { it.reviewDate }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: result.lastReviewDate}입니다.",
                style = LoorveTypography.bodyMedium,
                color = OnSurfaceVariant
            )
            Text(
                "시험까지 ${java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(zone), examDate)}일 남았습니다.",
                style = LoorveTypography.bodySmall,
                color = OnSurfaceVariant
            )
            Surface(
                color = if (result.generationOutcome == ScheduleGenerationOutcome.FULL) {
                    SuccessContainer
                } else {
                    WarningContainer
                },
                shape = CircleShape
            ) {
                Text(
                    "상태: $statusText",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = LoorveTypography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (result.generationOutcome == ScheduleGenerationOutcome.FULL) {
                        Success
                    } else {
                        Warning
                    }
                )
            }
            Text(
                "일정 압축 여부: ${if (result.compressed) "압축됨" else "압축되지 않음"}",
                style = LoorveTypography.bodySmall,
                color = OnSurfaceVariant
            )
            Text(statusDescription, style = LoorveTypography.bodySmall, color = OnSurfaceVariant)
            schedules.forEach { item ->
                Text(
                    "• ${Instant.ofEpochMilli(item.reviewDate).atZone(zone).toLocalDate()} · ${item.recommendedMethod.ifBlank { "인출 연습과 오답 점검" }}",
                    style = LoorveTypography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

// ── 진도 입력 섹션 ──────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyProgressInputSection(
    onSave: (
        learningDateMillis: Long,
        title: String,
        content: String
    ) -> Unit,
    isLoading: Boolean,
    isSaveEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }

    val kstZone = remember { ZoneId.of("Asia/Seoul") }
    val todayMillis = remember {
        LocalDate.now(kstZone).atStartOfDay(kstZone).toInstant().toEpochMilli()
    }
    var selectedDateMillis by remember { mutableLongStateOf(todayMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val displayDateText = remember(selectedDateMillis) {
        SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREA).format(Date(selectedDateMillis))
    }

    val canSave = (titleText.isNotBlank() || contentText.isNotBlank()) && !isLoading && isSaveEnabled

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMs ->
                        val localDate = Instant.ofEpochMilli(utcMs)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        selectedDateMillis = localDate
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

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "오늘 학습 진도",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceSolid,
                contentColor = OnBackground
            ),
            contentPadding = PaddingValues(horizontal = 16.dp),
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = displayDateText, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = titleText,
            onValueChange = { titleText = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "학습 제목 입력" },
            label = { Text("학습 제목 (제목 또는 내용 중 하나 필수)") },
            placeholder = { Text("예: 수학 미분 1단원") },
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Active,
                unfocusedBorderColor = Color.Black.copy(alpha = 0.08f),
                focusedContainerColor = SurfaceSolid,
                unfocusedContainerColor = SurfaceSolid
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = contentText,
            onValueChange = { contentText = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "학습 내용 입력" },
            label = { Text("학습 내용") },
            placeholder = { Text("예: 미분의 정의, 극한 개념 복습 완료") },
            maxLines = 4,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Active,
                unfocusedBorderColor = Color.Black.copy(alpha = 0.08f),
                focusedContainerColor = SurfaceSolid,
                unfocusedContainerColor = SurfaceSolid
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

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
                .height(52.dp)
                .semantics { contentDescription = "학습 진도 저장 버튼" },
            enabled = canSave,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Black.copy(alpha = 0.08f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (canSave) {
                            Brush.linearGradient(
                                colors = listOf(GradientStart, GradientMiddle, GradientEnd)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFD1D5DB),
                                    Color(0xFFE5E7EB)
                                )
                            )
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        "학습 기록 및 복습 일정 생성",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── 복습 일정 리스트 ──────────────────────────────────────────
@Composable
fun ReviewScheduleList(
    items: List<ReviewScheduleItem>,
    overdueItems: List<ReviewScheduleItem>,
    reviewOverloadWarning: Boolean,
    modifier: Modifier = Modifier,
    onComplete: ((ReviewScheduleItem, CompletionResult) -> Unit)? = null
) {
    val sdf = remember { SimpleDateFormat("MM/dd (E)", Locale.KOREA) }

    Column(modifier = modifier) {
        if (reviewOverloadWarning) OverloadWarningBanner()

        if (overdueItems.isNotEmpty()) {
            Text(
                text = "⚠️ 누락된 복습 (오래된 순)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            overdueItems.forEach { item ->
                ReviewScheduleItemCard(
                    item = item,
                    dateText = sdf.format(Date(item.reviewDate))
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        val pendingItems = items.filter {
            it.status == ReviewStatus.PENDING || it.status == ReviewStatus.FINAL_URGENT_REVIEW
        }
        pendingItems.forEach { item ->
            ReviewScheduleItemCard(
                item = item,
                dateText = sdf.format(Date(item.reviewDate))
            )
        }
    }
}

@Composable
fun ReviewScheduleItemCard(
    item: ReviewScheduleItem,
    dateText: String
) {
    val (bgColor, statusLabel, statusDesc) = when (item.status) {
        ReviewStatus.OVERDUE ->
            Triple(Color(0xFFFFF3E0), "• 누락 ${item.overdueDays}일 경과", "누락된 복습 항목")
        ReviewStatus.FINAL_URGENT_REVIEW ->
            Triple(Color(0xFFFFEBEE), "🔴 긴급 복습", "시험 임박 긴급 복습 항목")
        ReviewStatus.COMPLETED ->
            Triple(Color(0xFFF1F8E9), "✅ 완료", "완료된 복습 항목")
        else ->
            Triple(MaterialTheme.colorScheme.surface, "", "예정된 복습 항목")
    }

    val isCompressed = item.compressedReview

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics { contentDescription = "$statusDesc: ${item.title}, 날짜: $dateText" },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dateText, style = MaterialTheme.typography.labelSmall)
                if (isCompressed) {
                    Surface(
                        color = Color(0xFF1565C0).copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = "[압축 복습]",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (statusLabel.isNotBlank()) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.status == ReviewStatus.OVERDUE ||
                            item.status == ReviewStatus.FINAL_URGENT_REVIEW)
                            MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
fun OverloadWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEBEE))
            .padding(12.dp)
            .semantics { contentDescription = "경고: 시험 전 일정이 초과되었습니다." },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "시험 전 남은 기간에 모든 누락 복습을 배치하기 어렵습니다. 중요도 순으로 우선 복습하세요.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFC62828)
        )
    }
}