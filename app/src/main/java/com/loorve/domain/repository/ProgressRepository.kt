package com.loorve.domain.repository

import com.loorve.domain.model.ProgressEntity
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {

    suspend fun saveProgress(uid: String, progress: ProgressEntity): Result<Unit>

    suspend fun getProgressById(uid: String, progressId: String): Result<ProgressEntity>

    suspend fun getProgressList(uid: String): Result<List<ProgressEntity>>

    suspend fun deleteProgress(uid: String, progressId: String): Result<Unit>

    fun observeProgressList(uid: String): Flow<List<ProgressEntity>>
}
