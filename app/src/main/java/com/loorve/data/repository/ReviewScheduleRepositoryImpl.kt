package com.loorve.data.repository

import com.loorve.domain.repository.ReviewScheduleRepository
import javax.inject.Inject

class ReviewScheduleRepositoryImpl @Inject constructor(
    // 필요한 DataSource / DAO 의존성을 여기에 추가
    // 예: private val reviewScheduleDao: ReviewScheduleDao
) : ReviewScheduleRepository {

    // ReviewScheduleRepository 인터페이스의 추상 함수를 여기에 구현
    // 예:
    // override suspend fun getSchedules(): List<ReviewSchedule> { ... }
}
