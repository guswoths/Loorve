package com.loorve.presentation.notification

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.loorve.ui.theme.SkyBackgroundGradient
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@Composable
fun NotificationPermissionRoute(
    onNavigateBack: () -> Unit,
    onPermissionGranted: () -> Unit = {},
    viewModel: NotificationPermissionViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val shouldShowRationale = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null
        ) {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }

        viewModel.onPermissionResult(
            isGranted = isGranted,
            shouldShowRationale = shouldShowRationale,
            hasRequestedBefore = true
        )
        if (isGranted) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        val shouldShowRationale = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null
        ) {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }

        viewModel.refreshPermissionState(
            shouldShowRationale = shouldShowRationale,
            hasRequestedBefore = false
        )
        if (NotificationPermissionViewModel.hasNotificationPermission(context)) {
            onPermissionGranted()
        }
    }

    NotificationPermissionScreen(
        uiState = uiState,
        onEnableClick = {
            val shouldShowRationale = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null
            ) {
                activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                false
            }

            when (
                viewModel.onEnableNotificationClicked(
                    shouldShowRationale = shouldShowRationale,
                    hasRequestedBefore = uiState.hasRequestedBefore
                )
            ) {
                NotificationPermissionAction.LaunchSystemPermissionRequest -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                NotificationPermissionAction.None -> Unit
            }
        },
        onConfirmRationale = {
            when (viewModel.onConfirmRationaleRequest()) {
                NotificationPermissionAction.LaunchSystemPermissionRequest -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                NotificationPermissionAction.None -> Unit
            }
        },
        onDismissRationale = viewModel::onDismissRationaleDialog,
        onOpenSettings = { openAppNotificationSettings(context) },
        onDismissSettings = viewModel::onDismissSettingsDialog,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun NotificationPermissionScreen(
    uiState: NotificationPermissionUiState,
    onEnableClick: () -> Unit,
    onConfirmRationale: () -> Unit,
    onDismissRationale: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val statusMessage = when (uiState.permissionState) {
        NotificationPermissionState.GRANTED ->
            "현재 알림 권한이 허용되어 있습니다."
        NotificationPermissionState.NEEDS_REQUEST ->
            "알림 권한이 아직 허용되지 않았습니다. 아래 버튼으로 권한을 요청하세요."
        NotificationPermissionState.SHOW_RATIONALE ->
            "이전에 알림 권한을 거부했습니다. 다시 요청하기 전에 권한이 필요한 이유를 확인해 주세요."
        NotificationPermissionState.PERMANENTLY_DENIED ->
            "알림 권한이 영구 거부 상태입니다. 시스템 설정에서 직접 허용해야 합니다."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 54.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "알림",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "알림 권한 설정",
                modifier = Modifier.padding(top = 18.dp),
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "복습 알림을 제시간에 받으려면 알림 권한이 필요합니다.\n권한을 거부해도 앱은 계속 사용할 수 있지만, 푸시 알림은\n표시되지 않습니다.",
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = statusMessage,
                modifier = Modifier.padding(top = 22.dp),
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = Color(0xFF64748B)
            )

            Button(
                onClick = onEnableClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("알림 권한 허용하기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF2563EB)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFE2E8F0)
                )
            ) {
                Text("나중에", fontSize = 12.sp)
            }
        }
    }

    if (uiState.showRationaleDialog) {
        AlertDialog(
            onDismissRequest = onDismissRationale,
            title = { Text("알림 권한이 필요합니다") },
            text = {
                Text(
                    "복습 일정 시간에 맞춰 알림을 보여주려면 알림 권한이 필요합니다. 권한을 허용하지 않으면 앱은 계속 동작하지만 알림은 표시되지 않을 수 있습니다."
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmRationale) {
                    Text("다시 요청")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRationale) {
                    Text("나중에")
                }
            }
        )
    }

    if (uiState.showSettingsDialog) {
        AlertDialog(
            onDismissRequest = onDismissSettings,
            title = { Text("설정에서 권한 허용 필요") },
            text = {
                Text(
                    "알림 권한이 더 이상 시스템 팝업으로 표시되지 않습니다. 앱 설정 화면에서 알림 권한을 직접 허용해 주세요."
                )
            },
            confirmButton = {
                TextButton(onClick = onOpenSettings) {
                    Text("설정 열기")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSettings) {
                    Text("닫기")
                }
            }
        )
    }
}