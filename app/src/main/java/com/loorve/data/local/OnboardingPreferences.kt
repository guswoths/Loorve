package com.loorve.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// 싱글톤 DataStore 인스턴스 — Context 확장 프로퍼티로 중복 생성 방지
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_prefs"
)

/**
 * 앱 최초 실행 여부(온보딩 완료 플래그)를 DataStore Preferences로 관리합니다.
 * SharedPreferences 사용 금지 — 타입 안전성 및 코루틴 Flow 지원을 위해 DataStore 사용.
 *
 * @param context ApplicationContext (Hilt @ApplicationContext 주입)
 */
@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
    }

    /**
     * 온보딩 완료 여부를 Flow로 반환합니다.
     * - 저장된 값이 없을 경우 기본값 false 반환
     * - IOException 발생 시 emptyPreferences()로 fallback하여 false emit
     */
    val isOnboardingComplete: Flow<Boolean> = context.onboardingDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.IS_ONBOARDING_COMPLETE] ?: false
        }

    /**
     * 온보딩 완료 여부를 DataStore에 저장합니다.
     * @param value true = 온보딩 완료, false = 미완료
     */
    suspend fun setOnboardingComplete(value: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[Keys.IS_ONBOARDING_COMPLETE] = value
        }
    }
}
