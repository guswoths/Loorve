// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 파일 경로:
// app/src/main/java/com/loorve/presentation/login/LoginScreen.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.loorve.presentation.login

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.loorve.presentation.auth.AuthUiState
import com.loorve.presentation.auth.AuthViewModel
import com.loorve.ui.theme.*
import com.loorve.R

@Composable
fun LoginScreen(
    onLoginSuccess: (isNewUser: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val legacyGoogleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data == null) {
            viewModel.onLoginCancelled()
        } else {
            viewModel.completeLegacyGoogleSignIn(result.data!!)
        }
    }

    // 로그인 성공 → isNewUser 여부를 NavHost로 전달
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> onLoginSuccess(state.isNewUser)
            is AuthUiState.LegacyGoogleSignInRequired ->
                legacyGoogleSignInLauncher.launch(state.intent)
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

    // 인증 작업 중 기존 로딩 동작 유지
    if (uiState is AuthUiState.Loading) {
        SplashLoadingScreen()
        return
    }

    LoginSystemChrome()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCF9F8))
            .clip(RoundedCornerShape(0.dp))
    ) {
        LoginAmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .offset(y = (-16).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.loorve_wordmark),
                    contentDescription = "Loorve",
                    modifier = Modifier.width(240.dp)
                )
                Text(
                    text = "구글 계정으로 바로 시작",
                    modifier = Modifier.padding(top = 24.dp),
                    style = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.44).sp
                    ),
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "복습 블록과 캘린더를 기기 간 동기화하려면\n로그인부터 한 번만 완료하면 됩니다.",
                    modifier = Modifier.padding(top = 14.dp),
                    style = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.5.sp,
                        lineHeight = 23.sp
                    ),
                    color = Color(0xE647556B),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GoogleSignInButton(
                    enabled = uiState !is AuthUiState.Loading,
                    onClick = { viewModel.launchGoogleSignIn(context) }
                )
                Text(
                    text = "Google 로그인 후 온보딩에서 Loorve 사용법을 확인할 수 있습니다.",
                    modifier = Modifier.padding(top = 16.dp),
                    style = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        letterSpacing = (-0.4).sp
                    ),
                    color = Color(0xE694A3B8),
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .width(128.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x6694A3B8))
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun LoginSystemChrome() {
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = WindowCompat.getInsetsController(
            (view.context as android.app.Activity).window,
            view
        )
        controller.hide(WindowInsetsCompat.Type.statusBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }
}

@Composable
private fun LoginAmbientBackground() {
    val transition = rememberInfiniteTransition(label = "loginAmbient")
    val orb1 by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(20_000), RepeatMode.Reverse), label = "orb1"
    )
    val orb2 by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(22_000), RepeatMode.Reverse), label = "orb2"
    )
    val orb3 by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(18_000), RepeatMode.Reverse), label = "orb3"
    )
    val orb4 by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(24_000), RepeatMode.Reverse), label = "orb4"
    )

    Canvas(Modifier.fillMaxSize()) {
        fun drawOrb(center: Offset, radius: Float, color: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        val r1 = 160.dp.toPx()
        val r2 = 170.dp.toPx()
        val r3 = 150.dp.toPx()
        val r4 = 140.dp.toPx()
        drawOrb(
            Offset((-64 + 25 * orb1).dp.toPx() + r1, (-84 + 40 * orb1).dp.toPx() + r1),
            r1,
            Color(0x3D1E3A8A)
        )
        drawOrb(
            Offset(size.width + (80 - 35 * orb2).dp.toPx() - r2, size.height * .32f + (-25 * orb2).dp.toPx()),
            r2,
            Color(0x332563EB)
        )
        drawOrb(
            Offset((-45 + 30 * orb3).dp.toPx() + r3, size.height * .88f + (-35 * orb3).dp.toPx()),
            r3,
            Color(0x3838BDF8)
        )
        drawOrb(
            Offset(size.width - 14.dp.toPx() - r4, size.height + (-42 + 30 * orb4).dp.toPx() - r4),
            r4,
            Color(0x297DD3FC)
        )
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
            Image(
                painter = painterResource(R.drawable.loorve_wordmark),
                contentDescription = "Loorve",
                modifier = Modifier.width(190.dp)
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
        colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF38BDF8)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(200),
        label = "googleButtonScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(57.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .padding(1.5.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .border(
                width = 1.5.dp,
                brush = gradientBorder,
                shape = RoundedCornerShape(27.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(com.loorve.R.drawable.google_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Google로 계속하기",
                style = androidx.compose.ui.text.TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = Color(0xFF1E293B)
            )
        }
    }
}