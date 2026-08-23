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

// ─────────────────────────────────────────────────────────────────────────────
// 네비게이션 연동 준비 (LoorveNavHost.kt → Screen sealed class에 아래 추가)
// object BatteryOptimizationGuide : Screen("battery_optimization_guide")
// ─────────────────────────────────────────────────────────────────────────────

// ─── 제조사 분류 ────────────────────────────────────────────────────────────

/**
 * [Build.MANUFACTURER]를 소문자로 정규화하여 지원 제조사로 분류.
 * 감지 불가 기기는 [Manufacturer.GENERIC]으로 폴백.
 */
private enum class Manufacturer { SAMSUNG, XIAOMI, LG, GENERIC }

private fun detectManufacturer(): Manufacturer {
    return when {
        Build.MANUFACTURER.lowercase().contains("samsung") -> Manufacturer.SAMSUNG
        Build.MANUFACTURER.lowercase().contains("xiaomi") ||
                Build.MANUFACTURER.lowercase().contains("redmi") ||
                Build.MANUFACTURER.lowercase().contains("poco")   -> Manufacturer.XIAOMI
        Build.MANUFACTURER.lowercase().contains("lge") ||
                Build.MANUFACTURER.lowercase().contains("lg")     -> Manufacturer.LG
        else -> Manufacturer.GENERIC
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
 * 1. [Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS] — 직접 예외 요청
 *    ※ AndroidManifest.xml에 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 권한 선언 필요
 * 2. [Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS] — 배터리 최적화 목록
 * 3. [Settings.ACTION_APPLICATION_DETAILS_SETTINGS] — 앱 정보 화면 (최후 폴백)
 *
 * ActivityNotFoundException 및 SecurityException을 모두 포착하여
 * 제조사별 미지원 케이스에 안전하게 대응합니다.
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
            // REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 권한 미선언 시 발생 가능
            false
        } catch (e: Exception) {
            false
        }
    }

    // 1순위: 직접 예외 요청 (패키지 URI 필수)
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = packageUri
    }
    if (tryStart(direct)) return

    // 2순위: 배터리 최적화 목록
    val list = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    if (tryStart(list)) return

    // 3순위: 앱 정보 화면
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = packageUri
    }
    tryStart(appDetails)
}

// ─── Composable ──────────────────────────────────────────────────────────────

/**
 * 배터리 최적화 예외 등록 가이드 화면.
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

    // 현재 배터리 최적화 예외 등록 여부 감지
    val isIgnoring by remember {
        derivedStateOf {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService<PowerManager>()
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            } else {
                true // M 미만은 최적화 예외 개념 없음 → 항상 허용 상태로 간주
            }
        }
    }

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
            // 아이콘
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            // 이미 예외 등록된 경우 완료 상태 표시
            if (isIgnoring) {
                AlreadyExemptCard()
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
private fun AlreadyExemptCard() {
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
}

/** 안내 문구 + 설정 이동 버튼 조합 */
@Composable
private fun GuideContent(
    guideInfo: BatteryGuideInfo,
    onLaunchSettings: () -> Unit,
    onSkip: () -> Unit
) {
    // 안내 설명
    Text(
        text = "배터리 최적화가 활성화되어 있으면\n" +
                "복습 알림이 차단될 수 있습니다.\n" +
                "아래 경로를 따라 예외로 등록해 주세요.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // 제조사별 경로 카드
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

    // [지금 설정하기] 버튼 (Primary)
    Button(
        onClick = onLaunchSettings,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("지금 설정하기")
    }

    // [나중에] 버튼 (Text/Secondary)
    TextButton(
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("나중에")
    }
}