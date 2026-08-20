// ✅ app/src/main/java/com/loorve/data/repository/ReviewScheduleRepositoryImpl.kt
package com.loorve.data.repository

import com.loorve.domain.model.ReviewSchedule
import com.loorve.domain.repository.ReviewScheduleRepository  // domain 레이어 인터페이스 import
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReviewScheduleRepositoryImpl @Inject constructor(
    // 필요한 DataSource / DAO 의존성 추가
    // private val reviewScheduleDao: ReviewScheduleDao,
    // private val firestore: FirebaseFirestore
) : ReviewScheduleRepository {

    override suspend fun createReviewSchedule(uid: String, schedule: ReviewSchedule): Result<Unit> {
        TODO("Implement with Firestore/Room")
    }

    override fun getReviewSchedulesByDateRange(
        uid: String, startDate: String, endDate: String
    ): Flow<List<ReviewSchedule>> {
        TODO("Implement with Firestore/Room")
    }

    override fun getTodayReviewSchedules(uid: String): Flow<List<ReviewSchedule>> {
        TODO("Implement with Firestore/Room")
    }

    override fun getOverdueAndIncompleteSchedules(uid: String): Flow<List<ReviewSchedule>> {
        TODO("Implement with Firestore/Room")
    }

    override suspend fun completeReviewSchedule(uid: String, scheduleId: String): Result<Unit> {
        TODO("Implement with Firestore/Room")
    }

    override suspend fun updateReviewCompletion(
        uid: String, scheduleId: String, isCompleted: Boolean
    ): Result<Unit> {
        TODO("Implement with Firestore/Room")
    }

    override suspend fun saveReviewSchedules(uid: String, schedules: List<ReviewSchedule>): Result<Unit> {
        TODO("Implement with Firestore batch write")
    }

    override suspend fun deleteReviewSchedule(uid: String, scheduleId: String): Result<Unit> {
        TODO("Implement with Firestore/Room")
    }
}