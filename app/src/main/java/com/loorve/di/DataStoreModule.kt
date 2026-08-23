package com.loorve.di

import android.content.Context
import com.loorve.data.local.NotificationTimePreferences
import com.loorve.data.local.OnboardingPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataStore 관련 의존성을 제공하는 Hilt 모듈.
 * AppModule과 관심사를 분리하여 확장성을 유지합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideOnboardingPreferences(
        @ApplicationContext context: Context
    ): OnboardingPreferences = OnboardingPreferences(context)

    @Provides
    @Singleton
    fun provideNotificationTimePreferences(
        @ApplicationContext context: Context
    ): NotificationTimePreferences = NotificationTimePreferences(context)
}