package com.loorve.di

import com.loorve.data.repository.ReviewBlockRepositoryImpl
import com.loorve.data.repository.ReviewScheduleRepositoryImpl
import com.loorve.domain.repository.ReviewBlockRepository
import com.loorve.domain.repository.ReviewScheduleRepository
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
    abstract fun bindReviewBlockRepository(
        impl: ReviewBlockRepositoryImpl
    ): ReviewBlockRepository

    @Binds
    @Singleton
    abstract fun bindReviewScheduleRepository(
        impl: ReviewScheduleRepositoryImpl
    ): ReviewScheduleRepository
}