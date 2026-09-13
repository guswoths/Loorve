package com.loorve.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    fun notificationTime(uid: String): Flow<Pair<Int, Int>> =
        context.notificationTimeDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences ->
                Pair(
                    preferences[intPreferencesKey("notification_hour_$uid")] ?: 9,
                    preferences[intPreferencesKey("notification_minute_$uid")] ?: 0
                )
            }

    fun notificationEnabled(uid: String): Flow<Boolean> =
        context.notificationTimeDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences ->
                preferences[booleanPreferencesKey("notifications_enabled_$uid")] ?: true
            }

    /**
     * 알림 시간을 DataStore에 저장합니다.
     * @param hour   저장할 시 (0~23)
     * @param minute 저장할 분 (0, 10, 20, 30, 40, 50)
     */
    suspend fun setNotificationTime(uid: String, hour: Int, minute: Int) {
        context.notificationTimeDataStore.edit { preferences ->
            preferences[intPreferencesKey("notification_hour_$uid")] = hour
            preferences[intPreferencesKey("notification_minute_$uid")] = minute
        }
    }

    suspend fun setNotificationEnabled(uid: String, enabled: Boolean) {
        context.notificationTimeDataStore.edit { preferences ->
            preferences[booleanPreferencesKey("notifications_enabled_$uid")] = enabled
        }
    }

    suspend fun clearAll(uid: String) {
        context.notificationTimeDataStore.edit { preferences ->
            preferences.remove(intPreferencesKey("notification_hour_$uid"))
            preferences.remove(intPreferencesKey("notification_minute_$uid"))
            preferences.remove(booleanPreferencesKey("notifications_enabled_$uid"))
        }
    }
}