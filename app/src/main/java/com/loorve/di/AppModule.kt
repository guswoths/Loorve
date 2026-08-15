package com.loorve.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule
 *
 * Hilt DI 모듈: 앱 전역(Singleton) 스코프 Firebase 의존성 제공.
 * - FirebaseAuth: 이메일/소셜 인증 처리
 * - FirebaseFirestore: Firestore DB (오프라인 영속 캐시 활성화)
 *
 * 주의: Firebase 초기화는 google-services.json 기반으로 자동 수행되므로
 *       API 키 등 민감 정보를 코드에 직접 하드코딩하지 않습니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().also { firestore ->
            val settings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings {})
            }
            firestore.firestoreSettings = settings
        }
    }
}
