package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.models.Achievement
import com.example.alphakids.domain.repository.AchievementRepository
import javax.inject.Inject

class CreateAchievementUseCase @Inject constructor(
    private val repository: AchievementRepository
) {
    suspend operator fun invoke(achievement: Achievement) = repository.addAchievement(achievement)
}
