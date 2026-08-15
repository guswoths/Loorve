package com.loorve.domain.usecase

import com.loorve.domain.repository.AuthRepository

/**
 * 로그아웃 UseCase
 *
 * 현재 로그인된 사용자를 로그아웃 처리합니다.
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 위치하며,
 * Firebase, Android 프레임워크 등 어떠한 외부 라이브러리에도 직접 의존하지 않습니다.
 * 외부 의존성은 오직 [AuthRepository] 인터페이스를 통해 역전(Inversion of Control)됩니다.
 *
 * @param authRepository 인증 관련 데이터 작업을 처리하는 [AuthRepository] 구현체 (DI 주입)
 */
class SignOutUseCase(
    private val authRepository: AuthRepository
) {

    /**
     * 현재 로그인된 사용자를 로그아웃합니다.
     *
     * 기존 [AuthRepository.logout]을 호출하고,
     * 발생 가능한 모든 예외를 [Result.failure]로 래핑하여 호출자에게 전달합니다.
     *
     * ⚠️ 보안 주의사항:
     *  - 로그아웃 완료 후 ViewModel 및 UI에서 보유 중인 사용자 상태(캐시 포함)를
     *    반드시 초기화하여 데이터 잔존을 방지하세요.
     *  - 로그아웃 실패 시 UI에서 적절한 에러 처리 후 재시도 옵션을 제공하세요.
     *
     * @return 로그아웃 성공 시 [Result.success(Unit)],
     *         실패 시 [Result.failure]와 함께 발생한 예외를 반환합니다.
     */
    suspend operator fun invoke(): Result<Unit> {
        return try {
            authRepository.logout()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
