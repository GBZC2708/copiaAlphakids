package com.example.alphakids.data.firebase.repository

import android.util.Log
import com.example.alphakids.domain.models.Achievement
import com.example.alphakids.domain.repository.AchievementRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AchievementRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : AchievementRepository {

    private val collection = db.collection("logros")

    override suspend fun addAchievement(achievement: Achievement): Result<Unit> {
        return try {
            val documentId = achievement.id.ifBlank { collection.document().id }
            val data = mapOf(
                "nombre" to achievement.name,
                "descripcion" to achievement.description,
                "coins" to achievement.coins.coerceAtLeast(0),
                "estudianteId" to achievement.studentId,
                "createdAt" to FieldValue.serverTimestamp(),
                "createdAtMillis" to achievement.createdAtMillis
            )
            collection.document(documentId).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AchievementRepo", "Error creating achievement", e)
            Result.failure(e)
        }
    }
}
