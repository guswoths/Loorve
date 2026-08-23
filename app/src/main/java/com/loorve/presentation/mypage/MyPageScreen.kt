package com.loorve.presentation.mypage

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loorve.presentation.auth.AuthUiState
import com.loorve.presentation.auth.AuthViewModel

// ─── MyPage UiState ────────────────────────────────────────────────────────
data class MyPageUiState(
    val displayName: String = "",
    val email: String = "",
    val isLoggingOut: Boolean = false,
    val logoutComplete: Boolean = false,
    val errorMessage: String? = null
)

// ─── MyPageScreen ──────────────────────────────────────────────────────────
/**
 * 마이페이지 허브 화면
 *
 * - 계정 정보 표시 (domain User 모델 경유, FirebaseAuth 직접 참조 금지)
 * - 로그아웃: 확인 Dialog → signOut() → Login 화면으로 백스택 전체 클리어
 * - 알림 시간 설정으로 이동
 *
 * @param onNavigateBack                  상단 뒤로가기 버튼
 * @param onNavigateToNotificationTimeSetting  알림 시간 설정 화면 이동
 * @param onLogoutComplete                로그아웃 완료 후 Login 화면으로 이동 (백스택 클리어는 NavHost에서)
 * @param authViewModel                   AuthViewModel (Hilt 주입)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNotificationTimeSetting: () -> Unit,
    onLogoutComplete: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    // 로그아웃 완료 감지 → 네비게이션 처리 후 상태 초기화
    LaunchedEffect(authUiState) {
        if (authUiState is AuthUiState.LogoutComplete) {
            authViewModel.resetState()
            onLogoutComplete()
        }
    }

    // 로그아웃 확인 Dialog 표시 여부
    var showLogoutDialog by remember { mutableStateOf(false) }

    // 로그아웃 에러 Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = (authUiState as? AuthUiState.Error)?.message
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(
                message     = errorMessage,
                duration    = SnackbarDuration.Long,
                withDismissAction = true
            )
            authViewModel.resetState()
        }
    }

    // 현재 로그인 사용자 정보를 domain User 경유로 가져옴
    // AuthUiState.Success에서 user를 읽거나, getCurrentUser() Flow를 사용
    // 여기서는 AuthRepository.getCurrentUser()를 MyPageViewModel로 래핑하는 것이 이상적이나,
    // AuthViewModel이 이미 authRepository를 보유 중이므로 별도 ViewModel 없이
    // collectAsStateWithLifecycle로 Flow를 직접 수집 (단일 책임 유지)
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                authViewModel.signOut()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("마이페이지") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        val isLoading = authUiState is AuthUiState.Loading

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── 계정 정보 섹션 ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier           = Modifier.size(56.dp),
                        tint               = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text  = currentUser?.nickname ?: "로딩 중...",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text  = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 설정 메뉴 ─────────────────────────────────────────────
            Text(
                text     = "설정",
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // 알림 시간 설정
            MyPageMenuItem(
                icon    = Icons.Default.Notifications,
                label   = "알림 시간 설정",
                onClick = onNavigateToNotificationTimeSetting
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── 로그아웃 버튼 ─────────────────────────────────────────
            OutlinedButton(
                onClick  = { showLogoutDialog = true },
                enabled  = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("로그아웃", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ─── 로그아웃 확인 Dialog ──────────────────────────────────────────────────
@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("로그아웃") },
        text    = { Text("로그아웃 하시겠습니까?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text  = "로그아웃",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// ─── 메뉴 아이템 공통 컴포넌트 ────────────────────────────────────────────
@Composable
private fun MyPageMenuItem(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    label   : String,
    onClick : () -> Unit
) {
    Surface(
        onClick = onClick,
        shape   = MaterialTheme.shapes.medium,
        color   = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text     = label,
                style    = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}