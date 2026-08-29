// app/src/main/java/com/loorve/di/ReviewScheduleModule.kt
package com.loorve.di

import com.loorve.data.repository.ReviewScheduleRepositoryImpl
import com.loorve.domain.repository.ReviewScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewScheduleModule {

    @Binds
    @Singleton
    abstract fun bindReviewScheduleRepository(
        impl: ReviewScheduleRepositoryImpl
    ): ReviewScheduleRepository
}