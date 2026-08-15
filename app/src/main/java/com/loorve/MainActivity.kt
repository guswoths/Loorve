package com.loorve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.loorve.presentation.login.LoginScreen
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
                    startDestination = "login"
                ) {
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
