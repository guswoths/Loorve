// app/src/main/java/com/loorve/presentation/mypage/MyPageScreen.kt
// 상단 import에 아래가 있는지 확인 (별도 파일로 분리되었으므로 같은 패키지라 import 불필요)
// ViewModel, UiState, Event 모두 같은 패키지(mypage)이므로 추가 import 없이 동작
package com.loorve.presentation.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.loorve.ui.component.*
import com.loorve.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    onBack: () -> Unit,
    onNavigateToNotificationTimeSetting: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val user = FirebaseAuth.getInstance().currentUser
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MyPageEvent.SignOutSuccess -> onSignOut()
                is MyPageEvent.DeleteAccountSuccess -> onSignOut()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SETTINGS",
                            style = LoorveTypography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "설정",
                            style = LoorveTypography.titleLarge,
                            color = OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            item {
                LoorveCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(
                            text = user?.email ?: "guswoths@gmail.com",
                            style = LoorveTypography.bodyLarge,
                            color = OnBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "동기화가 켜져 있어 다른 기기에서도 같은 복습 블록을 이어서 볼 수 있습니다.",
                            style = LoorveTypography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            item {
                LoorveCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            title = "복습 알림",
                            subtitle = "저녁 8시에 오늘 블록 알림",
                            actionLabel = "변경",
                            onAction = onNavigateToNotificationTimeSetting
                        )
                        HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                        SettingsRow(
                            icon = Icons.Default.Tune,
                            title = "기본 주기",
                            subtitle = "에빙하우스 망각주기",
                            actionLabel = "변경",
                            onAction = { }
                        )
                    }
                }
            }

            item {
                LoorveCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        icon = Icons.Default.BatteryFull,
                        title = "배터리 최적화 안내",
                        subtitle = "알림 누락 방지를 위한 기기 설정",
                        actionLabel = "보기",
                        onAction = { }
                    )
                }
            }

            item {
                LoorveCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "로그아웃",
                        subtitle = "이 기기에서 계정 연결 해제",
                        actionLabel = "실행",
                        actionColor = Error,
                        onAction = { viewModel.signOut() }
                    )
                }
            }

            item {
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "계정 삭제",
                        color = Error.copy(alpha = 0.7f),
                        style = LoorveTypography.bodyMedium
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isLoading) showDeleteDialog = false },
            containerColor = Surface,
            title = { Text("계정 삭제", color = OnBackground) },
            text = { Text("정말 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.", color = OnSurface) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    },
                    enabled = !uiState.isLoading
                ) { Text("삭제", color = Error) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !uiState.isLoading
                ) {
                    Text("취소", color = OnSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    actionColor: Color = Primary,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LoorveTypography.bodyLarge,
                color = OnBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = LoorveTypography.bodySmall,
                color = OnSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(
                    color = actionColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onAction() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = actionLabel,
                style = LoorveTypography.labelMedium,
                color = actionColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}