package com.loorve.di

import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.data.repository.ProgressRepositoryImpl
import com.loorve.domain.repository.ProgressRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
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
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
