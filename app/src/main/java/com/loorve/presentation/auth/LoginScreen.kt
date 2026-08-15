package com.loorve.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import androidx.credentials.exceptions.GetCredentialCancellationException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 로그인 성공 시 메인 화면으로 이동
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> onLoginSuccess()
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    val credentialManager = remember { CredentialManager.create(context) }

    // Google 로그인 실행 함수
    fun launchGoogleSignIn() {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // 새 계정도 허용
                    .setServerClientId(context.getString(com.loorve.R.string.default_web_client_id))
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context as Activity
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdToken = GoogleIdTokenCredential
                        .createFrom(credential.data)
                        .idToken
                    viewModel.signInWithGoogle(googleIdToken)
                } else {
                    snackbarHostState.showSnackbar("지원하지 않는 인증 방식입니다.")
                }
            } catch (e: GetCredentialException) {
                // 취소 예외: 사용자가 직접 닫은 경우 → 에러 미노출, Idle 복원
                val isCancelled = e is androidx.credentials.exceptions.GetCredentialCancellationException
                if (isCancelled) {
                    viewModel.onLoginCancelled()  // Cancelled 상태로 전환 후 Idle 복원
                } else {
                    // 실제 에러만 ViewModel에 위임 (에러 분류는 ViewModel에서)
                    viewModel.handleGoogleCredentialError(e)
                }
            } catch (e: GoogleIdTokenParsingException) {
                viewModel.handleGoogleTokenParsingError()
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is AuthUiState.Loading -> CircularProgressIndicator()
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Loorve",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { launchGoogleSignIn() },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(48.dp)
                        ) {
                            Text(text = "Google로 계속하기")
                        }
                    }
                }
            }
        }
    }
}
