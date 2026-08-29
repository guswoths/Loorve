// 경로: app/src/main/java/com/loorve/presentation/progress/ProgressDetailScreen.kt
// 전체 파일 — onBack → onNavigateBack 으로 변경

package com.loorve.presentation.progress

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.loorve.ui.component.*
import com.loorve.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDetailScreen(
    progressId: String,
    onNavigateBack: () -> Unit,    // onBack → onNavigateBack 으로 통일
    viewModel: ProgressDetailViewModel = hiltViewModel()
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    LaunchedEffect(progressId) { viewModel.loadProgress(uid, progressId) }
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.progress?.content ?: "",
                        style = LoorveTypography.titleMedium,
                        color = OnBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {  // onBack → onNavigateBack
                        Icon(Icons.Default.ArrowBack, null, tint = OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.progress?.let { progress ->
                val isCompleted = progress.completedCount > 0 && progress.completedCount >= progress.totalCount
                LoorveCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = progress.content,
                            style = LoorveTypography.bodyLarge,
                            color = OnBackground
                        )
                        LoorveProgressBar(
                            completed = progress.completedCount,
                            total = progress.totalCount,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = run {
                                try {
                                    val date = java.time.Instant.ofEpochMilli(progress.createdAt)
                                        .atZone(java.time.ZoneId.of("Asia/Seoul"))
                                        .toLocalDate()
                                    date.format(java.time.format.DateTimeFormatter.ofPattern("M월 d일"))
                                } catch (e: Exception) { "" }
                            },
                            style = LoorveTypography.labelMedium,
                            color = OnSurfaceVariant
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isCompleted) Success.copy(0.15f) else Tertiary.copy(0.15f)
                        ) {
                            Text(
                                text = if (isCompleted) "완료" else "진행 중",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = LoorveTypography.labelMedium,
                                color = if (isCompleted) Success else Tertiary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* 수정 기능 추후 구현 */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
                    ) {
                        Text("수정", color = Primary)
                    }
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("삭제", color = Error)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Surface,
            title = { Text("진도 삭제", color = OnBackground) },
            text = { Text("이 학습 기록을 삭제하시겠습니까?", color = OnSurface) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProgress(uid, progressId)
                    onNavigateBack()  // onBack() → onNavigateBack()
                }) { Text("삭제", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = OnSurfaceVariant)
                }
            }
        )
    }
}