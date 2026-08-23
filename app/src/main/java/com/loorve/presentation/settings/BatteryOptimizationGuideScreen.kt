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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

/**
 * 배터리 최적화 예외 등록 시스템 설정으로 이동.
 *
 * 시도 순서:
 * 1. ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS — 직접 예외 요청
 * 2. ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS — 배터리 최적화 목록
 * 3. ACTION_APPLICATION_DETAILS_SETTINGS         — 앱 정보 화면 (최후 폴백)
 */
fun launchBatteryOptimizationSettings(context: Context) {
    val packageUri = Uri.parse("package:${context.packageName}")

    fun tryStart(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
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

/**
 * 배터리 최적화 예외 등록 가이드 화면.
 *
 * [변경점]
 * - isIgnoring: derivedStateOf → LaunchedEffect(lifecycleState == RESUMED) 로 교체.
 *   사용자가 시스템 설정 후 돌아오면 즉시 재확인하여 UI 갱신.
 *
 * @param onNavigateBack 상단 뒤로가기 버튼 클릭 콜백
 * @param onSkip         [나중에] 버튼 클릭 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationGuideScreen(
    onNavigateBack: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    // ── onResume 재확인 로직 ───────────────────────────────────────────────
    // lifecycleState가 RESUMED로 전환될 때마다 PowerManager를 재조회하여
    // 시스템 설정 복귀 후에도 즉시 최신 상태를 반영합니다.
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
                true // M 미만은 배터리 최적화 개념 없음 → 항상 허용 상태로 간주
            }
        }
    }
    // ─────────────────────────────────────────────────────────────────────

    val manufacturer = remember { detectManufacturer() }
    val guideInfo = remember(manufacturer) { getBatteryGuideInfo(manufacturer) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("배터리 최적화 설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            if (isIgnoring) {
                AlreadyExemptCard(onNavigateBack = onNavigateBack)
            } else {
                GuideContent(
                    guideInfo = guideInfo,
                    onLaunchSettings = { launchBatteryOptimizationSettings(context) },
                    onSkip = onSkip
                )
            }
        }
    }
}

// ─── 내부 서브 Composable ────────────────────────────────────────────────────

/** 이미 배터리 최적화 예외로 등록된 경우 표시되는 카드 */
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
    // 완료 상태에서 자동으로 뒤로 이동하는 버튼 제공
    Button(
        onClick = onNavigateBack,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("확인")
    }
}

/** 안내 문구 + 설정 이동 버튼 조합 */
@Composable
private fun GuideContent(
    guideInfo: BatteryGuideInfo,
    onLaunchSettings: () -> Unit,
    onSkip: () -> Unit
) {
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

    Spacer(modifier = Modifier.weight(1f))

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