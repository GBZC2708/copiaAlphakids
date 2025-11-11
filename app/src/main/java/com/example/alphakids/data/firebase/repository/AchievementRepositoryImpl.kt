package com.example.alphakids.data.firebase.repository

import android.util.Log
import com.example.alphakids.domain.models.Achievement
import com.example.alphakids.domain.repository.AchievementRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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
                "createdAtMillis" to achievement.createdAtMillis,
                "imageUrl" to achievement.imageUrl
            )
            collection.document(documentId).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AchievementRepo", "Error creating achievement", e)
            Result.failure(e)
        }
    }

    override fun observeStudentAchievements(studentId: String): Flow<List<Achievement>> {
        return collection
            .whereEqualTo("estudianteId", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document -> document.toAchievement() }
                    .sortedByDescending { it.createdAtMillis }
            }
            .catch { error ->
                Log.e("AchievementRepo", "Error observing achievements for student $studentId", error)
                emit(emptyList())
            }
    }

    private fun DocumentSnapshot.toAchievement(): Achievement? {
        val name = getString("nombre").orEmpty()
        if (name.isBlank()) return null
        val description = getString("descripcion").orEmpty()
        val coins = (getLong("coins") ?: 0L).toInt().coerceAtLeast(0)
        val studentId = getString("estudianteId").orEmpty()
        val createdAtMillis = when {
            getLong("createdAtMillis") != null -> getLong("createdAtMillis")!!.coerceAtLeast(0L)
            getTimestamp("createdAt") != null -> getTimestamp("createdAt")!!.toMillis().coerceAtLeast(0L)
            else -> 0L
        }
        val imageUrl = getString("imageUrl")
        return Achievement(
            id = id,
            studentId = studentId,
            name = name,
            description = description,
            coins = coins,
            createdAtMillis = createdAtMillis,
            imageUrl = imageUrl
        )
    }

    private fun Timestamp.toMillis(): Long {
        return toDate().time
    }
}
