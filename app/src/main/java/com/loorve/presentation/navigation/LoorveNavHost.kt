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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.loorve.ui.theme.GradientEnd
import com.loorve.ui.theme.GradientStart
import com.loorve.ui.theme.OnSurfaceVariant
import com.loorve.ui.theme.Primary
import kotlin.time.Duration.Companion.seconds
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
    object AddReviewBlock : Screen("add_review_block")
    object NotificationTimeSetting : Screen("notification_time_setting")
    object MyPage : Screen("my_page")
    object BatteryOptimizationGuide : Screen("battery_optimization_guide")
    object NotificationPermission : Screen("notification_permission")
}

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

    val gradientBrush = Brush.linearGradient(
        colors = listOf(GradientStart, GradientEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splashProgress")

    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val dotsAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotsAlpha"
    )

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

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
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

                Text(
                    text = "···",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary.copy(alpha = dotsAlpha),
                    textAlign = TextAlign.Center
                )
            }

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
                        Screen.Login.route
                    }

                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { isNewUser ->
                    val destination = if (isNewUser) {
                        Screen.Onboarding.route
                    } else {
                        Screen.Home.route
                    }

                    navController.navigate(destination) {
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
                containerColor = Background,
                bottomBar = {
                    NavigationBar(containerColor = Background) {
                        bottomNavItems.forEach { item ->
                            val isSelected = selectedTabIndex == item.index

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTabIndex = item.index },
                                icon = {
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                color = if (isSelected) {
                                                    Primary.copy(alpha = 0.1f)
                                                } else {
                                                    Color.Transparent
                                                },
                                                shape = RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) Primary else OnSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(
                                            text = item.label,
                                            color = if (isSelected) Primary else OnSurfaceVariant,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    }
                                },
                                label = null,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTabIndex) {
                        0 -> HomeScreen(
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

                        1 -> ReviewCalendarScreen(
                            onNavigateBack = { },
                            onNavigateToAddReviewBlock = {
                                navController.navigate(Screen.AddReviewBlock.route)
                            }
                        )

                        2 -> {
                            MyPageScreen(
                                onBack = {
                                    selectedTabIndex = 0
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
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddReviewBlock = {
                    navController.navigate(Screen.AddReviewBlock.route)
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
            NotificationTimeSettingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}