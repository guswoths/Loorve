// 파일: app/src/main/java/com/loorve/receiver/BootCompletedReceiver.kt

package com.loorve.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.loorve.data.notification.ReviewAlarmScheduler
import com.loorve.domain.repository.ReviewScheduleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BootCompletedReceiver"

/**
 * 기기 재부팅 후 AlarmManager 알람을 재등록하는 BroadcastReceiver.
 *
 * - BOOT_COMPLETED: 일반 재부팅 완료 후 수신
 * - LOCKED_BOOT_COMPLETED: 직접 부팅(FBE) 환경에서 잠금 해제 전에도 수신
 *
 * [주의] AlarmManager 알람은 기기 재부팅 시 모두 초기화되므로,
 * 앱 재시작 없이도 이 Receiver를 통해 자동 재등록해야 함.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reviewAlarmScheduler: ReviewAlarmScheduler

    @Inject
    lateinit var reviewScheduleRepository: ReviewScheduleRepository

    override fun onReceive(context: Context, intent: Intent) {
        // BOOT_COMPLETED 또는 LOCKED_BOOT_COMPLETED 이외의 인텐트는 무시
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            Log.w(TAG, "알 수 없는 액션 수신, 무시: ${intent.action}")
            return
        }

        Log.d(TAG, "재부팅 감지: action=${intent.action}")

        // Firebase Auth에서 현재 로그인된 uid 획득
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "로그인된 사용자 없음. 알람 재등록 건너뜀.")
            return
        }

        // goAsync(): onReceive()의 10초 제한을 비동기로 연장
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                Log.d(TAG, "미완료 복습 일정 조회 시작 (uid=$uid)")

                val now = System.currentTimeMillis()

                // 현재 시각 이후의 미완료 복습 일정 조회
                val result = reviewScheduleRepository.getUpcomingIncompleteSchedules(uid, now)

                result
                    .onSuccess { schedules ->
                        Log.d(TAG, "조회된 미완료 복습 일정 수: ${schedules.size}")

                        schedules.forEach { schedule ->
                            try {
                                val scheduleResult = reviewAlarmScheduler.scheduleReviewAlarm(
                                    reviewScheduleId = schedule.scheduleId,  // ✅ 수정
                                    triggerAtMillis = schedule.reviewDate
                                )
                                Log.d(
                                    TAG,
                                    "알람 재등록: id=${schedule.scheduleId}, " +  // ✅ 수정
                                            "reviewDate=${schedule.reviewDate}, result=$scheduleResult"
                                )
                            } catch (e: Exception) {
                                // 개별 알람 실패가 전체를 중단하지 않도록 개별 예외 처리
                                Log.e(
                                    TAG,
                                    "알람 재등록 실패: id=${schedule.scheduleId}, " +  // ✅ 수정
                                            "error=${e.message}"
                                )
                            }
                        }

                        Log.d(TAG, "알람 재등록 완료")
                    }
                    .onFailure { error ->
                        // 네트워크 불안정 등 Firestore 조회 실패 처리
                        Log.e(TAG, "복습 일정 조회 실패: ${error.message}")
                    }

            } catch (e: Exception) {
                Log.e(TAG, "재부팅 알람 재등록 중 예외 발생: ${e.message}")
            } finally {
                // PendingResult 반드시 finish() 호출하여 시스템에 완료 통보
                pendingResult.finish()
                Log.d(TAG, "PendingResult.finish() 호출 완료")
            }
        }
    }
}