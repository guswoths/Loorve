package com.loorve.domain.repository

import com.loorve.domain.model.ReviewBlock
import kotlinx.coroutines.flow.Flow

interface ReviewBlockRepository {
    suspend fun saveReviewBlock(reviewBlock: ReviewBlock): Result<Unit>
    suspend fun getReviewBlocks(uid: String): Result<List<ReviewBlock>>
    suspend fun getReviewBlock(uid: String, blockId: String): Result<ReviewBlock?>
    suspend fun deleteReviewBlock(uid: String, reviewBlockId: String): Result<Unit>
    
    /**
     * 복습 블록을 실시간으로 관찰합니다.
     * Firestore 스냅샷 리스너를 통해 블록 생성, 삭제, 수정을 즉시 반영합니다.
     */
    fun observeReviewBlocks(uid: String): Flow<List<ReviewBlock>>
}