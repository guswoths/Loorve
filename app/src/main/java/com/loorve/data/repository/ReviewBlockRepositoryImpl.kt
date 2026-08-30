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
            require(reviewBlock.uid.isNotBlank()) { "uid가 비어있습니다." }

            // blockId가 비어있을 경우 Firestore에서 새 ID 자동 생성
            val docId = reviewBlock.blockId.ifBlank {
                firestore.collection("users")
                    .document(reviewBlock.uid)
                    .collection("reviewBlocks")
                    .document().id
            }

            val data = hashMapOf(
                "blockId"     to docId,
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
                .document(docId)
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
                .documents
                .mapNotNull { snapshot ->
                    // @DocumentId 제거 후 snapshot.id를 blockId에 수동 할당
                    snapshot.toObject(ReviewBlock::class.java)
                        ?.copy(blockId = snapshot.id)
                }
        }
    }

    override suspend fun deleteReviewBlock(uid: String, reviewBlockId: String): Result<Unit> {
        return runCatching {
            require(reviewBlockId.isNotBlank()) { "reviewBlockId가 비어있습니다." }
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