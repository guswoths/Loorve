// 경로: app/src/main/java/com/loorve/ui/component/BannerAdView.kt
package com.loorve.ui.component

import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
 * AdMob 배너 광고 Composable.
 *
 * [방어 설계]
 * - 광고 로드 실패(onAdFailedToLoad) 시 adFailed = true 로 전환.
 * - adFailed = true 이면 AndroidView 자체가 Compose tree에서 제거(Gone 처리).
 * - DisposableEffect에서 AdView.destroy()를 반드시 호출해 메모리 리크 방지.
 * - adFailed 상태 변경은 View.post{}로 메인스레드에서 수행 (스레드 안전).
 * - onAdFailed 콜백을 통해 상위 Composable(ReviewCalendarScreen 등)에 실패 상태 전파.
 *
 * @param modifier       Compose Modifier
 * @param adUnitId       AdMob 배너 광고 단위 ID (BuildConfig에서 주입)
 * @param onAdFailed     광고 로드 실패 시 호출되는 콜백 (상위에서 레이아웃 조정 등 처리)
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_UNIT_ID,
    onAdFailed: () -> Unit = {}
) {
    var adFailed by remember { mutableStateOf(false) }

    // adFailed = true 시 AndroidView를 Compose tree에서 완전 제거
    // → BannerAdView 삽입 지점이 0 높이가 되어 핵심 UI 배치에 영향 없음
    if (!adFailed) {
        // [수정] adViewRef를 MutableState로 관리하여 DisposableEffect 클로저에서
        //        최신 AdView 참조를 안전하게 캡처 (기존 var 로컬 변수 방식의 null 참조 버그 수정)
        val adViewState = remember { mutableStateOf<AdView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                // Compose tree에서 제거될 때 AdView 리소스 해제 (메모리 리크 방지)
                adViewState.value?.destroy()
                adViewState.value = null
                Log.d("BannerAdView", "AdView destroyed on dispose.")
            }
        }

        AndroidView(
            modifier = modifier,
            factory = { context ->
                AdView(context).also { adView ->
                    adViewState.value = adView  // MutableState에 저장 → DisposableEffect에서 참조 가능
                    adView.setAdSize(AdSize.BANNER)
                    adView.adUnitId = adUnitId

                    adView.adListener = object : AdListener() {

                        override fun onAdLoaded() {
                            adView.visibility = View.VISIBLE
                            Log.d(
                                "BannerAdView",
                                "Ad loaded successfully. Unit: ${adView.adUnitId}"
                            )
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            adView.visibility = View.GONE
                            // [유지] View.post{}로 메인스레드 안전 보장
                            adView.post {
                                adFailed = true
                                // [추가] 상위 Composable에 실패 상태 전파
                                // ReviewCalendarScreen: isBannerVisible = false 처리
                                onAdFailed()
                            }
                            Log.w(
                                "BannerAdView",
                                "Ad failed to load. " +
                                        "Code: ${error.code}, " +
                                        "Message: ${error.message}, " +
                                        "Domain: ${error.domain}. " +
                                        "Banner hidden — core features unaffected."
                            )
                        }

                        override fun onAdOpened() {
                            Log.d("BannerAdView", "Ad opened (user tapped).")
                        }

                        override fun onAdClosed() {
                            Log.d("BannerAdView", "Ad closed (returned to app).")
                        }

                        override fun onAdImpression() {
                            Log.d("BannerAdView", "Ad impression recorded.")
                        }
                    }

                    adView.visibility = View.INVISIBLE
                    adView.loadAd(AdRequest.Builder().build())
                }
            },
            update = { adView ->
                if (adView.adUnitId != adUnitId) {
                    adView.adUnitId = adUnitId
                    adView.loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}