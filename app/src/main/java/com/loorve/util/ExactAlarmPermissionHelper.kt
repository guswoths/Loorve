package com.loorve.util

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager 정확한 알람 예약 권한 확인 유틸리티.
 *
 * API 레벨별 분기:
 * - API < 31 (Android 11 이하): 권한 불필요 → 항상 true
 * - API 31~32 (Android 12): SCHEDULE_EXACT_ALARM 권한, 사용자 수동 허용 필요
 * - API 33+ (Android 13+): SCHEDULE_EXACT_ALARM 또는 USE_EXACT_ALARM 권한 필요
 *
 * Hilt @Singleton으로 주입되어 앱 전체에서 단일 인스턴스 사용.
 */
@Singleton
class ExactAlarmPermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    /**
     * 정확한 알람 예약 가능 여부를 반환합니다.
     *
     * @return true: 정확한 알람 예약 가능 / false: 권한 없음, 사용자 설정 필요
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: 런타임 권한 상태 확인
            alarmManager.canScheduleExactAlarms()
        } else {
            // API 30 이하: 권한 체계 없음, 항상 허용
            true
        }
    }

    /**
     * 현재 기기가 정확한 알람 권한을 런타임으로 요구하는지 여부.
     * API 31+ 기기에서만 true 반환 (권한 요청 UI 표시 여부 결정에 활용).
     */
    fun requiresExactAlarmPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}