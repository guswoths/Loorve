package com.loorve.presentation.navigation

import android.os.PowerManager
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.loorve.R
import com.loorve.ui.theme.SkyBackgroundGradient
import com.loorve.presentation.auth.SplashDestination
import com.loorve.presentation.auth.SplashViewModel
import com.loorve.presentation.calendar.AddReviewBlockScreen
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
import com.loorve.ui.theme.Background
import com.loorve.ui.theme.AuroraBlue
import com.loorve.ui.theme.AuroraPink
import com.loorve.ui.theme.AuroraViolet
import com.loorve.ui.theme.OnSurfaceVariant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import com.loorve.presentation.reviewblock.ReviewBlockDetailScreen
import com.loorve.ui.component.BottomNavBar

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
    object AddReviewBlock : Screen("add_review_block")
    object NotificationTimeSetting : Screen("notification_time_setting")
    object MyPage : Screen("my_page")
    object BatteryOptimizationGuide : Screen("battery_optimization_guide")
    object NotificationPermission : Screen("notification_permission")

    object ReviewBlockDetail : Screen("reviewBlockDetail/{blockId}") {
        fun createRoute(blockId: String): String = "reviewBlockDetail/$blockId"
    }

}

private const val RETURN_TO_SETTINGS_TAB_KEY = "return_to_settings_tab"
private const val RETURN_TO_REVIEW_TAB_KEY = "return_to_review_tab"

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val index: Int
)

private val bottomNavItems = listOf(
    BottomNavItem("홈", Icons.Outlined.Home, 0),
    BottomNavItem("복습", Icons.Outlined.AutoStories, 1),
    BottomNavItem("설정", Icons.Outlined.Settings, 2)
)

@Composable
private fun SplashScreen(
    onSplashComplete: (isLoggedIn: Boolean) -> Unit
) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyBackgroundGradient)
    ) {
        SplashAmbientOrbs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.loorve_wordmark),
                    contentDescription = "Loorve",
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(260.dp)
                )
                Text(
                    text = "시험일 기반 자동 복습 스케줄러",
                    modifier = Modifier.padding(top = 12.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.225).sp
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
                val infiniteTransition = rememberInfiniteTransition(label = "splashStatus")
                val statusAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 0.45f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2800),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "splashStatusAlpha"
                )
                Text(
                    text = "계정과 복습 블록을 불러오는 중",
                    modifier = Modifier.padding(bottom = 28.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        letterSpacing = (-0.4).sp
                    ),
                    color = Color(0xE694A3B8).copy(alpha = statusAlpha),
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(4.dp)
                        .background(
                            color = Color(0x6694A3B8),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Composable
private fun SplashAmbientOrbs() {
    val infiniteTransition = rememberInfiniteTransition(label = "splashAmbient")
    val orb1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "orb1"
    )
    val orb2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "orb2"
    )
    val orb3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "orb3"
    )
    val orb4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "orb4"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val orb1Radius = 320.dp.toPx() / 2f
        val orb2Radius = 340.dp.toPx() / 2f
        val orb3Radius = 300.dp.toPx() / 2f
        val orb4Radius = 280.dp.toPx() / 2f

        fun drawOrb(
            center: Offset,
            radius: Float,
            color: Color
        ) {
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

        drawOrb(
            center = Offset(
                x = (-78 + 30 * orb1).dp.toPx() + orb1Radius,
                y = (-84 + 45 * orb1).dp.toPx() + orb1Radius
            ),
            radius = orb1Radius,
            color = Color(0x107DD3FC)
        )
        drawOrb(
            center = Offset(
                x = size.width + (85 - 40 * orb2).dp.toPx() - orb2Radius,
                y = size.height / 2f + (-12 - 30 * orb2).dp.toPx()
            ),
            radius = orb2Radius,
            color = Color(0x0E38BDF8)
        )
        drawOrb(
            center = Offset(
                x = (-45 + 35 * orb3).dp.toPx() + orb3Radius,
                y = size.height + (30 - 40 * orb3).dp.toPx() - orb3Radius
            ),
            radius = orb3Radius,
            color = Color(0x12BAE6FD)
        )
        drawOrb(
            center = Offset(
                x = size.width - 14.dp.toPx() - orb4Radius,
                y = size.height + (-68 + 35 * orb4).dp.toPx() - orb4Radius
            ),
            radius = orb4Radius,
            color = Color(0x0D7DD3FC)
        )
    }
}

@Composable
fun LoorveNavHost(
    navController: NavHostController = rememberNavController(),
    splashViewModel: SplashViewModel = hiltViewModel()
) {
    val destination by splashViewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(2.seconds)
        splashViewModel.resolveDestination()
    }

    if (destination == SplashDestination.Loading) {
        SplashScreen(onSplashComplete = {})
        return
    }

    val startDestination = when (destination) {
        SplashDestination.Home -> Screen.Home.route
        SplashDestination.Login -> Screen.Login.route
        SplashDestination.Onboarding -> Screen.Onboarding.route
        SplashDestination.Loading -> error("Loading destination must be rendered above")
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { _ ->
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    splashViewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) {
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
            var selectedTabIndex by remember { mutableStateOf(0) }
            val homeBackStackEntry = navController.currentBackStackEntry
            val returnToSettingsTab by navController.currentBackStackEntry!!
                .savedStateHandle
                .getStateFlow(RETURN_TO_SETTINGS_TAB_KEY, false)
                .collectAsStateWithLifecycle()

            LaunchedEffect(returnToSettingsTab) {
                if (returnToSettingsTab) {
                    selectedTabIndex = 2
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(RETURN_TO_SETTINGS_TAB_KEY, false)
                }
            }
            val returnToReviewTab by (
                homeBackStackEntry?.savedStateHandle
                    ?.getStateFlow(RETURN_TO_REVIEW_TAB_KEY, false)
                    ?: flowOf(false)
                ).collectAsStateWithLifecycle(false)

            LaunchedEffect(returnToReviewTab) {
                if (returnToReviewTab) {
                    selectedTabIndex = 1
                    homeBackStackEntry?.savedStateHandle?.set(RETURN_TO_REVIEW_TAB_KEY, false)
                }
            }

            // ⛔ 기능 금지 구역 — 절대 수정 금지
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

            Scaffold(
                containerColor = Color.White,
                bottomBar = {
                    BottomNavBar(
                        currentRoute = when (selectedTabIndex) {
                            1 -> "calendar"
                            2 -> "my_page"
                            else -> "home"
                        },
                        onTabSelected = { route ->
                            selectedTabIndex = when (route) {
                                "calendar" -> 1
                                "my_page" -> 2
                                else -> 0
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(innerPadding)
                ) {
                    when (selectedTabIndex) {
                        0 -> HomeScreen(
                            onNavigateToExamSetting = { navController.navigate(Screen.ExamSetting.route) },
                            onNavigateToProgressDetail = { progressId ->
                                navController.navigate(Screen.ProgressDetail.createRoute(progressId))
                            }
                        )

                        1 -> ReviewCalendarScreen(
                            onNavigateBack = { selectedTabIndex = 0 },
                            onNavigateToAddReviewBlock = {
                                homeBackStackEntry?.savedStateHandle?.set(
                                    RETURN_TO_REVIEW_TAB_KEY,
                                    true
                                )
                                navController.navigate(Screen.AddReviewBlock.route) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToReviewBlockDetail = { blockId ->
                                homeBackStackEntry?.savedStateHandle?.set(
                                    RETURN_TO_REVIEW_TAB_KEY,
                                    true
                                )
                                navController.navigate(Screen.ReviewBlockDetail.createRoute(blockId)) {
                                    launchSingleTop = true
                                }
                            }
                        )

                        2 -> {
                            MyPageScreen(
                                onBack = {
                                    selectedTabIndex = 0
                                },
                                onNavigateToNotificationTimeSetting = {
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(RETURN_TO_SETTINGS_TAB_KEY, true)
                                    navController.navigate(Screen.NotificationPermission.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToBatteryOptimization = {
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(RETURN_TO_SETTINGS_TAB_KEY, true)
                                    navController.navigate(Screen.BatteryOptimizationGuide.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToNotificationPermission = {
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(RETURN_TO_SETTINGS_TAB_KEY, true)
                                    navController.navigate(Screen.NotificationPermission.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onSignOut = {
                                    splashViewModel.resetOnboarding()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
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

            ProgressDetailScreen(
                progressId = progressId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Calendar.route) {
            ReviewCalendarScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddReviewBlock = { navController.navigate(Screen.AddReviewBlock.route) },
                onNavigateToReviewBlockDetail = { blockId ->
                    navController.navigate(Screen.ReviewBlockDetail.createRoute(blockId))
                }
            )
        }

        composable(Screen.AddReviewBlock.route) {
            AddReviewBlockScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
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
                },
                onPermissionGranted = {
                    navController.navigate(Screen.NotificationTimeSetting.route) {
                        popUpTo(Screen.NotificationPermission.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.MyPage.route) {
            MyPageScreen(
                onBack = { navController.popBackStack() },
                onNavigateToNotificationTimeSetting = {
                    navController.navigate(Screen.NotificationPermission.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToBatteryOptimization = {
                    navController.navigate(Screen.BatteryOptimizationGuide.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToNotificationPermission = {
                    navController.navigate(Screen.NotificationPermission.route) {
                        launchSingleTop = true
                    }
                },
                onSignOut = {
                    splashViewModel.resetOnboarding()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ReviewBlockDetail.route,
            arguments = listOf(
                navArgument("blockId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val blockId = backStackEntry.arguments?.getString("blockId")
                ?: return@composable

            ReviewBlockDetailScreen(
                blockId = blockId,
                block = null,  // ViewModel 내부에서 blockId로 자체 로드하도록 위임
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NotificationTimeSetting.route) {
            NotificationTimeSettingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}