package com.loorve.domain.repository

import com.loorve.domain.model.Exam
import com.loorve.domain.model.ExamResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain Layer - 시험(Exam) Repository 인터페이스
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 정의되는 순수 계약(Contract).
 * Firebase, Room, Retrofit 등 어떠한 외부 라이브러리에도 의존하지 않습니다.
 * 실제 구현체는 data 레이어의 ExamRepositoryImpl에서 담당합니다.
 */
interface ExamRepository {

    /**
     * 전체 시험 목록을 실시간으로 관찰합니다.
     *
     * 데이터가 변경될 때마다 최신 목록을 자동으로 emit합니다.
     * 목록이 비어 있으면 빈 리스트([emptyList])를 emit합니다.
     *
     * @return 시험 목록을 방출하는 [Flow]
     */
    fun getExamList(): Flow<List<Exam>>

    /**
     * 특정 시험의 상세 정보를 실시간으로 관찰합니다.
     *
     * @param examId 조회할 시험의 고유 식별자
     *
     * 해당 ID의 시험이 존재하지 않을 경우, 구현체는 적절한 예외를 발생시켜야 합니다.
     *
     * @return 해당 시험의 상세 정보를 방출하는 [Flow]
     */
    fun getExamById(examId: String): Flow<Exam>

    /**
     * 시험 결과를 저장합니다.
     *
     * @param result 저장할 [ExamResult] 객체
     *
     * ⚠️ 보안 주의사항:
     *  - result 내 사용자 식별 정보는 서버 사이드에서 검증되어야 합니다.
     *  - 민감한 채점 데이터가 클라이언트에서 임의로 조작되지 않도록
     *    구현체(서버 또는 보안 규칙)에서 반드시 검증 로직을 포함하세요.
     *  - 네트워크 전송 시 반드시 HTTPS를 사용하세요.
     *
     * @return 저장 성공 시 [Result.success(Unit)],
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun saveExamResult(result: ExamResult): Result<Unit>

    /**
     * 특정 사용자의 시험 결과 목록을 실시간으로 관찰합니다.
     *
     * @param userId 조회할 사용자의 고유 식별자
     *
     * ⚠️ 보안 주의사항:
     *  - userId는 반드시 현재 인증된 사용자의 ID와 일치하는지
     *    구현체(서버 또는 보안 규칙)에서 검증해야 합니다.
     *  - 타 사용자의 결과를 무단으로 조회할 수 없도록 접근 제어를 적용하세요.
     *
     * 결과가 없으면 빈 리스트([emptyList])를 emit합니다.
     *
     * @return 해당 사용자의 시험 결과 목록을 방출하는 [Flow]
     */

    fun getExamResults(userId: String): Flow<List<ExamResult>>

    /**
     * 새로운 시험을 저장합니다.
     *
     * @param exam 저장할 [Exam] 객체
     *
     * ⚠️ 보안 주의사항:
     *  - exam 데이터는 서버 사이드 보안 규칙에 의해 인증된 사용자만 저장 가능해야 합니다.
     *  - 클라이언트에서 전달된 examDate 등의 값은 구현체에서 서버 타임스탬프로 대체하는 것을 권장합니다.
     *
     * @return 저장 성공 시 [Result.success(Unit)],
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun addExam(exam: Exam): Result<Unit>
}
