package com.loorve.presentation.mypage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTimeSettingScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationTimeSettingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                title = { Text(text = "알림 시간 설정") },
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
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {

            // ── 시간 미리보기 ──────────────────────────────────────────
            val amPm   = if (uiState.hour < 12) "오전" else "오후"
            val hour12 = when (uiState.hour % 12) { 0 -> 12; else -> uiState.hour % 12 }
            val minuteStr = uiState.minute.toString().padStart(2, '0')

            Text(
                text  = "$amPm $hour12:$minuteStr",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // ── 시(hour) 슬라이더 (0~23) ──────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = "시간: ${uiState.hour}시",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value         = uiState.hour.toFloat(),
                    onValueChange = { viewModel.onHourChanged(it.toInt()) },
                    valueRange    = 0f..23f,
                    steps         = 22  // 0~23 = 24단계 → steps = 22(양 끝 제외)
                )
            }

            // ── 분(minute) 버튼 그룹 (0, 10, 20, 30, 40, 50) ──────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = "분",
                    style = MaterialTheme.typography.titleMedium
                )
                val minuteOptions = listOf(0, 10, 20, 30, 40, 50)
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    minuteOptions.forEach { option ->
                        val selected = uiState.minute == option
                        FilterChip(
                            selected = selected,
                            onClick  = { viewModel.onMinuteChanged(option) },
                            label    = { Text(text = "${option}분") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── 저장 버튼 ──────────────────────────────────────────────
            Button(
                onClick  = { viewModel.saveNotificationTime() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text  = "저장",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}