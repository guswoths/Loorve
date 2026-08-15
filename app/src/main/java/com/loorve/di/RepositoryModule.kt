package com.loorve.di

import com.loorve.data.repository.AuthRepositoryImpl
import com.loorve.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 도메인 Repository 인터페이스 ↔ 구현체 바인딩 모듈.
 * 새 Repository 추가 시 이 파일에 @Binds abstract fun을 추가합니다.
 *
 * [확장 예시]
 * @Binds @Singleton
 * abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
 *
 * @Binds @Singleton
 * abstract fun bindCoupleRepository(impl: CoupleRepositoryImpl): CoupleRepository
 *
 * NOTE: CredentialManager는 Activity Context가 필요하므로 Singleton 바인딩 금지.
 *       loginWithGoogle(activityContext) 호출부에서 직접 생성할 것.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
