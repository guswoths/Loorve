// 경로: app/src/main/java/com/loorve/presentation/calendar/ReviewCalendarScreen.kt
package com.loorve.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.ReviewSchedule
import com.loorve.ui.component.BannerAdView
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCalendarScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToAddBlock: () -> Unit = {},
    // isEmbedded=true: 탭 내부 호출 → Scaffold 없이 Column만 사용
    // isEmbedded=false: 독립 라우트 진입 → Scaffold 포함
    isEmbedded: Boolean = false,
    viewModel: ReviewCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isBannerVisible by remember { mutableStateOf(true) }

    // 오늘 날짜를 기본 선택값으로 설정 (UI 초기 상태 보정)
    LaunchedEffect(Unit) {
        viewModel.loadCurrentMonth()
        viewModel.onDateSelected(LocalDate.now())
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message     = msg,
                actionLabel = "닫기",
                duration    = SnackbarDuration.Long
            )
            viewModel.onDismissError()
        }
    }

    val topBar: @Composable () -> Unit = {
        TopAppBar(
            title = { Text("복습") },
            navigationIcon = {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            }
        )
    }

    val bodyContent: @Composable (PaddingValues) -> Unit = { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            SelectedDateSchedulePanel(
                selectedDate         = uiState.selectedDate,
                schedules            = uiState.selectedDateSchedules,
                onCompleteSchedule   = { viewModel.onCompleteSchedule(it) },
                onToggleCompletion   = { scheduleId, current ->
                    viewModel.toggleReviewCompletion(scheduleId, current)
                },
                isBannerVisible      = isBannerVisible,
                onNavigateToAddBlock = onNavigateToAddBlock,
                modifier             = Modifier.weight(1f)
            )

            BannerAdView(
                modifier   = Modifier.fillMaxWidth(),
                onAdFailed = { isBannerVisible = false }
            )
        }
    }

    if (isEmbedded) {
        // 탭 내부: Scaffold 없이 TopAppBar + Body를 Column으로 직접 배치
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            bodyContent(PaddingValues(0.dp))
        }
    } else {
        // 독립 라우트: 기존 Scaffold 유지
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = topBar
        ) { paddingValues ->
            bodyContent(paddingValues)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AddBlockDashedCard — 복습 블록이 없거나 추가 버튼으로 사용되는 점선 카드
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AddBlockDashedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dashWidth: Dp = 8.dp,
    gapWidth: Dp = 6.dp,
    strokeWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 12.dp
) {
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .drawBehind {
                val dash = dashWidth.toPx()
                val gap  = gapWidth.toPx()
                val sw   = strokeWidth.toPx()
                val cr   = cornerRadius.toPx()
                drawRoundRect(
                    color        = borderColor,
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(
                        width      = sw,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f)
                    )
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.Add,
                contentDescription = "복습블록 추가",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier           = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text  = "복습블록 추가",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SelectedDateSchedulePanel
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SelectedDateSchedulePanel(
    selectedDate: LocalDate?,
    schedules: List<ReviewSchedule>,
    onCompleteSchedule: (String) -> Unit,
    onToggleCompletion: (String, Boolean) -> Unit,
    isBannerVisible: Boolean,
    onNavigateToAddBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 달력 제거로 selectedDate 기반 제목 대신 고정 텍스트 사용
    val title = "오늘 복습 일정"

    Column(modifier = modifier) {
        Text(
            text     = title,
            style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (schedules.isEmpty()) {
            // 복습 블록이 없을 때 점선 추가 블록 표시
            AddBlockDashedCard(
                onClick  = onNavigateToAddBlock,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    bottom = if (isBannerVisible) 66.dp else 16.dp
                )
            ) {
                // 생성된 복습 블록 목록
                items(schedules) { schedule ->
                    ReviewScheduleItem(
                        schedule   = schedule,
                        onComplete = { onCompleteSchedule(schedule.reviewScheduleId) },
                        onToggle   = { onToggleCompletion(schedule.reviewScheduleId, schedule.isCompleted) }
                    )
                }
                // 항상 목록 마지막에 점선 추가 블록
                item {
                    AddBlockDashedCard(
                        onClick  = onNavigateToAddBlock,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ReviewScheduleItem
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ReviewScheduleItem(
    schedule: ReviewSchedule,
    onComplete: () -> Unit,
    onToggle: () -> Unit
) {
    val titleStyle = if (schedule.isCompleted) {
        MaterialTheme.typography.bodyLarge.copy(
            fontWeight     = FontWeight.SemiBold,
            textDecoration = TextDecoration.LineThrough,
            color          = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (schedule.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked         = schedule.isCompleted,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text  = "${schedule.reviewRound}회차 복습",
                        style = titleStyle
                    )
                    if (schedule.isCompleted) {
                        Text(
                            text  = "완료됨 ✅",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (!schedule.isCompleted) {
                Button(
                    onClick        = onComplete,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    Text("완료", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}