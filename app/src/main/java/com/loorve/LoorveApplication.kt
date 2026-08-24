// 경로: app/src/main/java/com/loorve/LoorveApplication.kt
package com.loorve

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
     * - DEBUG 빌드에서만 테스트 장치를 등록하여 실제 광고 정책 위반을 방지.
     * - setRequestConfiguration은 반드시 initialize() 호출 전에 실행해야 함.
     */
    private fun initAdMob() {
        try {
            // [추가] DEBUG 빌드 전용 테스트 장치 등록
            // release 빌드에서는 이 블록이 컴파일되지 않으므로 정책 위반 없음
            if (BuildConfig.DEBUG) {
                val testDeviceIds = mutableListOf(
                    com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR // 에뮬레이터 자동 등록
                    // 실기기 테스트 시 아래 주석 해제 후 Logcat에서 확인한 해시 ID 입력:
                    // "YOUR_REAL_DEVICE_HASH_ID"
                )
                val configuration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(configuration)
                Log.d("LoorveApp", "AdMob test device configuration applied (DEBUG only).")
            }

            MobileAds.initialize(this) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                Log.d("LoorveApp", "AdMob initialized. Adapter status: $statusMap")
            }
        } catch (e: Exception) {
            // AdMob 초기화 실패는 광고 미노출로만 처리 — 앱 크래시 금지
            // 알림(receiver/service) 및 핵심 기능은 이 예외와 독립적으로 동작함
            Log.e("LoorveApp", "AdMob initialization failed (non-fatal): ${e.message}")
        }
    }
}