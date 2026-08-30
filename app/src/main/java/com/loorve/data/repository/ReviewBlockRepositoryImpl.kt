// app/src/main/java/com/loorve/data/repository/ReviewBlockRepositoryImpl.kt
package com.loorve.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.repository.ReviewBlockRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewBlockRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewBlockRepository {

    // ✅ 파라미터 타입을 ReviewBlock으로 수정
    override suspend fun saveReviewBlock(reviewBlock: ReviewBlock): Result<Unit> {
        return runCatching {
            firestore
                .collection("users")
                .document(reviewBlock.uid)
                .collection("reviewBlocks")
                .document(reviewBlock.reviewBlockId)
                .set(reviewBlock)
                .await()
        }
    }

    override suspend fun getReviewBlocks(uid: String): Result<List<ReviewBlock>> {
        return runCatching {
            firestore
                .collection("users")
                .document(uid)
                .collection("reviewBlocks")
                .get()
                .await()
                .toObjects(ReviewBlock::class.java)
        }
    }

    override suspend fun deleteReviewBlock(uid: String, reviewBlockId: String): Result<Unit> {
        return runCatching {
            firestore
                .collection("users")
                .document(uid)
                .collection("reviewBlocks")
                .document(reviewBlockId)
                .delete()
                .await()
        }
    }
}