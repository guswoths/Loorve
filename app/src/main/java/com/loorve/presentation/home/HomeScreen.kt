// 경로: app/src/main/java/com/loorve/presentation/home/HomeScreen.kt
package com.loorve.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월 dd일") }

    Scaffold { paddingValues ->
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
                        Button(onClick = { viewModel.loadExams() }) {
                            Text("다시 시도")
                        }
                    }
                }

                uiState.exams.isEmpty() -> {
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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

                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text       = "내 시험 목록",
                                style      = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier   = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(uiState.exams) { exam ->
                            ExamListItem(
                                subjectName = exam.subjectName,
                                examDate    = exam.examDate,
                                formatter   = dateFormatter
                            )
                        }
                    }
                }
            }
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
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                text       = subjectName,
                style      = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text  = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
