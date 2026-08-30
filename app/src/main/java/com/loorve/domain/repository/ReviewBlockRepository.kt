// app/src/main/java/com/loorve/domain/repository/ReviewBlockRepository.kt
package com.loorve.domain.repository

import com.loorve.domain.model.ReviewBlock

interface ReviewBlockRepository {
    // ✅ 파라미터 타입을 String → ReviewBlock 으로 수정
    suspend fun saveReviewBlock(reviewBlock: ReviewBlock): Result<Unit>
    suspend fun getReviewBlocks(uid: String): Result<List<ReviewBlock>>
    suspend fun deleteReviewBlock(uid: String, reviewBlockId: String): Result<Unit>
}