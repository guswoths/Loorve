package com.loorve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.loorve.presentation.login.LoginScreen
import com.loorve.presentation.onboarding.OnboardingScreen  // ← 신규 import 추가
import com.loorve.ui.theme.LoorveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoorveTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "onboarding"  // ← "login"에서 "onboarding"으로 변경
                ) {
                    // 온보딩 라우트 (신규 추가)
                    composable("onboarding") {
                        OnboardingScreen(
                            onOnboardingComplete = {
                                navController.navigate("login") {
                                    // 온보딩 완료 후 백스택에서 onboarding 제거
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                            // ✅ onNavigateToSignUp 파라미터 제거됨 (Google 로그인 통합)
                        )
                    }
                    composable("home") {
                        androidx.compose.material3.Text("메인 화면 (준비 중)")
                    }
                }
            }
        }
    }
}
