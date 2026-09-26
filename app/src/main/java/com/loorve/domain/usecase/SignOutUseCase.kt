package com.loorve.domain.usecase

import android.util.Log
import com.loorve.data.local.NotificationTimePreferences
import com.loorve.data.local.OnboardingPreferences
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.repository.AuthRepository
import com.loorve.domain.repository.ReviewScheduleRepository
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val reviewScheduleRepository: ReviewScheduleRepository,
    private val reviewScheduleItemRepository: ReviewScheduleItemRepository,
    private val notificationTimePreferences: NotificationTimePreferences,
    private val reviewAlarmScheduler: ReviewAlarmScheduler,
    private val firebaseAuth: FirebaseAuth,
    private val onboardingPreferences: OnboardingPreferences
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid

            if (uid != null) {
                val now = System.currentTimeMillis()
                // ✅ 수정: 추가된 인터페이스 메서드 사용
                reviewScheduleRepository.getUpcomingIncompleteSchedules(uid, now)
                    .onSuccess { schedules ->
                        // ✅ 수정: scheduleId 필드명 통일
                        reviewAlarmScheduler.cancelAll(schedules.map { it.scheduleId })
                        Log.d(TAG, "알람 ${schedules.size}건 취소 완료 (uid=$uid)")
                    }
                    .onFailure { e ->
                        Log.w(TAG, "알람 취소 스케줄 조회 실패 (계속 진행): ${e.message}")
                    }
                reviewScheduleItemRepository.getAllScheduleItems(uid)
                    .onSuccess { schedules ->
                        reviewAlarmScheduler.cancelAll(schedules.map { it.id })
                        Log.d(TAG, "ReviewScheduleItem 알람 ${schedules.size}건 취소 완료")
                    }
                    .onFailure { e ->
                        Log.w(TAG, "ReviewScheduleItem 알람 취소 조회 실패 (계속 진행): ${e.message}")
                    }
            }

            if (uid != null) notificationTimePreferences.clearAll(uid)
            Log.d(TAG, "NotificationTimePreferences clearAll 완료")

            onboardingPreferences.setOnboardingComplete(false)
            Log.d(TAG, "OnboardingPreferences reset 완료")

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