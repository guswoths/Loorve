package com.loorve.domain.repository

import com.loorve.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Domain Layer - 인증(Auth) Repository 인터페이스
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 정의되는 순수 계약(Contract).
 * Firebase, Room, Retrofit 등 어떠한 외부 라이브러리에도 의존하지 않습니다.
 * 실제 구현체는 data 레이어의 AuthRepositoryImpl에서 담당합니다.
 */
interface AuthRepository {

    /**
     * 이메일과 비밀번호로 로그인합니다.
     *
     * @param email 사용자의 이메일 주소
     * @param password 사용자의 비밀번호
     *
     * ⚠️ 보안 주의사항:
     *  - password 파라미터는 절대 로그(Logcat 등)에 출력하지 마세요.
     *  - 이 인터페이스의 구현체에서도 password를 데이터 클래스 필드나
     *    로컬 변수에 불필요하게 저장하지 마세요.
     *  - 네트워크 전송 시 반드시 HTTPS를 사용하세요.
     *
     * @return 로그인 성공 시 [Result.success]와 함께 [User]를 반환,
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * 현재 로그인된 사용자를 로그아웃합니다.
     *
     * @return 로그아웃 성공 시 [Result.success(Unit)],
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun logout(): Result<Unit>

    /**
     * 현재 로그인 상태의 사용자를 실시간으로 관찰합니다.
     *
     * 로그인 상태이면 [User] 객체를 emit하고,
     * 로그아웃 상태이거나 인증 정보가 없으면 null을 emit합니다.
     * 인증 상태가 변경될 때마다 자동으로 새 값을 emit합니다.
     *
     * @return 인증 상태를 방출하는 [Flow]. 로그인: [User], 미로그인: null
     */
    fun getCurrentUser(): Flow<User?>

    /**
     * Google 계정의 ID 토큰으로 로그인합니다.
     *
     * @param idToken Google Sign-In 또는 Firebase Auth로부터 전달받은 ID 토큰 문자열
     *
     * ⚠️ 보안 주의사항:
     *  - idToken은 민감한 인증 자격증명입니다. 절대 로그(Logcat 등)에 출력하지 마세요.
     *  - idToken의 유효기간은 1시간이므로 장기 저장하지 마세요.
     *  - 네트워크 전송 시 반드시 HTTPS를 사용하세요.
     *
     * @return 로그인 성공 시 [Result.success]와 함께 [User]를 반환,
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun launchGoogleSignIn(activityContext: Context): Result<User>
}
