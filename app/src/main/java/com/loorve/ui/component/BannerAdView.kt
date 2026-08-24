// 경로: app/src/main/java/com/loorve/ui/component/BannerAdView.kt
package com.loorve.ui.component

import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.loorve.BuildConfig

/**
 * BannerAdView — AdMob 배너 광고 Compose 래퍼
 *
 * ──────────────────────────────────────────────
 * [방어 설계 원칙 - Fail-Safe Banner]
 * ──────────────────────────────────────────────
 * 1. 광고 로드 실패(onAdFailedToLoad) 시:
 *    - adFailed = true 로 상태 전환
 *    - Compose 트리에서 AndroidView 자체를 제거 (if (!adFailed) 블록)
 *    - View.GONE 처리로 Android View 레벨에서도 공간 확보 없음
 *    → 결과: 레이아웃에 빈 공간이 생기지 않음
 *
 * 2. Spacer / Box로 자리를 고정하지 않음:
 *    - 광고 실패 시 해당 LazyColumn item의 높이가 자연스럽게 0이 됨
 *    - ProgressInputSection, 시험 목록, 진도 기록 등 핵심 UI 배치에 영향 없음
 *
 * 3. AdMob 초기화 실패 시에도 크래시 없음:
 *    - LoorveApplication의 try-catch가 초기화 오류를 처리
 *    - loadAd() 자체가 실패해도 onAdFailedToLoad 콜백이 안전하게 처리
 *
 * ──────────────────────────────────────────────
 * [광고 ID 관리 원칙]
 * ──────────────────────────────────────────────
 * - 기본값: BuildConfig.ADMOB_BANNER_UNIT_ID (local.properties에서 주입)
 * - local.properties에 ADMOB_BANNER_UNIT_ID 미설정 시 → 테스트 ID 자동 폴백
 *   (app/build.gradle.kts의 buildConfigField 폴백 처리 참조)
 * - 하드코딩 금지: 이 파일에 광고 ID를 직접 작성하지 말 것
 *
 * @param modifier   외부에서 전달하는 Modifier (기본값: 빈 Modifier)
 * @param adUnitId   광고 단위 ID. 테스트 시 BuildConfig.ADMOB_BANNER_UNIT_ID 사용.
 *                   배포 시 local.properties에 실제 ID를 주입하면 자동 반영됨.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_UNIT_ID
) {
    // ── 상태 정의 ────────────────────────────────────────────────────────────
    // adFailed: true이면 AndroidView를 Compose 트리에서 완전히 제거
    var adFailed by remember { mutableStateOf(false) }

    // ── 렌더링 조건 ──────────────────────────────────────────────────────────
    // adFailed = true 이면 아무것도 렌더링하지 않음
    // → LazyColumn item의 높이가 0이 되어 핵심 UI 배치에 영향 없음
    // → Spacer/Box로 자리를 고정하지 않으므로 레이아웃 무결성 보장
    if (!adFailed) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                AdView(context).apply {
                    // 광고 크기 설정 — BANNER: 320x50 표준 배너
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId

                    adListener = object : AdListener() {

                        /**
                         * 광고 로드 성공
                         * - View를 VISIBLE로 전환하여 정상 표시
                         */
                        override fun onAdLoaded() {
                            visibility = View.VISIBLE
                            Log.d(
                                "BannerAdView",
                                "Ad loaded successfully. Unit: ${this@apply.adUnitId}"
                            )
                        }

                        /**
                         * 광고 로드 실패 — 핵심 방어 로직
                         *
                         * 처리 순서:
                         * 1. View.GONE: Android View 레벨에서 공간 제거
                         * 2. adFailed = true: Compose 상태 변경으로 recomposition 트리거
                         * 3. if (!adFailed) 블록이 false가 되어 AndroidView가 트리에서 제거
                         * → 진도 입력·시험 목록·진도 기록 등 핵심 UI는 전혀 영향받지 않음
                         *
                         * 재시도 없음: 실패 후 adFailed = true로 고정하여
                         * 무한 재시도 루프(battery drain) 방지
                         */
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            visibility = View.GONE
                            adFailed = true
                            Log.w(
                                "BannerAdView",
                                "Ad failed to load. " +
                                        "Code: ${error.code}, " +
                                        "Message: ${error.message}, " +
                                        "Domain: ${error.domain}. " +
                                        "Banner hidden — core features unaffected."
                            )
                        }

                        /** 광고 클릭으로 외부 화면 열림 */
                        override fun onAdOpened() {
                            Log.d("BannerAdView", "Ad opened (user tapped).")
                        }

                        /** 외부 화면에서 앱으로 복귀 */
                        override fun onAdClosed() {
                            Log.d("BannerAdView", "Ad closed (returned to app).")
                        }

                        /** 광고 노출 기록됨 */
                        override fun onAdImpression() {
                            Log.d("BannerAdView", "Ad impression recorded.")
                        }
                    }

                    // 초기 visibility — 로드 완료 전까지 숨김
                    // onAdLoaded에서 VISIBLE로 전환됨
                    visibility = View.INVISIBLE

                    // 광고 요청 시작
                    // AdRequest.Builder().build(): 기본 요청 (테스트 디바이스 자동 감지)
                    loadAd(AdRequest.Builder().build())
                }
            },
            // update 블록: adUnitId가 변경되는 경우 재설정 (방어 코드)
            update = { adView ->
                if (adView.adUnitId != adUnitId) {
                    adView.adUnitId = adUnitId
                }
            }
        )
    }
    // adFailed = true: 아무것도 렌더링하지 않음
    // Spacer/Box 없음 — 핵심 UI(ProgressInputSection 등)가 원래 위치에 자연스럽게 배치됨
}