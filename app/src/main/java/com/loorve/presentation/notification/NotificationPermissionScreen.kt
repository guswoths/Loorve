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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "알림 권한 설정",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "복습 알림을 제시간에 받으려면 알림 권한이 필요합니다. 권한을 거부해도 앱은 계속 사용할 수 있지만, 푸시 알림은 표시되지 않습니다."
        )

        when (uiState.permissionState) {
            NotificationPermissionState.GRANTED -> {
                Text("현재 알림 권한이 허용되어 있습니다.")
            }
            NotificationPermissionState.NEEDS_REQUEST -> {
                Text("알림 권한이 아직 허용되지 않았습니다. 아래 버튼으로 권한을 요청하세요.")
            }
            NotificationPermissionState.SHOW_RATIONALE -> {
                Text("이전에 알림 권한을 거부했습니다. 다시 요청하기 전에 권한이 필요한 이유를 확인해 주세요.")
            }
            NotificationPermissionState.PERMANENTLY_DENIED -> {
                Text("알림 권한이 영구 거부 상태입니다. 시스템 설정에서 직접 허용해야 합니다.")
            }
        }

        Button(
            onClick = onEnableClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("알림 권한 허용하기")
        }

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("나중에")
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