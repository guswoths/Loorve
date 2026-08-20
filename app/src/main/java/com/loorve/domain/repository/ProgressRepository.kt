package com.loorve.domain.repository

interface ProgressRepository {
    suspend fun getProgress(userId: String): Int
    suspend fun updateProgress(userId: String, progress: Int)
}
