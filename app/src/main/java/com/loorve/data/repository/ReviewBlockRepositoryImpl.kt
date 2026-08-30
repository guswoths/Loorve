package com.loorve.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.domain.model.ReviewBlock
import com.loorve.domain.repository.ReviewBlockRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewBlockRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewBlockRepository {

    override suspend fun saveReviewBlock(reviewBlock: ReviewBlock): Result<Unit> {
        return runCatching {
            firestore
                .collection("users")
                .document(reviewBlock.uid)
                .collection("reviewBlocks")
                .document(reviewBlock.blockId)  // ✅ reviewBlockId → blockId 로 수정
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