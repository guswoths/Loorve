package com.loorve

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LoorveApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // ✅ 앱 시작 시 Firebase 토큰 자동 갱신 설정
        // 장시간 백그라운드 후 포그라운드 복귀 시 토큰 만료 방지
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            auth.currentUser?.getIdToken(/* forceRefresh= */ false)
                ?.addOnFailureListener {
                    // 토큰 갱신 실패 시 강제 재발급 시도
                    auth.currentUser?.getIdToken(/* forceRefresh= */ true)
                }
        }
    }
}