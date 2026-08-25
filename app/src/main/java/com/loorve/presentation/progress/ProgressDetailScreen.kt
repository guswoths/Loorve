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
import com.loorve.ui.component.*
import com.loorve.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDetailScreen(
    progressId: String,
    onBack: () -> Unit,
    viewModel: ProgressDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(progressId) { viewModel.load(progressId) }
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.progress?.subjectName ?: "",
                        style = LoorveTypography.titleMedium,
                        color = OnBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                LoorveCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = progress.content,
                            style = LoorveTypography.bodyLarge,
                            color = OnBackground
                        )
                        LoorveProgressBar(
                            completed = progress.completed,
                            total = progress.total,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = progress.dateFormatted,
                            style = LoorveTypography.labelMedium,
                            color = OnSurfaceVariant
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (progress.isCompleted) Success.copy(0.15f) else Tertiary.copy(0.15f)
                        ) {
                            Text(
                                text = if (progress.isCompleted) "완료" else "진행 중",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = LoorveTypography.labelMedium,
                                color = if (progress.isCompleted) Success else Tertiary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEditSheet = true },
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
                    viewModel.delete(progressId)
                    onBack()
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