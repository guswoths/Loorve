// app/src/main/java/com/loorve/di/RepositoryModule.kt
package com.loorve.di

import com.loorve.data.repository.AuthRepositoryImpl
import com.loorve.data.repository.ExamRepositoryImpl
import com.loorve.data.repository.ProgressRepositoryImpl
import com.loorve.data.repository.ReviewBlockRepositoryImpl
import com.loorve.data.repository.ReviewScheduleRepositoryImpl
import com.loorve.domain.repository.AuthRepository
import com.loorve.domain.repository.ExamRepository
import com.loorve.domain.repository.ProgressRepository
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.domain.repository.ReviewScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 연쇄 실패 방지를 위해 ReviewSchedule 관련 바인딩을 별도 모듈로 분리
@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewScheduleModule {

    @Binds
    @Singleton
    abstract fun bindReviewScheduleRepository(
        impl: ReviewScheduleRepositoryImpl
    ): ReviewScheduleRepository
}

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
    abstract fun bindExamRepository(
        impl: ExamRepositoryImpl
    ): ExamRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(
        impl: ProgressRepositoryImpl
    ): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindReviewBlockRepository(
        impl: ReviewBlockRepositoryImpl
    ): ReviewBlockRepository
}