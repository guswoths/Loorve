package com.loorve.di

import com.loorve.data.repository.AuthRepositoryImpl
import com.loorve.data.repository.ExamRepositoryImpl
import com.loorve.data.repository.ProgressRepositoryImpl
import com.loorve.data.repository.ReviewScheduleRepositoryImpl  // ✅ 추가
import com.loorve.domain.repository.AuthRepository
import com.loorve.domain.repository.ExamRepository
import com.loorve.domain.repository.ProgressRepository
import com.loorve.domain.repository.ReviewScheduleRepository    // ✅ 추가
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
    abstract fun bindProgressRepository(
        impl: ProgressRepositoryImpl
    ): ProgressRepository

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

    // ✅ 추가
    @Binds
    @Singleton
    abstract fun bindReviewScheduleRepository(
        impl: ReviewScheduleRepositoryImpl
    ): ReviewScheduleRepository
}
