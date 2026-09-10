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
import androidx.compose.ui.window.DialogProperties
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatDisplayTime(uiState.hour, uiState.minute),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "복습 알림이 울릴 시간을 선택하세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("시간 변경")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
            is24Hour = false
        )

        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatPickerTime(timePickerState.hour, timePickerState.minute),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            periodSelectorSelectedContentColor = MaterialTheme.colorScheme.primary,
                            periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
                            Text("완료")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDisplayTime(hour: Int, minute: Int): String =
    "%02d:%02d".format(Locale.KOREA, hour, minute)

private fun formatPickerTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "오전" else "오후"
    val hour12 = when (val normalizedHour = hour % 12) {
        0 -> 12
        else -> normalizedHour
    }
    return "%s %d:%02d".format(Locale.KOREA, period, hour12, minute)
}