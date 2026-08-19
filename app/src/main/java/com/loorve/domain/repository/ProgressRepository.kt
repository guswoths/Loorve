package com.loorve.domain.repository

import com.loorve.domain.model.Progress
import kotlinx.coroutines.flow.Flow

/**
 * Domain Layer - 학습 진도(Progress) Repository 인터페이스
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 정의되는 순수 계약(Contract).
 * Firebase, Room, Retrofit 등 어떠한 외부 라이브러리에도 의존하지 않습니다.
 * 실제 구현체는 data 레이어의 ProgressRepositoryImpl에서 담당합니다.
 */
interface ProgressRepository {

    /**
     * 특정 사용자의 학습 진도 목록을 실시간으로 관찰합니다.
     *
     * 데이터가 변경될 때마다 최신 목록을 자동으로 emit합니다.
     * 진도 기록이 없으면 빈 리스트([emptyList])를 emit합니다.
     *
     * ⚠️ 보안 주의사항:
     *  - userId는 반드시 현재 인증된 사용자의 ID와 일치하는지
     *    구현체(서버 또는 보안 규칙)에서 검증해야 합니다.
     *  - 타 사용자의 진도를 무단으로 조회할 수 없도록 접근 제어를 적용하세요.
     *
     * @param userId 조회할 사용자의 고유 식별자
     * @return 해당 사용자의 학습 진도 목록을 방출하는 [Flow]
     */
    fun getProgressList(userId: String): Flow<List<Progress>>

    /**
     * 학습 진도를 저장하거나 업데이트합니다.
     *
     * [Progress.id]가 비어 있으면 새 문서를 생성(auto-id)하고,
     * 값이 있으면 해당 문서를 덮어씁니다(upsert).
     *
     * ⚠️ 보안 주의사항:
     *  - progress 내 userId는 서버 사이드 보안 규칙에서 현재 인증된 사용자의
     *    uid와 일치하는지 반드시 검증해야 합니다.
     *  - 클라이언트에서 전달된 [Progress.createdAt] 값은 구현체에서
     *    서버 타임스탬프로 대체하는 것을 권장합니다.
     *  - 네트워크 전송 시 반드시 HTTPS를 사용하세요.
     *
     * @param userId 진도를 저장할 사용자의 고유 식별자
     * @param progress 저장 또는 업데이트할 [Progress] 객체
     * @return 저장 성공 시 [Result.success(Unit)],
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun saveProgress(userId: String, progress: Progress): Result<Unit>

    /**
     * 특정 과목(시험)의 진도율을 조회합니다.
     *
     * 진도율은 0.0(0%)~1.0(100%) 범위의 [Float] 값으로 반환됩니다.
     * 해당 과목에 진도 기록이 없으면 0.0을 반환합니다.
     *
     * ⚠️ 확인 필요:
     *  - 현재 [Progress] 도메인 모델에는 '과목(subject)' 및 '완료 여부' 필드가
     *    없습니다. [Progress.examId]를 과목 식별자로 대체하여 사용하거나,
     *    [Progress] 모델에 관련 필드를 추가하는 것을 권장합니다.
     *
     * ⚠️ 보안 주의사항:
     *  - userId는 현재 인증된 사용자의 ID와 일치하는지 구현체에서 검증해야 합니다.
     *
     * @param userId 조회할 사용자의 고유 식별자
     * @param examId 진도율을 조회할 시험(과목)의 고유 식별자
     * @return 조회 성공 시 [Result.success]와 함께 진도율([Float], 0.0~1.0)을 반환,
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun getProgressRate(userId: String, examId: String): Result<Float>

    /**
     * 특정 사용자의 전체 학습 완료 여부를 확인합니다.
     *
     * 모든 등록된 시험에 대해 진도가 100%에 도달했을 경우 true를 반환합니다.
     * 진도 기록이 하나도 없는 경우 false를 반환합니다.
     *
     * ⚠️ 확인 필요:
     *  - '완료' 기준(예: 진도율 1.0, 특정 필드 값 등)을 구현체에서
     *    명확히 정의해야 합니다. 현재 [Progress] 모델에 완료 여부 플래그가
     *    없으므로, 모델 확장 또는 구현체 내 로직으로 처리해야 합니다.
     *
     * ⚠️ 보안 주의사항:
     *  - userId는 현재 인증된 사용자의 ID와 일치하는지 구현체에서 검증해야 합니다.
     *
     * @param userId 완료 여부를 확인할 사용자의 고유 식별자
     * @return 완료 여부 확인 성공 시 [Result.success]와 함께 완료 여부([Boolean])를 반환,
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun isAllCompleted(userId: String): Result<Boolean>
}
