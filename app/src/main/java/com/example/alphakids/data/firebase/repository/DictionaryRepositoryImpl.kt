package com.example.alphakids.data.firebase.repository

import com.example.alphakids.data.firebase.FirestoreTransactionHelper
import com.example.alphakids.data.firebase.models.DictionaryProgressRecord
import com.example.alphakids.data.firebase.models.TeacherDictionaryEntry
import com.example.alphakids.data.mappers.TeacherDictionaryMapper
import com.example.alphakids.domain.models.TeacherDictionaryWord
import com.example.alphakids.domain.repository.DictionaryRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DictionaryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val transactionHelper: FirestoreTransactionHelper
) : DictionaryRepository {

    private val dictionaryCollection = firestore.collection("diccionarioDocente")
    private val progressCollection = firestore.collection("diccionarioProgreso")

    override fun observePendingWords(studentId: String, teacherId: String): Flow<List<TeacherDictionaryWord>> {
        val wordsFlow = dictionaryCollection
            .whereEqualTo("docenteId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    document.toObject(TeacherDictionaryEntry::class.java)?.let(TeacherDictionaryMapper::toDomain)
                }
            }
            .catch { emit(emptyList()) }

        val progressFlow = progressCollection
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(DictionaryProgressRecord::class.java)?.wordId }
                    .toSet()
            }
            .catch { emit(emptySet()) }

        return combine(wordsFlow, progressFlow) { words, completed ->
            words.filterNot { completed.contains(it.id) }
        }.catch { emit(emptyList()) }
    }

    override suspend fun rewardStudentForWord(
        studentId: String,
        teacherId: String,
        wordId: String,
        rewardCoins: Int
    ): Result<Unit> {
        if (studentId.isBlank() || wordId.isBlank()) {
            return Result.failure(IllegalArgumentException("Datos incompletos"))
        }
        if (rewardCoins < 0) {
            return Result.failure(IllegalArgumentException("Recompensa inválida"))
        }
        return runCatching {
            if (rewardCoins > 0) {
                transactionHelper.rewardStudentCoins(studentId, rewardCoins)
            }
            val progressId = "${studentId}_$wordId"
            val data = mapOf(
                "studentId" to studentId,
                "wordId" to wordId,
                "teacherId" to teacherId,
                "completedAt" to FieldValue.serverTimestamp()
            )
            progressCollection.document(progressId).set(data, SetOptions.merge()).await()
        }
    }
}
