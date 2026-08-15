package com.loorve.presentation.login

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loorve.presentation.auth.AuthUiState
import com.loorve.presentation.auth.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()          // ✅ AuthViewModel 하나만 사용
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // ✅ CredentialManager는 Activity Context 필요 → 안전하게 캐스팅
    val activity = context as? Activity ?: return

    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> onLoginSuccess()
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            is AuthUiState.NetworkError -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            is AuthUiState.Cancelled -> viewModel.resetState() // 취소는 조용히 처리
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── 앱 타이틀 ──
            Text(
                text = "Loorve",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "시험일까지 가장 효율적인 복습 루프를 설계하는 학습 플래너",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(56.dp))

            // ── Google 로그인 버튼 (유일한 버튼) ──
            OutlinedButton(
                onClick = {
                    // ✅ Activity Context를 직접 전달 → CredentialManager 팝업 정상 동작
                    viewModel.launchGoogleSignIn(activity)
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Google로 계속하기")   // 로그인/회원가입 통합 문구
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Google 계정으로 로그인하거나 자동으로 가입됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
