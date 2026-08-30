// 경로: app/src/main/java/com/loorve/presentation/calendar/AddReviewBlockScreen.kt
package com.loorve.presentation.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.domain.model.ReviewBlock
import com.loorve.presentation.reviewblock.ReviewBlockUiState
import com.loorve.presentation.reviewblock.ReviewBlockViewModel
import com.loorve.ui.component.BannerAdView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewBlockScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit = {},
    reviewCalendarViewModel: ReviewCalendarViewModel = hiltViewModel(),
    // ✅ Hilt로 주입된 ReviewBlockViewModel 사용 (Firestore 직접 호출 제거)
    reviewBlockViewModel: ReviewBlockViewModel = hiltViewModel()
) {
    var examName by remember { mutableStateOf("") }
    var examDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedCycleOption by remember { mutableStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val currentUid by reviewCalendarViewModel.currentUid.collectAsState()
    val uiState by reviewBlockViewModel.uiState.collectAsState()

    // 화면 진입 시 토큰 갱신
    LaunchedEffect(Unit) {
        reviewCalendarViewModel.refreshUid()
    }

    // ✅ ViewModel uiState 변화 감지 → 성공/실패 처리
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ReviewBlockUiState.Success -> {
                reviewCalendarViewModel.reloadCurrentMonth()
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
                TextButton(onClick = {
                    examDateMillis = datePickerState.selectedDateMillis
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

    val isLoading = uiState is ReviewBlockUiState.Loading

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("복습 블록 생성하기") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
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
                        if (uid == null) {
                            reviewCalendarViewModel.refreshUid()
                            return@Button
                        }
                        if (examName.isBlank()) return@Button
                        if (examDateMillis == null) return@Button

                        val examDateLocal = Instant.ofEpochMilli(examDateMillis!!)
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toLocalDate()

                        // ✅ blockId를 미리 생성하여 ReviewBlock 객체에 포함
                        // → Rules hasAll(['blockId', 'uid', 'createdAt']) 충족
                        val newBlockId = UUID.randomUUID().toString()
                        val now = System.currentTimeMillis()

                        val reviewBlock = ReviewBlock(
                            blockId     = newBlockId,
                            uid         = uid,
                            date        = examDateLocal.toString(),
                            title       = examName.trim(),
                            description = "복습 주기: ${if (selectedCycleOption == 0) "에빙하우스" else "직접 세팅"}",
                            isCompleted = false,
                            createdAt   = now,
                            updatedAt   = now
                        )

                        // ✅ Firestore 직접 호출 대신 ViewModel을 통해 저장
                        reviewBlockViewModel.saveReviewBlock(reviewBlock)
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
                            modifier    = Modifier.size(20.dp),
                            color       = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text  = "블록 생성하기",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                BannerAdView(modifier = Modifier.fillMaxWidth())
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
                text  = "시험 정보와 복습 주기를 정하면 블록이 자동 생성됩니다.",
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text     = "💡",
                        style    = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 1.dp, end = 8.dp)
                    )
                    Column {
                        Text(
                            text  = "블록 생성 전 확인",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text  = "시험 종료일까지의 기간에 맞춰 복습 간격이 자동으로 압축되거나 조정됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            OutlinedTextField(
                value         = examName,
                onValueChange = { examName = it },
                label         = { Text("시험 이름") },
                placeholder   = { Text("한국사능력검정 심화") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )

            OutlinedTextField(
                value         = examDateDisplay ?: "",
                onValueChange = { },
                label         = { Text("시험 종료일") },
                placeholder   = { Text("시험 종료일을 선택해주세요") },
                modifier      = Modifier.fillMaxWidth(),
                readOnly      = true,
                singleLine    = true,
                trailingIcon  = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector        = Icons.Outlined.CalendarToday,
                            contentDescription = "날짜 선택"
                        )
                    }
                }
            )

            Text(
                text  = "복습 주기",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = if (selectedCycleOption == 0)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedCycleOption == 0, onClick = { selectedCycleOption = 0 })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = "에빙하우스 망각주기",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text  = "1일 · 3일 · 7일 · 14일 · 30일 자동 배치",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = if (selectedCycleOption == 1)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedCycleOption == 1, onClick = { selectedCycleOption = 1 })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = "직접 세팅",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text  = "복습 간격을 직접 지정해 나만의 블록 구성",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}