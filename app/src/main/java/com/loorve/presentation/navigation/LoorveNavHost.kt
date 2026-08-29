// 경로: app/src/main/java/com/loorve/presentation/navigation/LoorveNavHost.kt
package com.loorve.presentation.navigation

import android.os.PowerManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.loorve.presentation.calendar.ReviewCalendarScreen
import com.loorve.presentation.exam.ExamSettingScreen
import com.loorve.presentation.home.HomeScreen
import com.loorve.presentation.login.LoginScreen
import com.loorve.presentation.mypage.MyPageScreen
import com.loorve.presentation.mypage.NotificationTimeSettingScreen
import com.loorve.presentation.notification.NotificationPermissionRoute
import com.loorve.presentation.onboarding.OnboardingScreen
import com.loorve.presentation.progress.ProgressDetailScreen
import com.loorve.presentation.settings.BatteryOptimizationGuideScreen
import com.loorve.ui.theme.GradientEnd
import com.loorve.ui.theme.GradientStart
import com.loorve.ui.theme.OnSurfaceVariant
import com.loorve.ui.theme.Primary
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay


// ────────────────────────────────────────────────────────────────
// Screen Route 정의
// ────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Home : Screen("home")
    object ExamSetting : Screen("exam_setting")

    object ProgressDetail : Screen("progress_detail/{progressId}") {
        fun createRoute(progressId: String): String = "progress_detail/$progressId"
    }

    object Calendar : Screen("calendar")
    object NotificationTimeSetting : Screen("notification_time_setting")
    object MyPage : Screen("my_page")
    object BatteryOptimizationGuide : Screen("battery_optimization_guide")
    object NotificationPermission : Screen("notification_permission")
}

// ────────────────────────────────────────────────────────────────
// SplashScreen
// ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalTextApi::class)
@Composable
private fun SplashScreen(
    onSplashComplete: (isLoggedIn: Boolean) -> Unit
) {
    var triggered by remember { mutableStateOf(false) }

    // ⛔ 기능 금지 구역 — 절대 수정 금지
    LaunchedEffect(Unit) {
        delay(2.seconds)
        if (!triggered) {
            triggered = true
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            onSplashComplete(isLoggedIn)
        }
    }

    // 로고 그라디언트 브러시 (좌→우 linear)
    val gradientBrush = Brush.linearGradient(
        colors = listOf(GradientStart, GradientEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    // ── 애니메이션 정의 ──────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "splashProgress")

    // 1) 원형 스피너 회전각 (0 → 360, 1.2초 1회전)
    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepRotation"
    )

    // 2) 외부 트랙 링 펄스 scale (1.0 → 1.08 → 1.0)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // 3) 중앙 "..." 점 페이드 인/아웃
    val dotsAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotsAlpha"
    )

    // 배경 radial gradient 오버레이 브러시 (중앙 라벤더 빛 → 투명)
    val radialOverlayBrush = Brush.radialGradient(
        colors = listOf(
            Primary.copy(alpha = 0.06f),
            Color.Transparent
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // 은은한 라벤더 radial 오버레이 (전체 화면 중앙에서 바깥으로 퍼짐)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(radialOverlayBrush)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Loorve",
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = MaterialTheme.typography.displayLarge.fontSize,
                    brush = gradientBrush
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "시험일 기반 자동 복습 스케줄러",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── 그라디언트 원형 로딩바 (Canvas) ──────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // 외부 트랙 링 (펄스 scale 적용)
                Canvas(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                ) {
                    val strokeWidth = 8.dp.toPx()
                    val inset = strokeWidth / 2f
                    drawArc(
                        color = Primary.copy(alpha = 0.15f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 그라디언트 진행 링 (회전 스피너)
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val gradientBrushArc = Brush.sweepGradient(
                        colors = listOf(
                            GradientStart.copy(alpha = 0f),
                            GradientStart,
                            GradientEnd
                        ),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    rotate(degrees = sweepRotation, pivot = center) {
                        drawArc(
                            brush = gradientBrushArc,
                            startAngle = -90f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // 중앙 "..." 점 3개 (페이드 인/아웃)
                Text(
                    text = "···",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary.copy(alpha = dotsAlpha),
                    textAlign = TextAlign.Center
                )
            }
            // ── 그라디언트 원형 로딩바 끝 ──────────────────────────

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "계정과 복습 블록을 불러오는 중",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────
// LoorveNavHost
// ────────────────────────────────────────────────────────────────
@Composable
fun LoorveNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = { isLoggedIn ->
                    // 기존 로그인 사용자 → Home, 비로그인 → Login
                    val destination = if (isLoggedIn) {
                        Screen.Home.route
                    } else {
                        Screen.Login.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { isNewUser ->
                    // 신규 사용자(Firestore 문서 미존재) → Onboarding, 기존 사용자 → Home
                    val destination = if (isNewUser) Screen.Onboarding.route else Screen.Home.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    // 온보딩 완료 후 Home으로 이동 (뒤로 가기 시 Login으로 돌아가지 않도록 inclusive = true)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ExamSetting.route) {
            ExamSettingScreen(
                onSaveSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ExamSetting.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow
                .collectAsStateWithLifecycle()

            var batteryGuideShown by remember { mutableStateOf(false) }

            LaunchedEffect(lifecycleState) {
                if (lifecycleState == Lifecycle.State.RESUMED && !batteryGuideShown) {
                    val isIgnoringBatteryOptimizations =
                        context.getSystemService<PowerManager>()
                            ?.isIgnoringBatteryOptimizations(context.packageName)
                            ?: true

                    if (!isIgnoringBatteryOptimizations) {
                        batteryGuideShown = true
                        navController.navigate(Screen.BatteryOptimizationGuide.route)
                    }
                }
            }

            HomeScreen(
                onNavigateToMyPage = {
                    navController.navigate(Screen.MyPage.route)
                },
                onNavigateToExamSetting = {
                    navController.navigate(Screen.ExamSetting.route)
                },
                onNavigateToProgressDetail = { progressId ->
                    navController.navigate(Screen.ProgressDetail.createRoute(progressId))
                }
            )
        }

        composable(
            route = Screen.ProgressDetail.route,
            arguments = listOf(
                navArgument("progressId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val progressId = backStackEntry.arguments
                ?.getString("progressId")
                ?: return@composable

            ProgressDetailScreen(
                progressId = progressId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Calendar.route) {
            ReviewCalendarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BatteryOptimizationGuide.route) {
            BatteryOptimizationGuideScreen(
                onNavigateBack = { navController.popBackStack() },
                onSkip = { navController.popBackStack() }
            )
        }

        composable(Screen.NotificationPermission.route) {
            NotificationPermissionRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MyPage.route) {
            MyPageScreen(
                onBack = { navController.popBackStack() },
                onNavigateToNotificationTimeSetting = {
                    navController.navigate(Screen.NotificationTimeSetting.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.NotificationTimeSetting.route) {
            NotificationTimeSettingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}