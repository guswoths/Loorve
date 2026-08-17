package com.loorve.presentation.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.loorve.presentation.login.LoginScreen
import com.loorve.presentation.onboarding.OnboardingScreen
import kotlinx.coroutines.delay

// ─── 타입 안전 라우트 정의 ───────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Splash    : Screen("splash")
    object Onboarding: Screen("onboarding")
    object Login     : Screen("login")
    object Home      : Screen("home")
}

// ─── 앱 전체 네비게이션 그래프 ───────────────────────────────────────────
@Composable
fun LoorveNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        // ── 1. 스플래시 ──────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = { isLoggedIn ->
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
                    navController.navigate(Screen.Home.route) {
                        // 로그인 성공 시 스택 전체 비움 (뒤로 가기로 로그인 화면 접근 불가)
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 4. 홈 (스텁) ─────────────────────────────────────────────────
        composable(Screen.Home.route) {
            // TODO: HomeScreen composable로 교체 예정
            Text("메인 화면 (준비 중)")
        }
    }
}

// ─── 스플래시 내부 Composable ────────────────────────────────────────────
@Composable
private fun SplashScreen(
    onSplashComplete: (isLoggedIn: Boolean) -> Unit
) {
    // 확인 필요: 실제 스플래시 UI(로고 등)는 추후 디자인 확정 후 추가 권장
    var triggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2_000L) // 2초 대기
        if (!triggered) {
            triggered = true
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            onSplashComplete(isLoggedIn)
        }
    }
}
