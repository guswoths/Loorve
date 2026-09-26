package com.loorve

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.loorve.util.CalendarRefreshBus
import com.loorve.util.ensureReviewNotificationChannel
import com.loorve.widget.TodayReviewWidgetManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LoorveApplication : Application() {

    @Inject
    lateinit var calendarRefreshBus: CalendarRefreshBus

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        ensureReviewNotificationChannel(this)
        // ✅ 앱 시작 시 Firebase 토큰 자동 갱신 설정
        // 장시간 백그라운드 후 포그라운드 복귀 시 토큰 만료 방지
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            auth.currentUser?.getIdToken(/* forceRefresh= */ false)
                ?.addOnFailureListener {
                    // 토큰 갱신 실패 시 강제 재발급 시도
                    auth.currentUser?.getIdToken(/* forceRefresh= */ true)
                }
            if (auth.currentUser != null) {
                TodayReviewWidgetManager.updateAllWidgets(this)
            }
        }

        // ✅ 복습 상태 및 블록 변경 시 홈화면 위젯 동기화
        applicationScope.launch {
            calendarRefreshBus.refreshEvent.collect {
                TodayReviewWidgetManager.updateAllWidgets(this@LoorveApplication)
            }
        }
    }
}