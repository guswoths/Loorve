package com.loorve.domain.repository

import com.loorve.domain.model.ReviewSchedule
import kotlinx.coroutines.flow.Flow

/**
 * Domain Layer - 복습 일정(ReviewSchedule) Repository 인터페이스
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 정의되는 순수 계약(Contract).
 * Firebase, Room, Retrofit 등 어떠한 외부 라이브러리에도 의존하지 않습니다.
 * 실제 구현체는 data 레이어의 ReviewScheduleRepositoryImpl에서 담당합니다.
 */
interface ReviewScheduleRepository {

    /**
     * 복습 일정을 새로 생성하고 저장합니다.
     *
     * 단어 또는 시험 학습 완료 후, 에빙하우스 망각 곡선 등의 알고리즘에 따라
     * 산출된 복습 예정일 스케줄을 저장합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     * @param schedule 저장할 [ReviewSchedule] 객체
     *
     * ⚠️ 보안 주의사항:
     *  - schedule 데이터는 서버 사이드 보안 규칙에 의해 인증된 사용자만 저장 가능해야 합니다.
     *  - scheduledDate 등의 날짜 값은 구현체에서 서버 타임스탬프로 검증하는 것을 권장합니다.
     *
     * @return 저장 성공 시 [Result.success(Unit)],
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun createReviewSchedule(uid: String, schedule: ReviewSchedule): Result<Unit>

    /**
     * 특정 날짜 또는 기간의 복습 일정 목록을 실시간으로 관찰합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     * @param startDate 조회 시작 날짜 (ISO 8601 형식, 예: "2026-08-20")
     * @param endDate 조회 종료 날짜 (ISO 8601 형식, 예: "2026-08-27")
     *
     * ⚠️ 보안 주의사항:
     *  - uid는 반드시 현재 인증된 사용자의 ID와 일치하는지
     *    구현체(서버 또는 보안 규칙)에서 검증해야 합니다.
     *
     * 해당 기간에 일정이 없으면 빈 리스트([emptyList])를 emit합니다.
     *
     * @return 기간 내 복습 일정 목록을 방출하는 [Flow]
     */
    fun getReviewSchedulesByDateRange(
        uid: String,
        startDate: String,
        endDate: String
    ): Flow<List<ReviewSchedule>>

    /**
     * 특정 복습 항목을 완료 상태로 업데이트합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     * @param scheduleId 완료 처리할 복습 일정의 고유 식별자
     *
     * ⚠️ 보안 주의사항:
     *  - 완료 처리는 해당 일정의 소유자만 가능하도록 구현체에서 접근 제어를 적용하세요.
     *  - 완료 시각(completedAt)은 클라이언트 값이 아닌 서버 타임스탬프를 사용하는 것을 권장합니다.
     *
     * @return 업데이트 성공 시 [Result.success(Unit)],
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun completeReviewSchedule(uid: String, scheduleId: String): Result<Unit>

    /**
     * 복습 완료 여부를 지정값으로 업데이트합니다 (토글 지원).
     *
     * @param uid 인증된 사용자의 고유 식별자
     * @param scheduleId 완료 처리할 복습 일정의 고유 식별자
     * @param isCompleted 변경할 완료 여부 (true=완료, false=미완료)
     *
     * ⚠️ 보안 주의사항:
     *  - 해당 일정의 소유자만 수정 가능하도록 Firestore 보안 규칙을 적용하세요.
     *
     * @return 업데이트 성공 시 [Result.success(Unit)], 실패 시 [Result.failure]
     */
    suspend fun updateReviewCompletion(
        uid: String,
        scheduleId: String,
        isCompleted: Boolean
    ): Result<Unit>

    /**
     * 특정 복습 일정 항목을 삭제합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     * @param scheduleId 삭제할 복습 일정의 고유 식별자
     *
     * ⚠️ 보안 주의사항:
     *  - 삭제는 해당 일정의 소유자만 가능하도록 구현체에서 접근 제어를 적용하세요.
     *
     * @return 삭제 성공 시 [Result.success(Unit)],
     *         실패 시 [Result.failure]와 함께 예외를 반환합니다.
     */
    suspend fun deleteReviewSchedule(uid: String, scheduleId: String): Result<Unit>

    /**
     * 복습 스케줄 목록을 배치로 저장합니다.
     * Firestore WriteBatch를 사용해 원자적으로 처리합니다.
     *
     * @param uid       인증된 사용자 UID
     * @param schedules 저장할 ReviewSchedule 리스트
     * @return 전체 저장 성공 시 [Result.success(Unit)], 실패 시 [Result.failure]
     */
    suspend fun saveReviewSchedules(uid: String, schedules: List<ReviewSchedule>): Result<Unit>

    /**
     * 오늘 날짜에 해당하는 복습 일정 목록을 실시간으로 관찰합니다.
     *
     * 앱 메인 화면의 "오늘의 복습" 섹션 등에서 사용하며,
     * 데이터 변경 시 자동으로 최신 목록을 emit합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     *
     * ⚠️ 보안 주의사항:
     *  - uid는 반드시 현재 인증된 사용자의 ID와 일치하는지
     *    구현체에서 검증해야 합니다.
     *
     * 오늘 복습 일정이 없으면 빈 리스트([emptyList])를 emit합니다.
     *
     * @return 오늘의 복습 일정 목록을 방출하는 [Flow]
     */
    fun getTodayReviewSchedules(uid: String): Flow<List<ReviewSchedule>>

    /**
     * 기한이 지났거나 아직 완료되지 않은 복습 일정 목록을 실시간으로 관찰합니다.
     *
     * 미완료 상태이거나 scheduledDate가 현재 날짜보다 이전인 항목을 반환합니다.
     * 알림 배지 표시 또는 미완료 복습 경고 UI 등에서 활용합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     *
     * ⚠️ 보안 주의사항:
     *  - uid는 반드시 현재 인증된 사용자의 ID와 일치하는지
     *    구현체에서 검증해야 합니다.
     *
     * 해당 항목이 없으면 빈 리스트([emptyList])를 emit합니다.
     *
     * @return 미완료 및 기한 초과 복습 일정 목록을 방출하는 [Flow]
     */
    fun getOverdueAndIncompleteSchedules(uid: String): Flow<List<ReviewSchedule>>
}
