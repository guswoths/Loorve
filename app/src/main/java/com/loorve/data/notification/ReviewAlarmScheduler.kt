package com.loorve.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.loorve.util.ExactAlarmPermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/*
 * ────────────────────────────────────────────────────────────────
 *  AlarmManager 기반 복습 알림 예약/취소 스케줄러.
 *
 *  권한 처리 전략:
 *  1. ExactAlarmPermissionHelper.canScheduleExactAlarms() 선행 체크
 *  2. 권한 있음 → setExactAndAllowWhileIdle() (정확한 알람)
 *  3. 권한 없음 → Fallback 1: setAndAllowWhileIdle() (비정확 알람)
 *               → Fallback 2: WorkManager (향후 구현 안내 주석 참조)
 *
 *  재부팅 복원: BootReceiver (3~4단계)에서 이 클래스를 @Inject받아 재등록.
 * ────────────────────────────────────────────────────────────────
 */

private const val ALARM_RECEIVER_CLASS = "com.loorve.receiver.AlarmBroadcastReceiver"
const val EXTRA_REVIEW_SCHEDULE_ID = "extra_review_schedule_id"
private const val TAG = "ReviewAlarmScheduler"

@Singleton
class ReviewAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionHelper: ExactAlarmPermissionHelper
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 복습 알림을 예약합니다.
     *
     * @param reviewScheduleId 복습 일정 고유 ID
     * @param triggerAtMillis  알림 발생 시각 (Unix epoch milliseconds)
     * @return ScheduleResult 예약 방식 결과 (UI 레이어에서 권한 요청 흐름 판단에 활용)
     */
    fun scheduleReviewAlarm(reviewScheduleId: String, triggerAtMillis: Long): ScheduleResult {
        val now = System.currentTimeMillis()

        val adjustedTriggerMillis = if (triggerAtMillis <= now) {
            Log.w(TAG, "triggerAtMillis($triggerAtMillis) is in the past. Scheduling 1s from now.")
            now + 1_000L
        } else {
            triggerAtMillis
        }

        val pendingIntent = buildPendingIntent(reviewScheduleId) ?: run {
            Log.e(TAG, "Failed to build PendingIntent for id=$reviewScheduleId")
            return ScheduleResult.FAILED
        }

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (permissionHelper.canScheduleExactAlarms()) {
                    // 정확한 알람 예약 (권한 있음)
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        adjustedTriggerMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "[EXACT] Alarm scheduled: id=$reviewScheduleId, triggerAt=$adjustedTriggerMillis")
                    ScheduleResult.EXACT
                } else {
                    /*
                     * Fallback 1: setAndAllowWhileIdle (비정확 알람)
                     * - Doze 모드에서 몇 분~수십 분 지연 가능
                     * - 권한 없을 때 즉시 동작 보장은 안 되지만 유실보다 낫다
                     *
                     * Fallback 2: WorkManager (향후 선택적 구현)
                     * WorkManager.getInstance(context)
                     *     .enqueueUniqueWork(
                     *         "review_alarm_$reviewScheduleId",
                     *         ExistingWorkPolicy.REPLACE,
                     *         OneTimeWorkRequestBuilder<ReviewNotificationWorker>()
                     *             .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                     *             .setInputData(workDataOf(EXTRA_REVIEW_SCHEDULE_ID to reviewScheduleId))
                     *             .build()
                     *     )
                     */
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        adjustedTriggerMillis,
                        pendingIntent
                    )
                    Log.w(TAG, "[FALLBACK/INEXACT] SCHEDULE_EXACT_ALARM not granted. id=$reviewScheduleId")
                    ScheduleResult.FALLBACK_INEXACT
                }
            }
            else -> {
                // API 30 이하: setExact() 권한 불필요
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    adjustedTriggerMillis,
                    pendingIntent
                )
                Log.d(TAG, "[EXACT/LEGACY] Alarm scheduled: id=$reviewScheduleId, triggerAt=$adjustedTriggerMillis")
                ScheduleResult.EXACT
            }
        }
    }

    /**
     * 예약된 복습 알림을 취소합니다.
     */
    fun cancelReviewAlarm(reviewScheduleId: String) {
        val pendingIntent = buildPendingIntent(reviewScheduleId) ?: run {
            Log.e(TAG, "Failed to build PendingIntent for cancel: id=$reviewScheduleId")
            return
        }
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "[CANCEL] Alarm cancelled: id=$reviewScheduleId")
    }

    private fun buildPendingIntent(reviewScheduleId: String): PendingIntent? {
        val intent = try {
            Intent().apply {
                setClassName(context, ALARM_RECEIVER_CLASS)
                putExtra(EXTRA_REVIEW_SCHEDULE_ID, reviewScheduleId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Intent: ${e.message}")
            return null
        }

        return PendingIntent.getBroadcast(
            context,
            reviewScheduleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 알람 예약 결과 타입.
     * ViewModel/UI에서 이 결과를 기반으로 권한 요청 화면 표시 여부를 결정.
     */
    enum class ScheduleResult {
        /** 정확한 알람으로 예약 성공 */
        EXACT,
        /** 권한 없어 비정확 알람으로 폴백 예약 */
        FALLBACK_INEXACT,
        /** PendingIntent 생성 실패 등 예약 자체 실패 */
        FAILED
    }
}