package com.loorve.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.loorve.presentation.calendar.ReviewCalendarScreen   // ← 추가
import com.loorve.presentation.exam.ExamSettingScreen
import com.loorve.presentation.home.HomeScreen
import com.loorve.presentation.login.LoginScreen
import com.loorve.presentation.onboarding.OnboardingScreen
import com.loorve.presentation.progress.ProgressDetailScreen
import kotlinx.coroutines.delay

// ─── 타입 안전 라우트 정의 ───────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Splash         : Screen("splash")
    object Onboarding     : Screen("onboarding")
    object Login          : Screen("login")
    object Home           : Screen("home")
    object ExamSetting    : Screen("exam_setting")
    object ProgressDetail : Screen("progress_detail/{progressId}") {
        fun createRoute(progressId: String) = "progress_detail/$progressId"
    }
    object Calendar       : Screen("calendar")   // ← 작업 1: 라우트 추가
}

// ─── 스플래시 Composable ────────────────────────────────────────────────
@Composable
private fun SplashScreen(
    onSplashComplete: (isLoggedIn: Boolean) -> Unit
) {
    var triggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2_000L)
        if (!triggered) {
            triggered = true
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            onSplashComplete(isLoggedIn)
        }
    }

    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "Loorve",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }
}

// ─── 앱 전체 네비게이션 그래프 ───────────────────────────────────────────
@Composable
fun LoorveNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route
    ) {

        // ── 1. 스플래시 ──────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = { isLoggedIn: Boolean ->
                    val destination = if (isLoggedIn) Screen.Home.route
                    else Screen.Onboarding.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 2. 온보딩 ────────────────────────────────────────────────────
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 3. 로그인 ────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ExamSetting.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 4. 시험 설정 ─────────────────────────────────────────────────
        composable(Screen.ExamSetting.route) {
            ExamSettingScreen(
                onSaveSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ExamSetting.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 5. 홈 ────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProgressDetail = { progressId ->
                    navController.navigate(Screen.ProgressDetail.createRoute(progressId))
                },
                onNavigateToCalendar = {               // ← 작업 3: 콜백 연결
                    navController.navigate(Screen.Calendar.route)
                }
            )
        }

        // ── 6. 진도 상세 ─────────────────────────────────────────────────
        composable(
            route     = Screen.ProgressDetail.route,
            arguments = listOf(navArgument("progressId") { type = NavType.StringType })
        ) { backStackEntry ->
            val progressId = backStackEntry.arguments?.getString("progressId") ?: return@composable
            ProgressDetailScreen(
                progressId           = progressId,
                onNavigateBack       = { navController.popBackStack() },
                onNavigateToCalendar = {               // ← 작업 4: 캘린더 콜백 연결
                    navController.navigate(Screen.Calendar.route)
                }
            )
        }

        // ── 7. 복습 캘린더 ───────────────────────────────────────────────
        composable(Screen.Calendar.route) {            // ← 작업 1: 캘린더 composable 등록
            ReviewCalendarScreen(
                onNavigateBack = { navController.popBackStack() }  // ← 작업 5: 뒤로가기 전달
            )
        }
    }
}