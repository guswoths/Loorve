// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 파일 경로:
// app/src/main/java/com/loorve/presentation/login/LoginScreen.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.loorve.presentation.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loorve.presentation.auth.AuthUiState
import com.loorve.presentation.auth.AuthViewModel
import com.loorve.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 로그인 성공 → 홈으로 이동
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> onLoginSuccess()
            is AuthUiState.Cancelled -> viewModel.resetState()
            else -> Unit
        }
    }

    // 에러 Snackbar 표시
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Error -> snackbarHostState.showSnackbar(state.message)
            is AuthUiState.NetworkError -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
    }

    // 로딩 중: 스플래시 화면 표시
    if (uiState is AuthUiState.Loading) {
        SplashLoadingScreen()
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // ── STEP 1 뱃지 ──
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary.copy(alpha = 0.15f))
                        .border(1.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "STEP 1",
                        color = Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── 앱 로고 ──
                Text(
                    text = "Loorve",
                    style = LoorveTypography.displayLarge,
                    color = Primary
                )

                Spacer(Modifier.height(8.dp))

                // ── 서브타이틀 ──
                Text(
                    text = "구글 계정으로 바로 시작",
                    style = LoorveTypography.titleMedium,
                    color = OnBackground,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                // ── 설명 텍스트 ──
                Text(
                    text = "복습 블록과 캘린더를 기기 간 동기화하려면\n로그인부터 한 번만 완료하면 됩니다.",
                    style = LoorveTypography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(40.dp))

                // ── Google 로그인 버튼 ──
                GoogleSignInButton(
                    enabled = uiState !is AuthUiState.Loading,
                    onClick = { viewModel.launchGoogleSignIn(context) }
                )

                Spacer(Modifier.height(20.dp))

                // ── 하단 안내 문구 ──
                Text(
                    text = "로그인 후 처음 사용자에게만 온보딩이 표시됩니다.",
                    style = LoorveTypography.bodySmall,
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 스플래시 로딩 화면 (앱 최초 진입 시)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun SplashLoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_anim")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 900; 1f at 300 },
            repeatMode = RepeatMode.Restart
        ), label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 900; 1f at 500 },
            repeatMode = RepeatMode.Restart
        ), label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 900; 1f at 700 },
            repeatMode = RepeatMode.Restart
        ), label = "dot3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Loorve",
                style = LoorveTypography.displayLarge,
                color = Primary
            )
            Text(
                text = "계정과 복습 블록을 불러오는 중",
                style = LoorveTypography.bodyMedium,
                color = OnSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(alpha1, alpha2, alpha3).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Google 로그인 버튼 컴포넌트
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun GoogleSignInButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val gradientBorder = Brush.linearGradient(
        colors = listOf(GradientStart, GradientEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Surface)
            .border(
                width = 1.5.dp,
                brush = gradientBorder,
                shape = RoundedCornerShape(27.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = "G",
                color = Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Google로 계속하기",
                style = LoorveTypography.bodyLarge,
                color = OnBackground,
                fontWeight = FontWeight.Medium
            )
        }
    }
}