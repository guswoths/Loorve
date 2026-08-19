// app/src/main/java/com/loorve/data/repository/ExamRepositoryImpl.kt
package com.loorve.data.repository

import com.loorve.domain.repository.ExamRepository
import javax.inject.Inject  // ← 반드시 존재해야 함

class ExamRepositoryImpl @Inject constructor(
    // 필요한 의존성 (예: ExamDao, FirebaseFirestore 등)
) : ExamRepository {

    // ExamRepository 인터페이스 구현 메서드들...
}
