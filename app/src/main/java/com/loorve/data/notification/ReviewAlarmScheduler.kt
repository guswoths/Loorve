package com.loorve.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/*
 * ────────────────────────────────────────────────────────────────
 *  필수 권한 (AndroidManifest.xml에 추가 필요)
 *
 *  API 31 (Android 12) ~ API 32:
 *    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
 *
 *  API 33 (Android 13)+:
 *    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
 *
 *  두 권한을 모두 선언하면 OS가 적절히 처리하므로 둘 다 선언 권장.
 *  단, USE_EXACT_ALARM은 캘린더/알람 앱 전용으로 심사 기준이 엄격하므로
 *  일반 복습 알림 앱은 SCHEDULE_EXACT_ALARM + 사용자 권한 요청 흐름 권장.
 * ────────────────────────────────────────────────────────────────
 *
 *  재부팅 복원 설계:
 *  이 클래스는 예약/취소 책임만 담당 (SRP).
 *  재부팅 후 재등록은 BootReceiver(3단계)에서 이 클래스를 @Inject받아 호출.
 *
 *  AlarmBroadcastReceiver 연결:
 *  PendingIntent 대상은 AlarmBroadcastReceiver(2단계)로,
 *  수신 후 Notification 발행 책임은 해당 Receiver가 담당.
 * ────────────────────────────────────────────────────────────────
 */

/** AlarmManager 브로드캐스트 타겟 클래스명 (2단계에서 생성될 Receiver) */
private const val ALARM_RECEIVER_CLASS = "com.loorve.receiver.AlarmBroadcastReceiver"

/** Intent Extra 키: 복습 일정 ID */
const val EXTRA_REVIEW_SCHEDULE_ID = "extra_review_schedule_id"

private const val TAG = "ReviewAlarmScheduler"

/**
 * AlarmManager 기반 복습 알림 예약/취소 스케줄러.
 *
 * - 예약: [scheduleReviewAlarm]
 * - 취소: [cancelReviewAlarm]
 *
 * Hilt [Singleton]으로 주입되며, [ApplicationContext]를 통해 시스템 서비스에 접근.
 */
@Singleton
class ReviewAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 복습 알림을 예약합니다.
     *
     * @param reviewScheduleId 복습 일정 고유 ID (PendingIntent Extra 및 requestCode 생성에 사용)
     * @param triggerAtMillis  알림이 발생할 시각 (Unix epoch milliseconds)
     *
     * 엣지 케이스:
     * - [triggerAtMillis]가 현재 시각 이전이면 즉시 발송 처리 (1초 후 예약).
     *   이는 재부팅 직후 이미 지난 알람을 유실 없이 처리하기 위함.
     *
     * 권한 주의:
     * - API 31+에서 정확한 알람을 설정하려면 [AlarmManager.canScheduleExactAlarms] == true 필요.
     *   false인 경우 시스템 설정 유도 또는 비정확 알람([AlarmManager.set]) 폴백 처리.
     */
    fun scheduleReviewAlarm(reviewScheduleId: String, triggerAtMillis: Long) {
        val now = System.currentTimeMillis()

        // 과거 시각이면 즉시(1초 후) 발송으로 보정
        val adjustedTriggerMillis = if (triggerAtMillis <= now) {
            Log.w(TAG, "triggerAtMillis($triggerAtMillis) is in the past. Scheduling immediately.")
            now + 1_000L
        } else {
            triggerAtMillis
        }

        val pendingIntent = buildPendingIntent(reviewScheduleId) ?: run {
            Log.e(TAG, "Failed to build PendingIntent for $reviewScheduleId")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ : SCHEDULE_EXACT_ALARM 권한 보유 여부 확인
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    adjustedTriggerMillis,
                    pendingIntent
                )
                Log.d(TAG, "Exact alarm scheduled (API31+): id=$reviewScheduleId at=$adjustedTriggerMillis")
            } else {
                // 권한 없음: 비정확 알람으로 폴백 (Doze 모드 영향 받음)
                // TODO: ViewModel/UI 레이어에서 ACTION_REQUEST_SCHEDULE_EXACT_ALARM 인텐트로 권한 요청 유도 필요
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    adjustedTriggerMillis,
                    pendingIntent
                )
                Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted. Using inexact alarm as fallback.")
            }
        } else {
            // API 21~30: setExact() 사용 (setAlarmClock은 잠금화면 UI 노출로 UX 부적합)
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                adjustedTriggerMillis,
                pendingIntent
            )
            Log.d(TAG, "Exact alarm scheduled (legacy): id=$reviewScheduleId at=$adjustedTriggerMillis")
        }
    }

    /**
     * 예약된 복습 알림을 취소합니다.
     *
     * @param reviewScheduleId 취소할 복습 일정 고유 ID
     */
    fun cancelReviewAlarm(reviewScheduleId: String) {
        val pendingIntent = buildPendingIntent(reviewScheduleId) ?: run {
            Log.e(TAG, "Failed to build PendingIntent for cancel: $reviewScheduleId")
            return
        }
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Alarm cancelled: id=$reviewScheduleId")
    }

    /**
     * [reviewScheduleId]로부터 PendingIntent를 생성합니다.
     *
     * requestCode: String ID를 [hashCode]로 변환.
     * 주의: 극히 드문 해시 충돌로 인해 서로 다른 ID가 동일 requestCode를 가질 수 있음.
     *       실제 운영 시 ID 체계를 Int 범위 내 고유 값으로 설계하면 충돌 방지 가능.
     *
     * FLAG_IMMUTABLE: Android 12(API 31)+ 필수 보안 요구사항.
     */
    private fun buildPendingIntent(reviewScheduleId: String): PendingIntent? {
        // 2단계(AlarmBroadcastReceiver)가 생성되기 전까지 임시로 클래스명 직접 참조.
        // 2단계 완료 후: Intent(context, AlarmBroadcastReceiver::class.java) 로 교체 권장.
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
}