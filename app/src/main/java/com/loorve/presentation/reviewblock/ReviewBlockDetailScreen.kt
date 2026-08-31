package com.loorve.presentation.reviewblock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.CompletionResult
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.model.ReviewScheduleItem
import com.loorve.domain.model.ReviewStatus
import com.loorve.domain.model.StudyRecord
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
    block: ReviewBlock?,                    // NavHost에서 전달 (null이면 ViewModel이 자체 로드)
    onNavigateBack: () -> Unit,
    viewModel: ReviewBlockDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ✅ [포인트 3] SnackbarHostState 추가
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(blockId) {
        // ✅ [포인트 4] NavHost에서 block=null로 전달될 때를 대비해 ViewModel에서 로드
        viewModel.loadBlockData(uid, blockId, externalBlock = block)
    }

    // ✅ [포인트 3] savedSuccess 감지 → Snackbar 표시 후 리셋
    LaunchedEffect(uiState.savedSuccess) {
        if (uiState.savedSuccess) {
            snackbarHostState.showSnackbar("복습 일정이 생성되었습니다.")
            viewModel.resetSavedSuccess()
        }
    }

    // ✅ [포인트 4] block 데이터를 uiState.reviewBlock에서 읽음 (null-safe)
    val resolvedBlock = uiState.reviewBlock
    val examDateMillis = resolvedBlock?.examDate ?: 0L
    val prepStartDateMillis = resolvedBlock?.prepStartDate ?: 0L
    val dailyCap = resolvedBlock?.dailyCap ?: 5
    val examName = resolvedBlock?.examName?.ifBlank { resolvedBlock.title }
        ?: resolvedBlock?.title
        ?: blockId

    // D-Day 계산
    val dDayText = if (examDateMillis > 0L) {
        val examLocal = Instant.ofEpochMilli(examDateMillis)
            .atZone(ZoneId.of("Asia/Seoul")).toLocalDate()
        val days = java.time.temporal.ChronoUnit.DAYS
            .between(LocalDate.now(), examLocal).toInt()
        when {
            days > 0  -> "D-$days"
            days == 0 -> "D-Day"
            else      -> "D+${-days}"
        }
    } else "D-?"

    Scaffold(
        // ✅ [포인트 3] SnackbarHost 연결
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        examName,
                        style = LoorveTypography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "뒤로")
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
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── 블록 요약 정보 ──
            item {
                LoorveCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = examName,
                                style = LoorveTypography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnBackground
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "하루 최대 $dailyCap 회 복습",
                                style = LoorveTypography.labelSmall,
                                color = OnSurfaceVariant
                            )
                        }
                        Surface(
                            color = Primary.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = dDayText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = LoorveTypography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
            }

            // ── 에러 메시지 ──
            uiState.errorMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = LoorveTypography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // ✅ [포인트 4] examDateMillis=0이면 저장 불가 → 사용자에게 안내 메시지
            if (examDateMillis == 0L) {
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
                StudyProgressInputSection(
                    onSave = { learningDateMillis, title, content ->
                        viewModel.saveProgress(
                            uid = uid,
                            blockId = blockId,
                            // ✅ [포인트 4] examId를 blockId 대신 실제 examId로 전달
                            examId = resolvedBlock?.blockId ?: blockId,
                            title = title,
                            content = content,
                            learningDateMillis = learningDateMillis,
                            examDateMillis = examDateMillis,
                            prepStartDateMillis = prepStartDateMillis,
                            dailyCap = dailyCap
                        )
                    },
                    isLoading = uiState.isLoading,
                    // ✅ [포인트 3] examDate 미로드 상태에서는 저장 버튼 비활성화
                    isSaveEnabled = examDateMillis > 0L
                )
            }

            // ── 복습 일정 리스트 ──
            item {
                ReviewScheduleList(
                    items = uiState.scheduleItems,
                    overdueItems = uiState.overdueItems,
                    reviewOverloadWarning = uiState.reviewOverloadWarning,
                    onComplete = { scheduleItem, result ->
                        viewModel.completeReview(uid, scheduleItem, result, examDateMillis)
                    }
                )
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

// ── 진도 입력 섹션 ──────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyProgressInputSection(
    onSave: (learningDateMillis: Long, title: String, content: String) -> Unit,
    isLoading: Boolean,
    // ✅ [포인트 3] examDate 미로드 시 버튼 비활성화용 파라미터 추가
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

    // ✅ [포인트 3] canSave: 제목 OR 내용 중 하나 입력 + 로딩 중 아님 + examDate 로드 완료
    val canSave = (titleText.isNotBlank() || contentText.isNotBlank()) && !isLoading && isSaveEnabled

    // ── DatePickerDialog ──
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMs ->
                        // UTC 자정(ms) → KST 자정(ms) 변환 (정상 유지)
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

    // ── UI ──
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "오늘 학습 진도",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // ① 날짜 선택 버튼
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "학습 날짜 선택: $displayDateText" },
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

        // ② 제목 입력
        OutlinedTextField(
            value = titleText,
            onValueChange = { titleText = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "학습 제목 입력" },
            // ✅ [포인트 3] 라벨에 필수 조건 힌트 추가
            label = { Text("학습 제목 (제목 또는 내용 중 하나 필수)") },
            placeholder = { Text("예: 수학 미분 1단원") },
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ③ 내용 입력
        OutlinedTextField(
            value = contentText,
            onValueChange = { contentText = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "학습 내용 입력" },
            label = { Text("학습 내용") },
            placeholder = { Text("예: 미분의 정의, 극한 개념 복습 완료") },
            maxLines = 4,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ④ 저장 버튼
        Button(
            onClick = {
                if (canSave) {
                    onSave(selectedDateMillis, titleText.trim(), contentText.trim())
                    // ✅ [포인트 3] 저장 후 입력 필드 초기화 (날짜는 편의상 유지)
                    titleText = ""
                    contentText = ""
                }
            },
            modifier = Modifier
                .align(Alignment.End)
                .semantics { contentDescription = "학습 진도 저장 버튼" },
            enabled = canSave
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("저장하고 복습 일정 생성")
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
    onComplete: (ReviewScheduleItem, CompletionResult) -> Unit,
    modifier: Modifier = Modifier
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
                    dateText = sdf.format(Date(item.reviewDate)),
                    onComplete = onComplete
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
                dateText = sdf.format(Date(item.reviewDate)),
                onComplete = onComplete
            )
        }
    }
}

@Composable
fun ReviewScheduleItemCard(
    item: ReviewScheduleItem,
    dateText: String,
    onComplete: (ReviewScheduleItem, CompletionResult) -> Unit
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

            if (item.status != ReviewStatus.COMPLETED) {
                Column {
                    TextButton(
                        onClick = { onComplete(item, CompletionResult.REMEMBERED) },
                        modifier = Modifier.semantics {
                            contentDescription = "기억함 버튼 - ${item.title}"
                        }
                    ) { Text("기억함", color = Color(0xFF2E7D32)) }
                    TextButton(
                        onClick = { onComplete(item, CompletionResult.FORGOT) },
                        modifier = Modifier.semantics {
                            contentDescription = "잊어버림 버튼 - ${item.title}"
                        }
                    ) { Text("잊어버림", color = MaterialTheme.colorScheme.error) }
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