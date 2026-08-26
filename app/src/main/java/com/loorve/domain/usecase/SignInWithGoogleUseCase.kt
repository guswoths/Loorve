package com.loorve.domain.usecase

import com.loorve.domain.model.User
import com.loorve.domain.repository.AuthRepository

/**
 * Google Sign-In UseCase
 *
 * Google 계정의 ID 토큰을 받아 인증 처리를 수행합니다.
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 위치하며,
 * Firebase, Android 프레임워크 등 어떠한 외부 라이브러리에도 직접 의존하지 않습니다.
 * 외부 의존성은 오직 [AuthRepository] 인터페이스를 통해 역전(Inversion of Control)됩니다.
 *
 * @param authRepository 인증 관련 데이터 작업을 처리하는 [AuthRepository] 구현체 (DI 주입)
 */
class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository
) {

    /**
     * Google ID 토큰으로 로그인을 수행합니다.
     *
     * [AuthRepository.signInWithGoogle]을 호출하고,
     * 발생 가능한 모든 예외를 [Result.failure]로 래핑하여 호출자에게 전달합니다.
     *
     * @param idToken Google Sign-In 또는 Firebase Auth로부터 전달받은 ID 토큰 문자열.
     *                null이거나 빈 문자열이면 [IllegalArgumentException]으로 실패합니다.
     *
     * ⚠️ 보안 주의사항:
     *  - idToken은 민감한 인증 자격증명입니다. 절대 로그(Logcat 등)에 출력하지 마세요.
     *  - idToken의 유효기간은 일반적으로 1시간이므로 장기 저장하지 마세요.
     *
     * @return 로그인 성공 시 [Result.success]와 함께 인증된 [User]와 신규유저 여부(Boolean)를 반환,
     *         실패 시 [Result.failure]와 함께 발생한 예외를 반환합니다.
     */
    suspend operator fun invoke(idToken: String): Result<Pair<User, Boolean>> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("idToken은 비어있을 수 없습니다."))
        }
        return try {
            authRepository.signInWithGoogle(idToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}