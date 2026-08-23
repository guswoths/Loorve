package com.loorve.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// 싱글톤 DataStore 인스턴스 — Context 확장 프로퍼티로 중복 생성 방지
private val Context.notificationTimeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_time_prefs"
)

/**
 * 알림 시간(시·분)을 DataStore Preferences로 관리합니다.
 * SharedPreferences 사용 금지 — 타입 안전성 및 코루틴 Flow 지원을 위해 DataStore 사용.
 *
 * @param context ApplicationContext (Hilt @ApplicationContext 주입)
 */
@Singleton
class NotificationTimePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val NOTIFICATION_HOUR   = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
    }

    /**
     * 저장된 알림 시간을 (hour, minute) 쌍의 Flow로 반환합니다.
     * - 저장된 값이 없을 경우 기본값 hour = 9, minute = 0 (오전 9시) 반환
     * - IOException 발생 시 emptyPreferences()로 fallback하여 기본값 emit
     */
    val notificationTime: Flow<Pair<Int, Int>> = context.notificationTimeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val hour   = preferences[Keys.NOTIFICATION_HOUR]   ?: 9
            val minute = preferences[Keys.NOTIFICATION_MINUTE] ?: 0
            Pair(hour, minute)
        }

    /**
     * 알림 시간을 DataStore에 저장합니다.
     * @param hour   저장할 시 (0~23)
     * @param minute 저장할 분 (0, 10, 20, 30, 40, 50)
     */
    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.notificationTimeDataStore.edit { preferences ->
            preferences[Keys.NOTIFICATION_HOUR]   = hour
            preferences[Keys.NOTIFICATION_MINUTE] = minute
        }
    }

    /** 로그아웃 시 호출 — 알림 시간 설정 전체 삭제 (기본값 9:00으로 자동 fallback) */
    suspend fun clearAll() {
        context.notificationTimeDataStore.edit { preferences ->
            preferences.remove(Keys.NOTIFICATION_HOUR)
            preferences.remove(Keys.NOTIFICATION_MINUTE)
        }
    }
}