// app/src/main/java/com/loorve/presentation/settings/BatteryOptimizationGuideScreen.kt

package com.loorve.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning  // ✅ BatteryAlert → Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ─── 제조사 분류 ─────────────────────────────────────────────────────────────

private enum class Manufacturer { SAMSUNG, XIAOMI, LG, HUAWEI, OPPO, GENERIC }

private fun detectManufacturer(): Manufacturer {
    val m = Build.MANUFACTURER.lowercase()
    return when {
        m.contains("samsung")                                    -> Manufacturer.SAMSUNG
        m.contains("xiaomi") || m.contains("redmi")
                || m.contains("poco")                           -> Manufacturer.XIAOMI
        m.contains("lge") || m.contains("lg")                   -> Manufacturer.LG
        m.contains("huawei") || m.contains("honor")             -> Manufacturer.HUAWEI
        m.contains("oppo") || m.contains("oneplus")
                || m.contains("vivo")                           -> Manufacturer.OPPO
        else                                                     -> Manufacturer.GENERIC
    }
}

// ─── 제조사별 안내 데이터 ────────────────────────────────────────────────────

private data class BatteryGuideInfo(
    val title: String,
    val steps: List<String>
)

private fun getBatteryGuideInfo(manufacturer: Manufacturer): BatteryGuideInfo {
    return when (manufacturer) {
        Manufacturer.SAMSUNG -> BatteryGuideInfo(
            title = "Samsung 배터리 설정 경로",
            steps = listOf(
                "설정 앱 열기",
                "배터리 및 디바이스 케어 선택",
                "배터리 선택",
                "앱 배터리 사용 관리 선택",
                "Loorve 앱 찾기",
                "'제한 없음' 선택"
            )
        )
        Manufacturer.XIAOMI -> BatteryGuideInfo(
            title = "Xiaomi(MIUI) 배터리 설정 경로",
            steps = listOf(
                "설정 앱 열기",
                "배터리 및 성능 선택",
                "앱 배터리 절약 선택",
                "Loorve 앱 찾기",
                "'제한 없음' 선택"
            )
        )
        Manufacturer.LG -> BatteryGuideInfo(
            title = "LG 배터리 설정 경로",
            steps = listOf(
                "설정 앱 열기",
                "일반 > 배터리 선택",
                "배터리 사용량 선택",
                "Loorve 앱 찾기",
                "배터리 최적화 비활성화"
            )
        )
        Manufacturer.HUAWEI -> BatteryGuideInfo(
            title = "Huawei 배터리 설정 경로",
            steps = listOf(
                "설정 앱 열기",
                "배터리 선택",
                "앱 시작 관리 선택",
                "Loorve 앱 찾기",
                "수동 관리로 전환",
                "'자동 실행', '백그라운드 실행', '알림' 모두 허용"
            )
        )
        Manufacturer.OPPO -> BatteryGuideInfo(
            title = "OPPO/OnePlus/Vivo 배터리 설정 경로",
            steps = listOf(
                "설정 앱 열기",
                "배터리 > 배터리 최적화 선택",
                "Loorve 앱 찾기",
                "'최적화 안 함' 선택"
            )
        )
        Manufacturer.GENERIC -> BatteryGuideInfo(
            title = "배터리 최적화 설정 경로",
            steps = listOf(
                "설정 앱 열기",
                "배터리 선택",
                "배터리 최적화 선택",
                "모든 앱 보기 선택",
                "Loorve 앱 찾기",
                "'최적화 안 함' 선택"
            )
        )
    }
}

// ─── Intent 처리 (3단계 폴백) ─────────────────────────────────────────────────

fun launchBatteryOptimizationSettings(context: Context) {
    val packageUri = Uri.parse("package:${context.packageName}")

    fun tryStart(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) { false
        } catch (e: SecurityException) { false
        } catch (e: Exception) { false }
    }

    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = packageUri
    }
    if (tryStart(direct)) return

    val list = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    if (tryStart(list)) return

    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = packageUri
    }
    tryStart(appDetails)
}

// ─── Composable ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationGuideScreen(
    onNavigateBack: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow
        .collectAsStateWithLifecycle()

    var isIgnoring by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService<PowerManager>()
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            } else {
                true
            }
        }
    }

    val manufacturer = remember { detectManufacturer() }
    val guideInfo = remember(manufacturer) { getBatteryGuideInfo(manufacturer) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFDCEBFF),
                        Color(0xFFEDE9FF),
                        Color(0xFFF8FAFC)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 48.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "배터리 최적화 권한 설정",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "배터리 최적화",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                }
                BatteryGuideCard(
                    isIgnoring = isIgnoring,
                    guideInfo = guideInfo
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (isIgnoring) onNavigateBack()
                        else launchBatteryOptimizationSettings(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        if (isIgnoring) "확인" else "배터리 최적화 제외 허용하기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2563EB)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFE2E8F0)
                    )
                ) {
                    Text("나중에", fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── 내부 서브 Composable ────────────────────────────────────────────────────

@Composable
private fun AlreadyExemptCard(onNavigateBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✅ 이미 설정되어 있습니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "배터리 최적화 예외가 이미 등록되어 있습니다.\n" +
                        "Loorve 알림이 정상적으로 동작합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onNavigateBack,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("확인")
    }
}

@Composable
private fun GuideContent(
    guideInfo: BatteryGuideInfo,
    onLaunchSettings: () -> Unit,
    onSkip: () -> Unit
) {
    // ✅ weight() 제거 → fillMaxHeight + SpaceBetween으로 대체
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "배터리 최적화가 활성화되어 있으면\n" +
                        "복습 알림이 차단될 수 있습니다.\n" +
                        "아래 경로를 따라 예외로 등록해 주세요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = guideInfo.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    guideInfo.steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = onLaunchSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("지금 설정하기")
            }
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("나중에")
            }
        }

    }
}

@Composable
private fun BatteryGuideCard(
    isIgnoring: Boolean,
    guideInfo: BatteryGuideInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isIgnoring) {
                    "배터리 최적화 예외가 이미 등록되어 있습니다."
                } else {
                    "복습 알림을 제시간에 받으려면 배터리 최적화 권한이 필요합니다."
                },
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF475569)
            )
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Text(
                text = if (isIgnoring) {
                    "Loorve 알림이 정상적으로 동작합니다."
                } else {
                    "배터리 최적화가 활성화되어 있으면 복습 알림이 차단될 수 있습니다."
                },
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF64748B)
            )
            if (!isIgnoring) {
                Text(
                    text = "현재 기기: ${guideInfo.title}\n${guideInfo.steps.take(3).joinToString("  ·  ")}",
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}