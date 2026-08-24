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

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_UNIT_ID
) {
    var adFailed by remember { mutableStateOf(false) }

    // [추가] adFailed = false일 때만 AndroidView를 렌더링하되,
    // DisposableEffect로 컴포저블 생명주기 종료 시 AdView.destroy() 보장
    if (!adFailed) {
        var adViewRef: AdView? = remember { null }

        DisposableEffect(Unit) {
            onDispose {
                // Compose tree에서 제거될 때 AdView 리소스 해제 (메모리 리크 방지)
                adViewRef?.destroy()
                adViewRef = null
                Log.d("BannerAdView", "AdView destroyed on dispose.")
            }
        }

        AndroidView(
            modifier = modifier,
            factory = { context ->
                AdView(context).also { adView ->
                    adViewRef = adView  // DisposableEffect에서 참조 가능하도록 저장
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