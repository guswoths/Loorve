package com.loorve.presentation.exam

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * 시험 설정 화면
 *
 * @param onSaveSuccess 저장 성공 시 호출되는 콜백
 * @param viewModel HiltViewModel (기본값 주입)
 */
@Composable
fun ExamSettingScreen(
    onSaveSuccess: () -> Unit,
    viewModel: ExamSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ── 저장 성공 처리 ──────────────────────────────────────────────────
    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            onSaveSuccess()
        }
    }

    // ── 에러 Snackbar 처리 ──────────────────────────────────────────────
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // ── 날짜 포맷터 ─────────────────────────────────────────────────────
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
    }

    // ── DatePickerDialog 팩토리 ──────────────────────────────────────────
    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                viewModel.onExamDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // ── D-day 텍스트 색상 결정 ───────────────────────────────────────────
    val dDayColor = when {
        uiState.dDayText == "D-Day" -> MaterialTheme.colorScheme.error
        uiState.dDayText.startsWith("D+") -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ① 타이틀
            Text(
                text = "시험 설정",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ② 과목명 입력
            OutlinedTextField(
                value = uiState.subjectName,
                onValueChange = { viewModel.onSubjectNameChange(it) },
                label = { Text("과목명") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ③ 시험일 선택
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { datePickerDialog.show() }) {
                    Text(
                        text = "시험일 선택",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (uiState.examDate != 0L) {
                    val formattedDate = remember(uiState.examDate) {
                        Instant.ofEpochMilli(uiState.examDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(dateFormatter)
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // ④ D-day 표시 영역
            if (uiState.dDayText.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = uiState.dDayText,
                        style = MaterialTheme.typography.displayMedium,
                        color = dDayColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "시험까지 ${uiState.dDayText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ⑤ 저장 버튼
            Button(
                onClick = { viewModel.saveExam() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 4.dp),
                enabled = !uiState.isLoading &&
                        uiState.subjectName.isNotBlank() &&
                        uiState.examDate != 0L
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "저장")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ⑥ Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(showBackground = true, name = "시험 설정 화면 미리보기")
@Composable
private fun ExamSettingScreenPreview() {
    MaterialTheme {
        // Preview용 더미 상태 (ViewModel 없이 UI 구조만 확인)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "시험 설정",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                OutlinedTextField(
                    value = "수능 국어",
                    onValueChange = {},
                    label = { Text("과목명") },
                    modifier = Modifier.fillMaxWidth()
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "D-30",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "시험까지 D-30",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
