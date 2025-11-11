package com.example.alphakids.domain.repository

import com.example.alphakids.domain.models.Achievement

interface AchievementRepository {
    suspend fun addAchievement(achievement: Achievement): Result<Unit>
}
