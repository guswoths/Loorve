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
            // ✅ @DocumentId 필드가 직렬화에서 제외되는 문제 해결:
            // ReviewBlock 객체 대신 명시적 HashMap으로 저장하여
            // Firestore Rules의 hasAll(['blockId', 'uid', 'createdAt'])를 충족
            val data = hashMapOf(
                "blockId"     to reviewBlock.blockId,
                "uid"         to reviewBlock.uid,
                "date"        to reviewBlock.date,
                "title"       to reviewBlock.title,
                "description" to reviewBlock.description,
                "isCompleted" to reviewBlock.isCompleted,
                "createdAt"   to reviewBlock.createdAt,
                "updatedAt"   to reviewBlock.updatedAt
            )

            firestore
                .collection("users")
                .document(reviewBlock.uid)
                .collection("reviewBlocks")
                .document(reviewBlock.blockId)
                .set(data)
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