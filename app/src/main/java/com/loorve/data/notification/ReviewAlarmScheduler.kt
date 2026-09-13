package com.loorve.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.loorve.data.local.NotificationTimePreferences
import com.loorve.util.ExactAlarmPermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
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
const val EXTRA_REVIEW_ALARM_TRIGGER_AT_MILLIS = "extra_review_alarm_trigger_at_millis"
private const val TAG = "ReviewAlarmScheduler"

@Singleton
class ReviewAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionHelper: ExactAlarmPermissionHelper,
    private val notificationPreferences: NotificationTimePreferences,
    private val firebaseAuth: FirebaseAuth
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
    suspend fun scheduleReviewAlarm(reviewScheduleId: String, triggerAtMillis: Long): ScheduleResult {
        val now = System.currentTimeMillis()
        val uid = firebaseAuth.currentUser?.uid

        if (uid.isNullOrBlank() || !notificationPreferences.notificationEnabled(uid).first()) {
            Log.d(TAG, "Alarm not scheduled because review notifications are disabled: id=$reviewScheduleId")
            return ScheduleResult.DISABLED
        }

        if (reviewScheduleId.isBlank()) {
            Log.e(TAG, "Alarm not scheduled: blank scheduleId, triggerAt=$triggerAtMillis")
            return ScheduleResult.FAILED
        }

        if (triggerAtMillis <= now) {
            Log.w(
                TAG,
                "Alarm not scheduled because triggerAt is not in the future: " +
                    "id=$reviewScheduleId, triggerAt=$triggerAtMillis, now=$now"
            )
            return ScheduleResult.FAILED
        }

        val pendingIntent = buildPendingIntent(reviewScheduleId, triggerAtMillis) ?: run {
            Log.e(TAG, "Failed to build PendingIntent for id=$reviewScheduleId")
            return ScheduleResult.FAILED
        }
        cancelLegacyAlarm(reviewScheduleId)
        alarmManager.cancel(pendingIntent)
        Log.d(
            TAG,
            "Replacing existing one-shot alarm: id=$reviewScheduleId, triggerAt=$triggerAtMillis"
        )

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (permissionHelper.canScheduleExactAlarms()) {
                    // 정확한 알람 예약 (권한 있음)
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "[EXACT] Alarm scheduled: id=$reviewScheduleId, triggerAt=$triggerAtMillis")
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
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.w(
                        TAG,
                        "[FALLBACK/INEXACT] Alarm scheduled: " +
                            "id=$reviewScheduleId, triggerAt=$triggerAtMillis"
                    )
                    ScheduleResult.FALLBACK_INEXACT
                }
            }
            else -> {
                // API 30 이하: setExact() 권한 불필요
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "[EXACT/LEGACY] Alarm scheduled: id=$reviewScheduleId, triggerAt=$triggerAtMillis")
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
        cancelLegacyAlarm(reviewScheduleId)
        Log.d(TAG, "[CANCEL] Alarm cancelled: id=$reviewScheduleId")
    }

    /**
     * 로그아웃 시 호출 — 전달받은 ID 목록의 알람 전체 취소.
     * UseCase에서 Firestore/로컬 캐시의 미완료 일정 ID를 수집하여 전달.
     *
     * @param reviewScheduleIds 취소할 복습 일정 ID 목록
     */
    fun cancelAll(reviewScheduleIds: List<String>) {
        reviewScheduleIds.forEach { id ->
            cancelReviewAlarm(id)
        }
        Log.d(TAG, "[CANCEL_ALL] ${reviewScheduleIds.size}개 알람 취소 완료")
    }

    private fun buildPendingIntent(
        reviewScheduleId: String,
        triggerAtMillis: Long = 0L
    ): PendingIntent? {
        val intent = try {
            Intent().apply {
                setClassName(context, ALARM_RECEIVER_CLASS)
                putExtra(EXTRA_REVIEW_SCHEDULE_ID, reviewScheduleId)
                putExtra(EXTRA_REVIEW_ALARM_TRIGGER_AT_MILLIS, triggerAtMillis)
                data = Uri.parse(
                    "loorve://review-alarm/${Uri.encode(reviewScheduleId, "")}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Intent: ${e.message}")
            return null
        }

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildLegacyPendingIntent(reviewScheduleId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            reviewScheduleId.hashCode(),
            Intent().apply {
                setClassName(context, ALARM_RECEIVER_CLASS)
                putExtra(EXTRA_REVIEW_SCHEDULE_ID, reviewScheduleId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun cancelLegacyAlarm(reviewScheduleId: String) {
        val legacyPendingIntent = buildLegacyPendingIntent(reviewScheduleId)
        alarmManager.cancel(legacyPendingIntent)
        legacyPendingIntent.cancel()
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
        /** Review notifications are disabled for the signed-in account. */
        DISABLED,
        /** PendingIntent 생성 실패 등 예약 자체 실패 */
        FAILED
    }
}