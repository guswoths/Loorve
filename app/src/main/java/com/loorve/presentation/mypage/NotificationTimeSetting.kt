package com.loorve.presentation.mypage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTimeSettingScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationTimeSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }

    // isSaved = true 시 화면 이탈 후 즉시 이벤트 소비 (중복 네비게이션 방지)
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
            viewModel.onSavedConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "기본 알림 시간 설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

                Text(
                    text = "기본 알림 시간",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "%02d:%02d".format(Locale.KOREA, uiState.hour, uiState.minute),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "시간 선택",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "시와 분을 직접 입력하세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { showTimePicker = true }) {
                            Text("시간 변경")
                        }
                    }
                }

            Spacer(modifier = Modifier.weight(1f))

            // ── 저장 버튼 ──────────────────────────────────────────────
            Button(
                onClick = { viewModel.saveNotificationTime() },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("저장", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.hour,
            initialMinute = uiState.minute,
            is24Hour = true
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "기본 알림 시간",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    TimeInput(state = timePickerState)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("취소")
                        }
                        TextButton(
                            onClick = {
                                viewModel.onHourChanged(timePickerState.hour)
                                viewModel.onMinuteChanged(timePickerState.minute)
                                showTimePicker = false
                            }
                        ) {
                            Text("확인")
                        }
                    }
                }
            }
        }
    }
}