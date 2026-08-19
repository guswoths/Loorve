// app/src/main/java/com/loorve/di/RepositoryModule.kt
package com.loorve.di

import com.loorve.data.repository.AuthRepositoryImpl
import com.loorve.data.repository.ExamRepositoryImpl   // ← 추가
import com.loorve.data.repository.ProgressRepositoryImpl
import com.loorve.domain.repository.AuthRepository
import com.loorve.domain.repository.ExamRepository     // ← 추가
import com.loorve.domain.repository.ProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(
        impl: ProgressRepositoryImpl
    ): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindExamRepository(      // ← 추가
        impl: ExamRepositoryImpl
    ): ExamRepository
}
