package com.loorve.presentation.exam

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun ExamSettingScreen(
    onSaveSuccess: () -> Unit,
    viewModel: ExamSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }

    // ──────── ONE-SHOT 이벤트 ────────
    var navigated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExamSettingEvent.SaveSuccess -> {
                    if (!navigated) {
                        navigated = true
                        onSaveSuccess()
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // ──────── 시험일 DatePickerDialog ────────
    val examCalendar = remember { Calendar.getInstance() }
    val examDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            examCalendar.set(year, month, dayOfMonth, 0, 0, 0)
            examCalendar.set(Calendar.MILLISECOND, 0)
            viewModel.onExamDateSelected(examCalendar.timeInMillis)
        },
        examCalendar.get(Calendar.YEAR),
        examCalendar.get(Calendar.MONTH),
        examCalendar.get(Calendar.DAY_OF_MONTH)
    )

    // ──────── 학습 종료일 DatePickerDialog ────────
    val studyEndCalendar = remember { Calendar.getInstance() }
    val studyEndDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            studyEndCalendar.set(year, month, dayOfMonth, 0, 0, 0)
            studyEndCalendar.set(Calendar.MILLISECOND, 0)
            viewModel.onStudyEndDateSelected(studyEndCalendar.timeInMillis)
        },
        studyEndCalendar.get(Calendar.YEAR),
        studyEndCalendar.get(Calendar.MONTH),
        studyEndCalendar.get(Calendar.DAY_OF_MONTH)
    )

    val dDayColor = when {
        uiState.dDayText == "D-Day"       -> MaterialTheme.colorScheme.error
        uiState.dDayText.startsWith("D+") -> MaterialTheme.colorScheme.outline
        else                               -> MaterialTheme.colorScheme.primary
    }

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text      = "시험 설정",
                style     = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color     = Color(0xFF1A1A1A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 과목명 입력 ──
            OutlinedTextField(
                value         = uiState.subjectName,
                onValueChange = { viewModel.onSubjectNameChange(it) },
                label         = { Text("과목명") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                textStyle     = TextStyle(color = Color(0xFF1A1A1A)),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedTextColor   = Color(0xFF1A1A1A),
                    unfocusedTextColor = Color(0xFF1A1A1A),
                    cursorColor        = Color(0xFF1A1A1A)
                )
            )

            // ── 시험일 선택 ──
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { examDatePickerDialog.show() }) {
                    Text(
                        text  = "시험일 선택",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (uiState.examDate != 0L) {
                    val formattedDate = remember(uiState.examDate) {
                        Instant.ofEpochMilli(uiState.examDate)
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toLocalDate()
                            .format(dateFormatter)
                    }
                    Text(
                        text  = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // ── 학습 종료일 선택 (추가) ──
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { studyEndDatePickerDialog.show() }) {
                    Text(
                        text  = "학습 종료일 선택 (선택사항)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (uiState.studyEndDate != 0L) {
                    val formattedStudyEnd = remember(uiState.studyEndDate) {
                        Instant.ofEpochMilli(uiState.studyEndDate)
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toLocalDate()
                            .format(dateFormatter)
                    }
                    Text(
                        text  = formattedStudyEnd,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.studyEndDateError != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onBackground
                    )
                }
                // 유효성 오류 메시지 표시
                uiState.studyEndDateError?.let { errorMsg ->
                    Text(
                        text  = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ── D-Day 표시 ──
            if (uiState.dDayText.isNotEmpty()) {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text       = uiState.dDayText,
                        style      = MaterialTheme.typography.displayMedium,
                        color      = dDayColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = "시험까지 ${uiState.dDayText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── 저장 버튼 ──
            Button(
                onClick  = { viewModel.saveExam() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 4.dp),
                enabled  = !uiState.isLoading &&
                        uiState.subjectName.isNotBlank() &&
                        uiState.examDate != 0L &&
                        uiState.studyEndDateError == null   // 오류 있으면 비활성화
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.height(24.dp),
                        color       = Color(0xFF1A1A1A),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "저장", color = Color(0xFF1A1A1A))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}