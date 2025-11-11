package com.example.alphakids.domain.repository

import com.example.alphakids.domain.models.Achievement
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    suspend fun addAchievement(achievement: Achievement): Result<Unit>
    fun observeStudentAchievements(studentId: String): Flow<List<Achievement>>
}
