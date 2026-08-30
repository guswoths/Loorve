// 경로: app/src/main/java/com/loorve/presentation/calendar/AddReviewBlockScreen.kt
package com.loorve.presentation.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.presentation.reviewblock.ReviewBlockUiState
import com.loorve.presentation.reviewblock.ReviewBlockViewModel
import com.loorve.ui.component.BannerAdView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewBlockScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    reviewCalendarViewModel: ReviewCalendarViewModel = hiltViewModel(),
    reviewBlockViewModel: ReviewBlockViewModel = hiltViewModel()
) {
    var examName by remember { mutableStateOf("") }
    var examDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedCycleOption by remember { mutableStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val currentUid by reviewCalendarViewModel.currentUid.collectAsState()
    val uiState by reviewBlockViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        reviewCalendarViewModel.refreshUid()
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ReviewBlockUiState.Success -> {
                reviewBlockViewModel.resetState()
                onSaveSuccess()
            }

            is ReviewBlockUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "저장에 실패했습니다: ${state.message}",
                    duration = SnackbarDuration.Long
                )
                reviewBlockViewModel.resetState()
            }

            else -> Unit
        }
    }

    val examDateDisplay = examDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.of("Asia/Seoul"))
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = examDateMillis
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        examDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val isLoading = uiState is ReviewBlockUiState.Loading

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("복습 블록 생성하기")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                Button(
                    onClick = {
                        val uid = currentUid

                        when {
                            uid.isNullOrBlank() -> {
                                // ✅ refreshUid()를 coroutineScope.launch 안으로 이동
                                coroutineScope.launch {
                                    reviewCalendarViewModel.refreshUid()
                                    snackbarHostState.showSnackbar(
                                        message = "로그인 정보를 확인 중입니다. 잠시 후 다시 시도해주세요."
                                    )
                                }
                            }

                            examName.isBlank() -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("시험 이름을 입력해주세요.")
                                }
                            }

                            examDateMillis == null -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("시험 종료일을 선택해주세요.")
                                }
                            }

                            else -> {
                                reviewBlockViewModel.createReviewBlock(
                                    uid = uid,
                                    examName = examName.trim(),
                                    examDateMillis = examDateMillis,
                                    cycleOption = selectedCycleOption
                                )
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "블록 생성하기",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                BannerAdView(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "시험 정보와 복습 주기를 정하면 복습 일정이 자동 생성됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "💡",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 1.dp, end = 8.dp)
                    )

                    Column {
                        Text(
                            text = "블록 생성 전 확인",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "시험일까지 남은 기간에 맞춰 복습 간격이 자동으로 조정됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            OutlinedTextField(
                value = examName,
                onValueChange = { examName = it },
                label = {
                    Text("시험 이름")
                },
                placeholder = {
                    Text("한국사능력검정 심화")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = examName.isBlank() && isLoading
            )

            OutlinedTextField(
                value = examDateDisplay.orEmpty(),
                onValueChange = {},
                label = {
                    Text("시험 종료일")
                },
                placeholder = {
                    Text("시험 종료일을 선택해주세요")
                },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { showDatePicker = true },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = "날짜 선택"
                        )
                    }
                }
            )

            Text(
                text = "복습 주기",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedCycleOption == 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCycleOption == 0,
                        onClick = {
                            selectedCycleOption = 0
                        },
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "에빙하우스 망각주기",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            text = "1일 · 3일 · 7일 · 14일 · 30일 자동 배치",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedCycleOption == 1) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedCycleOption == 1,
                        onClick = {
                            selectedCycleOption = 1
                        },
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "직접 세팅",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        Text(
                            text = "기본 복습 간격 1일 · 3일 · 7일 자동 배치",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    LaunchedEffect(
        key1 = examName,
        key2 = examDateMillis,
        key3 = currentUid
    ) {
        if (uiState is ReviewBlockUiState.Idle) {
            return@LaunchedEffect
        }
    }
}