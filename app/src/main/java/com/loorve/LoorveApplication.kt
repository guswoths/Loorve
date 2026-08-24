// 경로: app/src/main/java/com/loorve/LoorveApplication.kt
package com.loorve

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.hilt.android.HiltAndroidApp

/**
 * LoorveApplication
 *
 * @HiltAndroidApp 어노테이션으로 Hilt DI 컨테이너 초기화.
 * AndroidManifest.xml의 android:name=".LoorveApplication"과 반드시 일치해야 합니다.
 */
@HiltAndroidApp
class LoorveApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirestoreOfflinePersistence()
        initAdMob()
    }

    /**
     * Firestore 오프라인 퍼시스턴스 활성화.
     * - 오프라인 상태에서도 로컬 캐시에 먼저 쓰이고, 재연결 시 Firestore와 자동 동기화됨.
     * - PersistentCacheSettings: 디스크 기반 영구 캐시 (기기 재시작 후에도 유지)
     * - 반드시 FirebaseFirestore 첫 사용 전(onCreate)에 호출해야 함.
     */
    private fun initFirestoreOfflinePersistence() {
        try {
            val settings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings {})
            }
            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.d("LoorveApp", "Firestore offline persistence enabled (PersistentCache)")
        } catch (e: Exception) {
            // 이미 초기화된 경우 IllegalStateException 발생 가능 - 무시
            Log.w("LoorveApp", "Firestore settings already configured: ${e.message}")
        }
    }

    /**
     * Google AdMob 초기화.
     * - 초기화 실패(네트워크 불가, SDK 오류 등)가 발생해도 앱 크래시 없이 계속 실행됨.
     * - 초기화 콜백 내부에서만 광고 요청을 시작하는 것이 Google 권장 사항.
     *   (BannerAdView에서 loadAd() 직접 호출하므로 여기선 initialize만 수행)
     */
    private fun initAdMob() {
        try {
            MobileAds.initialize(this) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                Log.d("LoorveApp", "AdMob initialized. Adapter status: $statusMap")
            }
        } catch (e: Exception) {
            // AdMob 초기화 실패는 광고 미노출로만 처리 — 앱 크래시 금지
            Log.e("LoorveApp", "AdMob initialization failed (non-fatal): ${e.message}")
        }
    }
}