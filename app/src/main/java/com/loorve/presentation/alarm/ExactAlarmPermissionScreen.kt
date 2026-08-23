package com.loorve.presentation.alarm

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

/**
 * 정확한 알람 권한 요청 화면 (AlertDialog 형태).
 *
 * 사용 시점: 알람 등록 시도 시 [ExactAlarmPermissionState.DENIED] 상태면 표시.
 *
 * @param onGoToSettings [설정으로 이동] 버튼 클릭 → ACTION_REQUEST_SCHEDULE_EXACT_ALARM
 * @param onUseFallback  [나중에] 버튼 클릭 → Fallback(비정확 알람)으로 진행
 * @param onDismiss      다이얼로그 외부 클릭 시 처리
 */
@Composable
fun ExactAlarmPermissionDialog(
    onGoToSettings: () -> Unit,
    onUseFallback: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "정확한 알람 권한이 필요합니다",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = "복습 알림을 정확한 시간에 받으려면\n" +
                        "시스템 설정에서 '정확한 알람 예약' 권한을\n" +
                        "허용해 주세요.\n\n" +
                        "'나중에'를 선택하면 알림이 다소 늦게\n" +
                        "도착할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("설정으로 이동")
            }
        },
        dismissButton = {
            TextButton(onClick = onUseFallback) {
                Text("나중에")
            }
        }
    )
}

/**
 * [설정으로 이동] 버튼 클릭 시 시스템 정확한 알람 설정 화면으로 이동하는 Intent 처리.
 * Activity Context에서 호출해야 합니다.
 *
 * API 31+: ACTION_REQUEST_SCHEDULE_EXACT_ALARM (패키지 URI 필수)
 * API 30 이하: 도달 불가 (권한 불필요) - 방어 코드 포함
 */
fun launchExactAlarmPermissionSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 일부 제조사에서 해당 인텐트 미지원 시 앱 설정으로 폴백
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }
}