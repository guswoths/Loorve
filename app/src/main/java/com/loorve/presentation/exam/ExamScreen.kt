package com.loorve.presentation.exam

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add       // ✅ 중복 제거: 여기서 한 번만 선언
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.ui.component.*
import com.loorve.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    viewModel: ExamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<ExamUiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("시험 관리",
                        style = LoorveTypography.titleLarge,
                        color = OnBackground)
                },
                actions = {
                    IconButton(onClick = {
                        editingExam = null
                        showBottomSheet = true
                    }) {
                        Icon(Icons.Default.Add, "시험 추가", tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(   // ✅ 수정: 4-파라미터 형태 사용
                top = 16.dp,
                bottom = 88.dp,
                start = 0.dp,
                end = 0.dp
            )
        ) {
            if (uiState.exams.isEmpty()) {
                item {
                    EmptyStateView(
                        message = "등록된 시험이 없습니다",
                        subMessage = "우측 상단 + 버튼으로 시험을 추가하세요",
                        actionLabel = "+ 시험 추가",
                        onActionClick = { showBottomSheet = true }
                    )
                }
            } else {
                items(uiState.exams, key = { it.id }) { exam ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteExam(exam.id)
                                true
                            } else false
                        }
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled)
                            Color.Transparent else Error,
                        label = "swipeBg"
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bgColor, MaterialTheme.shapes.medium)
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, null, tint = Color.White)
                            }
                        }
                    ) {
                        LoorveCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(exam.subjectName,
                                        style = LoorveTypography.titleMedium,
                                        color = OnBackground)
                                    Spacer(Modifier.height(4.dp))
                                    Text(exam.examDateFormatted,
                                        style = LoorveTypography.bodyMedium,
                                        color = OnSurfaceVariant)
                                }
                                if (exam.daysLeft >= 0) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = Primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = if (exam.daysLeft == 0) "D-Day" else "D-${exam.daysLeft}",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = LoorveTypography.labelMedium,
                                            color = Primary
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

    if (showBottomSheet) {
        ExamBottomSheet(
            exam = editingExam,
            onDismiss = { showBottomSheet = false },
            onSave = { subjectName, date ->
                if (editingExam != null) {
                    viewModel.updateExam(editingExam!!.id, subjectName, date)
                } else {
                    viewModel.addExam(subjectName, date)
                }
                showBottomSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamBottomSheet(
    exam: ExamUiModel?,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate) -> Unit
) {
    var subjectName by remember { mutableStateOf(exam?.subjectName ?: "") }
    var selectedDate by remember { mutableStateOf(exam?.examDate ?: LocalDate.now().plusDays(30)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
            .atStartOfDay()
            .toInstant(java.time.ZoneOffset.UTC)
            .toEpochMilli()
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (exam != null) "시험 수정" else "시험 추가",
                style = LoorveTypography.titleMedium,
                color = OnBackground
            )
            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("과목명", color = OnSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = OnBackground,
                    unfocusedTextColor = OnBackground,
                    cursorColor = Primary
                )
            )
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariant)
            ) {
                Text(
                    text = "시험일: ${selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))}",
                    color = OnBackground
                )
            }
            GradientButton(
                text = "시험 저장",
                onClick = { if (subjectName.isNotBlank()) onSave(subjectName, selectedDate) },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("취소", color = OnSurfaceVariant)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("확인", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소", color = OnSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}