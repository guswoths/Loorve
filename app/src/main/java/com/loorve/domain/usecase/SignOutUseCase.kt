// 경로: app/src/main/java/com/loorve/domain/usecase/SignOutUseCase.kt
package com.loorve.domain.usecase

import android.util.Log
import com.loorve.data.local.NotificationTimePreferences
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.repository.AuthRepository
import com.loorve.domain.repository.ReviewScheduleRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val notificationTimePreferences: NotificationTimePreferences,
    private val reviewAlarmScheduler: ReviewAlarmScheduler,
    private val firebaseAuth: FirebaseAuth
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid

            // 1. 등록된 알람 전체 취소 (uid 기반으로 일정 ID 조회 후 취소)
            if (uid != null) {
                val now = System.currentTimeMillis()
                reviewScheduleRepository.getUpcomingIncompleteSchedules(uid, now)
                    .onSuccess { schedules ->
                        reviewAlarmScheduler.cancelAll(schedules.map { it.reviewScheduleId })
                        Log.d(TAG, "알람 ${schedules.size}건 취소 완료 (uid=$uid)")
                    }
                    .onFailure { e ->
                        // ✅ 추가: 알람 취소 실패 시 로그 기록 (로그아웃은 계속 진행)
                        Log.w(TAG, "알람 취소 스케줄 조회 실패 (계속 진행): ${e.message}")
                    }
            }

            // 2. 알림 시간 DataStore 초기화
            notificationTimePreferences.clearAll()
            Log.d(TAG, "NotificationTimePreferences clearAll 완료")

            // 3. Firebase signOut (Google Credential revoke 포함)
            authRepository.signOut()

        } catch (e: Exception) {
            Log.e(TAG, "SignOutUseCase 실패", e)
            Result.failure(Exception("로그아웃 처리 중 오류가 발생했습니다.", e))
        }
    }

    companion object {
        private const val TAG = "SignOutUseCase"
    }
}