// app/src/main/java/com/loorve/domain/repository/ReviewBlockRepository.kt
package com.loorve.domain.repository

import com.loorve.domain.model.ReviewBlock
import kotlinx.coroutines.flow.Flow

/**
 * Domain Layer - 복습 블록(ReviewBlock) Repository 인터페이스
 *
 * 클린 아키텍처 원칙에 따라 domain 레이어에 정의되는 순수 계약(Contract).
 * 실제 구현체는 data 레이어의 ReviewBlockRepositoryImpl에서 담당합니다.
 */
interface ReviewBlockRepository {

    /**
     * 특정 날짜의 복습 블록 목록을 실시간으로 관찰합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     * @param date 조회할 날짜 (ISO 8601 형식, 예: "2026-08-30")
     * @return 해당 날짜의 복습 블록 목록을 방출하는 [Flow]
     */
    fun getReviewBlocksByDate(uid: String, date: String): Flow<List<ReviewBlock>>

    /**
     * 복습 블록을 저장합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     * @param reviewBlock 저장할 [ReviewBlock] 객체
     * @return 저장 성공 시 [Result.success(Unit)], 실패 시 [Result.failure]
     */
    suspend fun saveReviewBlock(uid: String, reviewBlock: ReviewBlock): Result<Unit>

    /**
     * 특정 복습 블록을 삭제합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     * @param reviewBlockId 삭제할 복습 블록의 고유 식별자
     * @return 삭제 성공 시 [Result.success(Unit)], 실패 시 [Result.failure]
     */
    suspend fun deleteReviewBlock(uid: String, reviewBlockId: String): Result<Unit>

    /**
     * 날짜 범위 내 복습 블록 목록을 실시간으로 관찰합니다.
     *
     * @param uid 인증된 사용자의 고유 식별자
     * @param startDate 시작 날짜 (ISO 8601 형식)
     * @param endDate 종료 날짜 (ISO 8601 형식)
     * @return 해당 기간의 복습 블록 목록을 방출하는 [Flow]
     */
    fun getReviewBlocksByDateRange(
        uid: String,
        startDate: String,
        endDate: String
    ): Flow<List<ReviewBlock>>
}