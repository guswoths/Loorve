package com.loorve.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.loorve.data.repository.AuthRepositoryImpl
import com.loorve.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        // AuthRepositoryImpl이 @ApplicationContext를 생성자에서 직접
        // @Inject받으므로 별도 provide 불필요 — Hilt가 자동 처리.
        // CredentialManager는 Activity Context가 필요하므로 Singleton 바인딩 불가,
        // loginWithGoogle(activityContext) 호출부에서 직접 생성.
    }
}
