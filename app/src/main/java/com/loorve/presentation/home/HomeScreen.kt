// 경로: app/src/main/java/com/loorve/presentation/home/HomeScreen.kt
package com.loorve.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.Progress
import com.loorve.ui.component.BannerAdView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProgressDetail: (progressId: String) -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }
    val snackbarHostState = remember { SnackbarHostState() }

    // [추가] 배너 광고 표시 여부 상태 — BannerAdView의 onAdFailed 콜백으로 갱신
    // 광고 실패 시 false → LazyColumn contentPadding bottom을 16dp로 전환
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
        topBar = {
            TopAppBar(
                title = { Text("Loorve") },
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(
                            imageVector        = Icons.Default.DateRange,
                            contentDescription = "복습 캘린더"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text  = uiState.errorMessage ?: "오류가 발생했습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadExams() }) { Text("다시 시도") }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        // [수정] 광고 표시 여부에 따라 bottom padding 동적 적용
                        // isBannerVisible = true  → 66dp (배너 높이 ~50dp + 여백 16dp)
                        // isBannerVisible = false → 16dp (광고 실패 시 불필요한 공백 제거)
                        contentPadding      = PaddingValues(
                            top    = 16.dp,
                            bottom = if (isBannerVisible) 66.dp else 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ── 진도 입력 섹션
                        // [검증] ProgressInputSection은 adFailed 상태와 완전히 독립적으로 동작.
                        // isSaveEnabled = selectedExam != null && content.isNotBlank() 로만 제어됨.
                        item {
                            ProgressInputSection(
                                exams  = uiState.exams,
                                onSave = { id, content, completed, total ->
                                    viewModel.addProgress(id, content, completed, total)
                                }
                            )
                        }

                        // ── AdMob 배너 광고
                        // [수정] onAdFailed 콜백으로 isBannerVisible 상태 갱신
                        // 광고 실패 시 BannerAdView가 Compose tree에서 제거되고,
                        // LazyColumn의 contentPadding bottom이 66dp → 16dp로 전환됨.
                        item {
                            BannerAdView(
                                modifier   = Modifier.fillMaxWidth(),
                                onAdFailed = { isBannerVisible = false }
                            )
                        }

                        // ── 시험 목록 헤더
                        if (uiState.exams.isNotEmpty()) {
                            item {
                                Text(
                                    text     = "내 시험 목록",
                                    style    = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(uiState.exams) { exam ->
                                ExamListItem(
                                    subjectName = exam.subjectName,
                                    examDate    = exam.examDate,
                                    formatter   = dateFormatter
                                )
                            }
                        } else {
                            item {
                                Column(
                                    modifier            = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text  = "등록된 시험이 없습니다.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text  = "시험을 추가해 보세요!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // ── 학습 진도 목록 헤더
                        if (uiState.progressList.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text  = "학습 진도 기록",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(uiState.progressList) { progress ->
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

            // ── AdMob 배너 광고 — Box 최하단 고정 오버레이
            // [수정] onAdFailed 콜백으로 isBannerVisible 상태 갱신
            // LazyColumn item의 BannerAdView와 역할 분리:
            // → LazyColumn item: 스크롤 영역 내 배너 삽입 (콘텐츠 흐름에 자연스럽게 포함)
            // → 아래 주석처리: HomeScreen 구조상 LazyColumn item 배너만 사용
            // (필요 시 Box 하단 고정 배너로 교체 가능)
        }
    }
}

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

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text  = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text  = "${progress.completedCount} / ${progress.totalCount}  " +
                        if (progress.isCompleted) "✅ 완료" else "⏳ 진행 중",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = subjectName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text  = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}