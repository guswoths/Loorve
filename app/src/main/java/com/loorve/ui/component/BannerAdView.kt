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

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_UNIT_ID
) {
    var adFailed by remember { mutableStateOf(false) }

    if (!adFailed) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId

                    adListener = object : AdListener() {

                        override fun onAdLoaded() {
                            visibility = View.VISIBLE
                            Log.d(
                                "BannerAdView",
                                "Ad loaded successfully. Unit: ${this@apply.adUnitId}"
                            )
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            visibility = View.GONE
                            // [수정] Compose 상태 변경을 메인 스레드에서 명시적으로 실행
                            // View.post()는 메인 스레드 Handler에 enqueue → 안전한 상태 변경 보장
                            post {
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

                    visibility = View.INVISIBLE
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { adView ->
                if (adView.adUnitId != adUnitId) {
                    adView.adUnitId = adUnitId
                    // [수정] adUnitId 변경 시 새 ID로 광고 재요청
                    adView.loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}