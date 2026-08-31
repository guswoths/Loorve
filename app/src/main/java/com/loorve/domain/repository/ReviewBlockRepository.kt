package com.loorve.domain.repository

import com.loorve.domain.model.ReviewBlock

interface ReviewBlockRepository {
    suspend fun saveReviewBlock(reviewBlock: ReviewBlock): Result<Unit>
    suspend fun getReviewBlocks(uid: String): Result<List<ReviewBlock>>
    suspend fun getReviewBlock(uid: String, blockId: String): Result<ReviewBlock?>
    suspend fun deleteReviewBlock(uid: String, reviewBlockId: String): Result<Unit>
}