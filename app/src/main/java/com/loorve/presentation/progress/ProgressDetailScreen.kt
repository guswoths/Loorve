// 경로: app/src/main/java/com/loorve/presentation/progress/ProgressDetailScreen.kt
package com.loorve.presentation.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.loorve.domain.model.Progress
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner

@Composable
fun ProgressDetailScreen(
    progressId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProgressDetailViewModel = hiltViewModel()
) {
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(progressId) {
        if (uid.isNotBlank()) viewModel.loadProgress(uid, progressId)
    }

    // ── 저장 결과 Snackbar ─────────────────────────────────────
    LaunchedEffect(uiState.saveResult) {
        when (uiState.saveResult) {
            true -> {
                snackbarHostState.showSnackbar("저장되었습니다 ✅")
                viewModel.consumeSaveResult()
                // onNavigateBack() 제거 → 상세화면에서 수정 내용 바로 확인
            }
            false -> {
                snackbarHostState.showSnackbar("저장에 실패했습니다. 다시 시도해 주세요.")
                viewModel.consumeSaveResult()
            }
            null -> Unit
        }
    }

    // ── 삭제 결과 Snackbar ─────────────────────────────────────
    LaunchedEffect(uiState.deleteResult) {
        when (uiState.deleteResult) {
            true -> {
                snackbarHostState.showSnackbar("삭제되었습니다.")
                viewModel.consumeDeleteResult()
                onNavigateBack()
            }
            false -> {
                snackbarHostState.showSnackbar("삭제에 실패했습니다. 다시 시도해 주세요.")
                viewModel.consumeDeleteResult()
            }
            null -> Unit
        }
    }

    ProgressDetailContent(
        uiState           = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack    = onNavigateBack,
        onEnterEditMode   = { viewModel.enterEditMode() },
        onExitEditMode    = { viewModel.exitEditMode() },
        onSave            = { updated -> viewModel.saveProgress(uid, updated) },
        onDelete          = { viewModel.deleteProgress(uid, progressId) },
        onRetry           = { viewModel.loadProgress(uid, progressId) }
        onEditChanged     = { c, cc, tc, ic -> viewModel.onEditChanged(c, cc, tc, ic) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressDetailContent(
    uiState: ProgressDetailUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEnterEditMode: () -> Unit,
    onExitEditMode: () -> Unit,
    onSave: (Progress) -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
    onEditChanged: (String, String, String, Boolean) -> Unit
) {
    var editContent        by remember { mutableStateOf("") }
    var editCompletedCount by remember { mutableStateOf("") }
    var editTotalCount     by remember { mutableStateOf("") }
    var editIsCompleted    by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isEditMode, uiState.progress) {
        if (uiState.isEditMode) {
            uiState.progress?.let { p ->
                editContent        = p.content
                editCompletedCount = p.completedCount.toString()
                editTotalCount     = p.totalCount.toString()
                editIsCompleted    = p.isCompleted
            }
        }
    }

    // [추가] 미저장 경고 다이얼로그 상태
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // [추가] 시스템 뒤로가기 처리
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(uiState.isEditMode, uiState.isDirty) {
        val callback = object : OnBackPressedCallback(enabled = uiState.isEditMode) {
            override fun handleOnBackPressed() {
                if (uiState.isDirty) {
                    showExitConfirmDialog = true
                } else {
                    onExitEditMode()
                }

            }
        }
        backDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    // ── 미저장 경고 다이얼로그 ────────────────────────────────
    // After — 제목/내용을 사양에 맞게 수정
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title   = { Text("저장하지 않고 나가시겠어요?") },
            text    = { Text("변경 사항이 저장되지 않습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmDialog = false
                    onExitEditMode()
                }) { Text("나가기") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) { Text("계속 편집") }
            }
        )
    }

    // ── 삭제 확인 다이얼로그 ───────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("진도 삭제") },
            text    = { Text("이 항목을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "진도 편집" else "진도 상세") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isEditMode) {
                                if (uiState.isDirty) showExitConfirmDialog = true
                                else onExitEditMode()   // 변경 없으면 그냥 View 모드로
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                actions = {
                    if (!uiState.isEditMode && uiState.progress != null) {
                        IconButton(onClick = onEnterEditMode) {
                            Icon(
                                imageVector        = Icons.Filled.Edit,
                                contentDescription = "편집"
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector        = Icons.Filled.Delete,
                                contentDescription = "삭제",
                                tint               = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (uiState.isEditMode) {
                        TextButton(
                            onClick = {
                                val base = uiState.progress ?: return@TextButton
                                val completed = editCompletedCount.toIntOrNull() ?: 0
                                val total     = editTotalCount.toIntOrNull() ?: 0
                                onSave(
                                    base.copy(
                                        content        = editContent,
                                        completedCount = completed,
                                        totalCount     = total,
                                        isCompleted    = editIsCompleted
                                    )
                                )
                            }
                        ) { Text("저장") }
                    }
                }
            )
        }
    ) { paddingValues ->
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
                        Button(onClick = onRetry) { Text("다시 시도") }
                    }
                }
                uiState.progress == null -> {
                    Text(
                        text     = "데이터를 찾을 수 없습니다.",
                        modifier = Modifier.align(Alignment.Center),
                        style    = MaterialTheme.typography.bodyLarge,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    val progress = uiState.progress
                    if (uiState.isEditMode) {
                        ProgressEditBody(
                            editContent        = editContent,
                            editCompletedCount = editCompletedCount,
                            editTotalCount     = editTotalCount,
                            editIsCompleted    = editIsCompleted,
                            onContentChange        = { editContent = it; onEditChanged(it, editCompletedCount, editTotalCount, editIsCompleted) },
                            onCompletedCountChange = { if (it.all(Char::isDigit)) { editCompletedCount = it; onEditChanged(editContent, it, editTotalCount, editIsCompleted) } },
                            onTotalCountChange     = { if (it.all(Char::isDigit)) { editTotalCount = it; onEditChanged(editContent, editCompletedCount, it, editIsCompleted) } },
                            onIsCompletedChange    = { editIsCompleted = it; onEditChanged(editContent, editCompletedCount, editTotalCount, it) }
                        )
                    } else {
                        ProgressViewBody(progress = progress)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressViewBody(progress: Progress) {
    val formattedDate = remember(progress.createdAt) {
        if (progress.createdAt <= 0L) "날짜 미설정"
        else Instant.ofEpochMilli(progress.createdAt)
            .atZone(ZoneId.of("Asia/Seoul"))
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ProgressInfoCard {
            DetailRow(label = "작성일", value = formattedDate)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(label = "학습 내용", value = progress.content)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(
                label = "완료 수 / 전체 수",
                value = "${progress.completedCount} / ${progress.totalCount}"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(
                label = "완료 여부",
                value = if (progress.isCompleted) "✅ 완료" else "⏳ 진행 중"
            )
        }
    }
}

@Composable
private fun ProgressInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            content  = content
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun ProgressEditBody(
    editContent: String,
    editCompletedCount: String,
    editTotalCount: String,
    editIsCompleted: Boolean,
    onContentChange: (String) -> Unit,
    onCompletedCountChange: (String) -> Unit,
    onTotalCountChange: (String) -> Unit,
    onIsCompletedChange: (Boolean) -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value         = editContent,
            onValueChange = onContentChange,
            label         = { Text("학습 내용") },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = 3,
            maxLines      = 6
        )
        OutlinedTextField(
            value         = editCompletedCount,
            onValueChange = { if (it.all(Char::isDigit)) onCompletedCountChange(it) },
            label         = { Text("완료 수") },
            modifier      = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value         = editTotalCount,
            onValueChange = { if (it.all(Char::isDigit)) onTotalCountChange(it) },
            label         = { Text("전체 수") },
            modifier      = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "완료 여부", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = editIsCompleted, onCheckedChange = onIsCompletedChange)
        }
    }
}

@Preview(showBackground = true, name = "뷰 모드 Preview")
@Composable
private fun ProgressDetailContentViewModePreview() {
    val sampleProgress = Progress(
        progressId     = "preview-id",
        examId         = "exam-001",
        content        = "Chapter 3: Kotlin Coroutines 정리 및 Flow 학습",
        completedCount = 7,
        totalCount     = 10,
        isCompleted    = false,
        createdAt      = 1_753_884_000_000L
    )
    MaterialTheme {
        ProgressDetailContent(
            uiState           = ProgressDetailUiState(
                progress  = sampleProgress,
                isLoading = false,
                isEditMode = false
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack    = {},
            onEnterEditMode   = {},
            onExitEditMode    = {},
            onSave            = {},
            onDelete          = {},
            onRetry           = {}
        )
    }
}

@Preview(showBackground = true, name = "편집 모드 Preview")
@Composable
private fun ProgressDetailContentEditModePreview() {
    val sampleProgress = Progress(
        progressId     = "preview-id",
        examId         = "exam-001",
        content        = "Chapter 3: Kotlin Coroutines 정리",
        completedCount = 7,
        totalCount     = 10,
        isCompleted    = false,
        createdAt      = 1_753_884_000_000L
    )
    MaterialTheme {
        ProgressDetailContent(
            uiState           = ProgressDetailUiState(
                progress   = sampleProgress,
                isLoading  = false,
                isEditMode = true
            ),
            snackbarHostState = SnackbarHostState(),
            onNavigateBack    = {},
            onEnterEditMode   = {},
            onExitEditMode    = {},
            onSave            = {},
            onDelete          = {},
            onRetry           = {}
        )
    }
}
