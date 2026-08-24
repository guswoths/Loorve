// app/src/main/java/com/loorve/presentation/home/HomeScreen.kt
package com.loorve.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.Progress
import com.loorve.ui.component.BannerAdView
import com.loorve.ui.component.DdayCard
import com.loorve.ui.component.EmptyStateView
import com.loorve.ui.component.LoorveCard
import com.loorve.ui.component.SectionLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProgressDetail: (progressId: String) -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToMyPage: () -> Unit = {},
    onNavigateToExamSetting: () -> Unit = {},   // 빈 상태 CTA용 — 확인 필요: NavHost에 파라미터 추가 필요
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }
    val snackbarHostState = remember { SnackbarHostState() }
    var isBannerVisible by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.progressSaveResult) {
        when (uiState.progressSaveResult) {
            true -> {
                snackbarHostState.showSnackbar("진도가 저장되었습니다 ✅")
                viewModel.consumeProgressSaveResult()
            }
            false -> {
                snackbarHostState.showSnackbar("저장에 실패했습니다. 다시 시도해 주세요.")
                viewModel.consumeProgressSaveResult()
            }
            null -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Loorve",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor        = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                actions = {
                    IconButton(
                        onClick = onNavigateToCalendar,
                        modifier = Modifier.semantics { contentDescription = "복습 캘린더" }
                    ) {
                        Icon(
                            imageVector        = Icons.Default.DateRange,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onNavigateToMyPage,
                        modifier = Modifier.semantics { contentDescription = "마이페이지" }
                    ) {
                        Icon(
                            imageVector        = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        // ✅ 광고 배너: 학습 흐름을 방해하지 않도록 하단 고정
        bottomBar = {
            if (isBannerVisible) {
                Surface(
                    color     = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    BannerAdView(
                        modifier   = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        onAdFailed = { isBannerVisible = false }
                    )
                }
            }
        }
    ) { paddingValues ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = uiState.errorMessage ?: "오류가 발생했습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadExams() }) { Text("다시 시도") }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier       = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── D-Day 요약 카드 (최근 시험 1개) ──────────────────
                    val nearestExam = uiState.exams.minByOrNull { it.examDate }
                    if (nearestExam != null && nearestExam.examDate > 0L) {
                        val today = LocalDate.now()
                        val examLocalDate = Instant.ofEpochMilli(nearestExam.examDate)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        val daysLeft = ChronoUnit.DAYS.between(today, examLocalDate)
                        if (daysLeft >= 0) {
                            item(key = "dday_card") {
                                DdayCard(
                                    subjectName = nearestExam.subjectName,
                                    daysLeft    = daysLeft
                                )
                            }
                        }
                    }

                    // ── 오늘의 진도 입력 ──────────────────────────────────
                    item(key = "progress_input") {
                        ProgressInputSection(
                            exams  = uiState.exams,
                            onSave = { id, content, completed, total ->
                                viewModel.addProgress(id, content, completed, total)
                            }
                        )
                    }

                    // ── 내 시험 목록 ──────────────────────────────────────
                    if (uiState.exams.isNotEmpty()) {
                        item(key = "exam_section_label") {
                            SectionLabel(text = "내 시험")
                        }
                        items(
                            items = uiState.exams,
                            key   = { it.examId }
                        ) { exam ->
                            ExamListItem(
                                subjectName = exam.subjectName,
                                examDate    = exam.examDate,
                                formatter   = dateFormatter
                            )
                        }
                    } else {
                        item(key = "exam_empty") {
                            EmptyStateView(
                                message     = "등록된 시험이 없습니다.",
                                subMessage  = "시험을 추가하면 복습 일정이 자동으로 생성됩니다.",
                                actionLabel = "시험 추가하기",
                                onAction    = onNavigateToExamSetting
                            )
                        }
                    }

                    // ── 학습 진도 기록 ────────────────────────────────────
                    if (uiState.progressList.isNotEmpty()) {
                        item(key = "progress_section_label") {
                            SectionLabel(text = "학습 진도 기록")
                        }
                        items(
                            items = uiState.progressList,
                            key   = { it.progressId }
                        ) { progress ->
                            ProgressListItem(
                                progress  = progress,
                                formatter = dateFormatter,
                                onClick   = { onNavigateToProgressDetail(progress.progressId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 진도 목록 아이템 ──────────────────────────────────────────────────────
@Composable
private fun ProgressListItem(
    progress: Progress,
    formatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    val formattedDate = remember(progress.createdAt) {
        if (progress.createdAt <= 0L) "날짜 미설정"
        else Instant.ofEpochMilli(progress.createdAt)
            .atZone(ZoneId.of("Asia/Seoul"))
            .toLocalDate()
            .format(formatter)
    }

    LoorveCard(
        modifier  = Modifier.fillMaxWidth(),
        onClick   = onClick,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = progress.content.take(30) + if (progress.content.length > 30) "…" else "",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text  = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text  = "${progress.completedCount} / ${progress.totalCount}  " +
                        if (progress.isCompleted) "완료" else "진행 중",
                style = MaterialTheme.typography.bodyMedium,
                color = if (progress.isCompleted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── 시험 목록 아이템 ──────────────────────────────────────────────────────
@Composable
private fun ExamListItem(
    subjectName: String,
    examDate: Long,
    formatter: DateTimeFormatter
) {
    val formattedDate = remember(examDate) {
        if (examDate <= 0L) "날짜 미설정"
        else Instant.ofEpochMilli(examDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
    }

    LoorveCard(
        modifier  = Modifier.fillMaxWidth(),
        elevation = 0.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = subjectName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text  = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}