package com.loorve.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.ui.component.*
import com.loorve.ui.theme.*

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
                    Text(
                        text = "Loorve",
                        style = LoorveTypography.titleLarge,
                        color = Primary
                    )
                },
                actions = {
                    IconButton(onClick = { /* 알림 */ }) {
                        Icon(Icons.Outlined.Notifications, null, tint = OnBackground)
                    }
                    IconButton(onClick = onNavigateToMyPage) {
                        Icon(Icons.Outlined.AccountCircle, null, tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                top = 8.dp,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1) D-Day 카드
            uiState.nearestExam?.let { exam ->
                item {
                    DdayCard(
                        subjectName = exam.subjectName,
                        daysLeft = exam.daysLeft,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2) 오늘의 학습 입력 섹션
            item {
                SectionLabel("오늘의 학습")
                ProgressInputSection(
                    exams = uiState.exams,
                    onSave = { input -> viewModel.addProgress(input) },
                    isSaving = uiState.isSaving
                )
            }

            // 3) 내 시험 목록
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
                                    text = exam.examDateFormatted,
                                    style = LoorveTypography.labelMedium,
                                    color = OnSurfaceVariant
                                )
                            }
                            if (exam.daysLeft >= 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = if (exam.daysLeft == 0) "D-Day" else "D-${exam.daysLeft}",
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

            // 4) 학습 진도 기록
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
                                    color = if (progress.isCompleted)
                                        Success.copy(alpha = 0.15f)
                                    else
                                        Tertiary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (progress.isCompleted) "완료" else "진행 중",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = LoorveTypography.labelMedium,
                                        color = if (progress.isCompleted) Success else Tertiary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}