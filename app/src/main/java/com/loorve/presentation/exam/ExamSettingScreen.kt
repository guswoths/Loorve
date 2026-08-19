// 경로: app/src/main/java/com/loorve/presentation/exam/ExamSettingScreen.kt
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
import androidx.compose.ui.tooling.preview.Preview
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

    // SharedFlow ONE-SHOT 이벤트 수신
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExamSettingEvent.SaveSuccess -> onSaveSuccess()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }
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

    val dDayColor = when {
        uiState.dDayText == "D-Day"          -> MaterialTheme.colorScheme.error
        uiState.dDayText.startsWith("D+")    -> MaterialTheme.colorScheme.outline
        else                                  -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "시험 설정",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1A1A1A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.subjectName,
                onValueChange = { viewModel.onSubjectNameChange(it) },
                label = { Text("과목명") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(color = Color(0xFF1A1A1A)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor   = Color(0xFF1A1A1A),
                    unfocusedTextColor = Color(0xFF1A1A1A),
                    cursorColor        = Color(0xFF1A1A1A)
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { datePickerDialog.show() }) {
                    Text(
                        text  = "시험일 선택",
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
                        text  = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (uiState.dDayText.isNotEmpty()) {
                Column(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text        = uiState.dDayText,
                        style       = MaterialTheme.typography.displayMedium,
                        color       = dDayColor,
                        fontWeight  = FontWeight.Bold
                    )
                    Text(
                        text  = "시험까지 ${uiState.dDayText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick  = { viewModel.saveExam() },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 4.dp),
                enabled  = !uiState.isLoading &&
                            uiState.subjectName.isNotBlank() &&
                            uiState.examDate != 0L
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
