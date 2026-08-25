package com.loorve.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.compose.*
import com.loorve.presentation.calendar.CalendarScreen
import com.loorve.presentation.exam.ExamScreen
import com.loorve.presentation.home.HomeScreen
import com.loorve.presentation.mypage.MyPageScreen
import com.loorve.presentation.onboarding.OnboardingScreen
import com.loorve.presentation.login.LoginScreen
import com.loorve.presentation.progress.ProgressDetailScreen
import com.loorve.ui.component.BottomNavBar
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: Screen.Home.route

    val bottomBarRoutes = setOf(
        Screen.Home.route, Screen.Calendar.route,
        Screen.Exam.route, Screen.MyPage.route
    )
    val showBottomBar = currentRoute in bottomBarRoutes

    // Firebase 인증 상태 확인 → 시작 경로 결정
    val startDestination = remember {
        if (FirebaseAuth.getInstance().currentUser != null) Screen.Home.route
        else Screen.Login.route
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                        slideInHorizontally(animationSpec = androidx.compose.animation.core.tween(300)) { it / 8 }
            },
            exitTransition = {
                fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
            }
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToMyPage = { navController.navigate(Screen.MyPage.route) },
                    onNavigateToExamSetting = { navController.navigate(Screen.Exam.route) },
                    onNavigateToProgressDetail = { id ->
                        navController.navigate("progress/$id")
                    }
                )
            }
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Exam.route) { ExamScreen() }
            composable(Screen.MyPage.route) {
                MyPageScreen(
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "progress/{progressId}",
                arguments = listOf(navArgument("progressId") { type = NavType.StringType })
            ) { backStack ->
                val id = backStack.arguments?.getString("progressId") ?: ""
                ProgressDetailScreen(
                    progressId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}