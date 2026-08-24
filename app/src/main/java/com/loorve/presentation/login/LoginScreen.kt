// app/src/main/java/com/loorve/presentation/login/LoginScreen.kt
package com.loorve.presentation.login

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loorve.presentation.auth.AuthUiState
import com.loorve.presentation.auth.AuthViewModel
import com.loorve.ui.component.LoorveOutlineButton

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context  = LocalContext.current
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
            is AuthUiState.Cancelled -> viewModel.resetState()
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost    = { SnackbarHost(snackbarHostState) },
        containerColor  = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── 앱 로고 텍스트 ──
            Text(
                text  = "Loorve",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── USP 핵심 문구 (분리된 2줄로 시각적 강조) ──
            Text(
                text      = "시험일까지 복습 일정을\n자동으로 배치해 드립니다",
                style     = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    lineHeight = MaterialTheme.typography.titleMedium.lineHeight
                ),
                color     = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text      = "에빙하우스 망각곡선 기반 자동 복습 스케줄러",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(56.dp))

            // ── Google 로그인 버튼 ──
            // Google 브랜드 가이드라인 준수 (텍스트 유지), 전체 톤에 맞게 outline 스타일
            LoorveOutlineButton(
                text      = "Google로 계속하기",
                onClick   = { viewModel.launchGoogleSignIn(activity) },
                enabled   = !isLoading,
                isLoading = isLoading,
                modifier  = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text      = "Google 계정으로 로그인하거나 자동으로 가입됩니다.",
                style     = MaterialTheme.typography.labelMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}