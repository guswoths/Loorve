package com.loorve.presentation.navigation

import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.delay

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

@OptIn(ExperimentalTextApi::class)
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

    val gradientBrush = Brush.linearGradient(
        colors = listOf(GradientStart, GradientEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splashProgress")
    val progressAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progressAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
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

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Primary,
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round,
                trackColor = Color.Transparent
            )

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
                    val destination = if (isLoggedIn) {
                        Screen.Home.route
                    } else {
                        Screen.Onboarding.route
                    }

                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ExamSetting.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.ExamSetting.route) {
            ExamSettingScreen(
                onSaveSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ExamSetting.route) {
                            inclusive = true
                        }
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
                if (
                    lifecycleState == Lifecycle.State.RESUMED &&
                    !batteryGuideShown
                ) {
                    val isIgnoringBatteryOptimizations =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.getSystemService<PowerManager>()
                                ?.isIgnoringBatteryOptimizations(context.packageName)
                                ?: true
                        } else {
                            true
                        }

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
                    navController.navigate(
                        Screen.ProgressDetail.createRoute(progressId)
                    )
                }
            )
        }

        composable(
            route = Screen.ProgressDetail.route,
            arguments = listOf(
                navArgument("progressId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val progressId = backStackEntry.arguments
                ?.getString("progressId")
                ?: return@composable

            // ✅ ProgressDetailScreen의 파라미터명은 onBack
            ProgressDetailScreen(
                progressId = progressId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Calendar.route) {
            ReviewCalendarScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.BatteryOptimizationGuide.route) {
            BatteryOptimizationGuideScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSkip = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.NotificationPermission.route) {
            NotificationPermissionRoute(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.MyPage.route) {
            MyPageScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToNotificationTimeSetting = {
                    navController.navigate(Screen.NotificationTimeSetting.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.NotificationTimeSetting.route) {
            // ✅ 핵심 수정: onBack → onNavigateBack (실제 함수 시그니처에 맞춤)
            NotificationTimeSettingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}