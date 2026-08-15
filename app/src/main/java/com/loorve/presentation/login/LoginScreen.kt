package com.loorve.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // UI 입력 상태
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // UI 레벨 유효성 검사 상태
    val isEmailValid = email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.isEmpty() || password.length >= 6
    val isFormReady = email.isNotBlank() && password.isNotBlank() && isEmailValid && isPasswordValid

    val focusManager = LocalFocusManager.current
    val isLoading = uiState is LoginUiState.Loading

    // 상태 처리
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> onLoginSuccess()
            is AuthUiState.Cancelled -> {
                // 사용자 취소: 조용히 Idle로 복원 (에러 메시지 없음)
                viewModel.resetState()
            }
            is AuthUiState.NetworkError -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
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
                .padding(horizontal = 24.dp),
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
                text = "나만의 러브 다이어리",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(40.dp))

            // ── 이메일 입력 ──
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일") },
                singleLine = true,
                isError = !isEmailValid,
                supportingText = {
                    if (!isEmailValid) Text("올바른 이메일 형식을 입력해주세요.")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(8.dp))

            // ── 비밀번호 입력 ──
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                singleLine = true,
                isError = !isPasswordValid,
                supportingText = {
                    if (!isPasswordValid) Text("비밀번호는 6자 이상이어야 합니다.")
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                          else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isFormReady && !isLoading) {
                            // SECURITY: password is passed directly, never logged
                            viewModel.login(email, password)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ── 로그인 버튼 ──
            Button(
                onClick = {
                    focusManager.clearFocus()
                    // SECURITY: password is passed directly to ViewModel, never logged
                    viewModel.login(email, password)
                },
                enabled = isFormReady && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("로그인")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // ── 회원가입 링크 ──
            TextButton(onClick = onNavigateToSignUp) {
                Text(
                    text = "계정이 없으신가요? 회원가입",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
