package com.loorve.di

import com.loorve.data.repository.AuthRepositoryImpl
import com.loorve.data.repository.ExamRepositoryImpl
import com.loorve.data.repository.ProgressRepositoryImpl
import com.loorve.data.repository.ReviewBlockRepositoryImpl
import com.loorve.data.repository.ReviewScheduleItemRepositoryImpl
import com.loorve.data.repository.StudyRecordRepositoryImpl
import com.loorve.domain.repository.AuthRepository
import com.loorve.domain.repository.ExamRepository
import com.loorve.domain.repository.ProgressRepository
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.domain.repository.ReviewScheduleItemRepository
import com.loorve.domain.repository.StudyRecordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindExamRepository(impl: ExamRepositoryImpl): ExamRepository

    @Binds @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds @Singleton
    abstract fun bindReviewBlockRepository(impl: ReviewBlockRepositoryImpl): ReviewBlockRepository

    @Binds @Singleton
    abstract fun bindStudyRecordRepository(impl: StudyRecordRepositoryImpl): StudyRecordRepository

    @Binds @Singleton
    abstract fun bindReviewScheduleItemRepository(
        impl: ReviewScheduleItemRepositoryImpl
    ): ReviewScheduleItemRepository

    // ✅ ReviewScheduleRepository는 ReviewScheduleModule.kt에서 이미 바인딩됨 — 여기 없어야 함
}