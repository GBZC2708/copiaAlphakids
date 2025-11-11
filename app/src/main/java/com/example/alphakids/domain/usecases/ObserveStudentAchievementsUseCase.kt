package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.AchievementRepository
import javax.inject.Inject

class ObserveStudentAchievementsUseCase @Inject constructor(
    private val repository: AchievementRepository
) {
    operator fun invoke(studentId: String) = repository.observeStudentAchievements(studentId)
}
