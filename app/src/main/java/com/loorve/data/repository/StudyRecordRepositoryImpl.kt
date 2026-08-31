package com.loorve.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.loorve.data.model.StudyRecordDto
import com.loorve.data.model.toDto
import com.loorve.domain.model.StudyRecord
import com.loorve.domain.repository.StudyRecordRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StudyRecordRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : StudyRecordRepository {

    // 경로: users/{uid}/studyRecords/{studyRecordId}
    private fun studyRecordsRef(uid: String) =
        firestore.collection("users").document(uid).collection("studyRecords")

    override suspend fun saveStudyRecord(record: StudyRecord): Result<String> = runCatching {
        val uid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(record.uid == uid) { "본인의 학습기록만 저장할 수 있습니다." }

        val dto = record.toDto()
        val docRef = if (record.id.isBlank()) {
            studyRecordsRef(uid).document()
        } else {
            studyRecordsRef(uid).document(record.id)
        }

        val data = hashMapOf(
            "id" to docRef.id,
            "uid" to dto.uid,
            "blockId" to dto.blockId,
            "examId" to dto.examId,
            "title" to dto.title,
            "content" to dto.content,
            "learningDate" to dto.learningDate,
            "examDate" to dto.examDate,
            "prepStartDate" to dto.prepStartDate,
            "recommendedCompletionDate" to dto.recommendedCompletionDate,
            "stage" to dto.stage,
            "successCount" to dto.successCount,
            "stability" to dto.stability,
            "plannedReviewCount" to dto.plannedReviewCount,
            "completedReviewCount" to dto.completedReviewCount,
            "isAtRisk" to dto.isAtRisk,
            // 서버 시간 우선 사용
            "createdAt" to if (record.id.isBlank()) FieldValue.serverTimestamp()
            else FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(data).await()
        docRef.id
    }

    override suspend fun getStudyRecords(
        uid: String,
        blockId: String
    ): Result<List<StudyRecord>> = runCatching {
        val currentUid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(currentUid == uid) { "본인의 학습기록만 조회할 수 있습니다." }

        studyRecordsRef(uid)
            .whereEqualTo("blockId", blockId)
            .get().await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(StudyRecordDto::class.java)
                    ?.copy(id = doc.id)
                    ?.toDomain()
            }
    }

    override suspend fun updateStudyRecord(record: StudyRecord): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid
            ?: throw SecurityException("인증되지 않은 사용자입니다.")
        require(record.uid == uid) { "본인의 학습기록만 수정할 수 있습니다." }

        studyRecordsRef(uid).document(record.id)
            .update(
                mapOf(
                    "stage" to record.stage,
                    "successCount" to record.successCount,
                    "stability" to record.stability,
                    "completedReviewCount" to record.completedReviewCount,
                    "isAtRisk" to record.isAtRisk,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }
}