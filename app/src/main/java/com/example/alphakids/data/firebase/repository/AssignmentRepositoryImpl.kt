package com.example.alphakids.data.firebase.repository

import android.util.Log
import com.example.alphakids.data.firebase.models.AsignacionPalabra
import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.data.mappers.WordAssignmentMapper
import com.example.alphakids.domain.models.WordAssignment
import com.example.alphakids.domain.repository.AssignmentRepository
import com.example.alphakids.domain.repository.AssignmentResult
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import javax.inject.Inject

class AssignmentRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : AssignmentRepository {

    private val asignacionesCol = db.collection("asignaciones")
    private val estudiantesCol = db.collection("estudiantes")

    override suspend fun createAssignment(assignment: WordAssignment): AssignmentResult {
        return try {
            val resultId = db.runTransaction { transaction ->
                val existingSnapshot = transaction.get(
                    asignacionesCol
                        .whereEqualTo("id_estudiante", assignment.idEstudiante)
                        .whereEqualTo("id_palabra", assignment.idPalabra)
                        .limit(1)
                )

                if (!existingSnapshot.isEmpty) {
                    existingSnapshot.documents.first().id
                } else {
                    val newRef = asignacionesCol.document()
                    val asignacionMap = WordAssignmentMapper.fromDomain(assignment)
                    transaction.set(newRef, asignacionMap)
                    newRef.id
                }
            }.await()
            Log.d("AssignmentRepo", "Asignación creada/recuperada con ID: $resultId")
            Result.success(resultId)
        } catch (e: Exception) {
            Log.e("AssignmentRepo", "Error al crear asignación", e)
            Result.failure(e)
        }
    }

    override fun getStudentsForDocente(docenteId: String): Flow<List<Estudiante>> {
        Log.d("AssignmentRepo", "Fetching students for docente: $docenteId")
        val query: Query = estudiantesCol.whereEqualTo("id_docente", docenteId)
        return query.snapshots().map { querySnapshot ->
            querySnapshot.toObjects(Estudiante::class.java)
        }.catch { exception ->
            Log.e("AssignmentRepo", "Error in student flow for docente $docenteId", exception)
            emit(emptyList())
        }
    }

    override fun getStudentsAssignedToWord(wordId: String): Flow<List<Estudiante>> {
        val studentIdsFlow: Flow<List<String>> = asignacionesCol
            .whereEqualTo("id_palabra", wordId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.getString("id_estudiante") }
            }

        return studentIdsFlow.flatMapLatest { studentIds ->
            if (studentIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                estudiantesCol.whereIn(FieldPath.documentId(), studentIds.take(10))
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(Estudiante::class.java)
                    }
                    .catch { e ->
                        Log.e("AssignmentRepo", "Error fetching assigned students", e)
                        emit(emptyList())
                    }
            }
        }.catch { e ->
            Log.e("AssignmentRepo", "Error combining flows for assigned students", e)
            emit(emptyList())
        }
    }


    override fun getFilteredAssignmentsByStudent(
        studentId: String,
        difficulty: String?,
        query: String?
    ): Flow<List<WordAssignment>> = asignacionesCol
        .whereEqualTo("id_estudiante", studentId)
        .apply {

            if (difficulty != null && difficulty != "Todos") {
                whereEqualTo("palabra_dificultad", difficulty)
            }
        }
        .orderBy("fecha_asignacion", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->

            snapshot.toObjects(AsignacionPalabra::class.java).mapNotNull { dto ->
                val domain = WordAssignmentMapper.toDomain(dto)

                if (query.isNullOrBlank() || domain.palabraTexto.contains(query, ignoreCase = true)) {
                    domain
                } else {
                    null
                }
            }
        }
        .catch { exception ->
            Log.e("AssignmentRepo", "Error fetching assignments for student $studentId", exception)
            emit(emptyList())
        }

    override fun observePendingAssignments(studentId: String): Flow<List<WordAssignment>> {
        return asignacionesCol
            .whereEqualTo("id_estudiante", studentId)
            .whereEqualTo("estado", "PENDIENTE")
            .orderBy("fecha_asignacion", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AsignacionPalabra::class.java).map { dto ->
                    WordAssignmentMapper.toDomain(dto)
                }
            }
            .catch { exception ->
                Log.e("AssignmentRepo", "Error observing pending assignments for student $studentId", exception)
                emit(emptyList())
            }
    }

    override suspend fun decrementAssignmentAttempts(assignmentId: String, minimum: Int): Result<Int> {
        return try {
            val updatedAttempts = db.runTransaction { transaction ->
                val assignmentRef = asignacionesCol.document(assignmentId)
                val snapshot = transaction.get(assignmentRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Asignación no encontrada")
                }
                val current = (snapshot.getLong("intentos_restantes") ?: DEFAULT_ATTEMPTS.toLong()).toInt()
                val safeMinimum = max(minimum, 0)
                if (current <= safeMinimum) {
                    transaction.update(assignmentRef, "intentos_restantes", safeMinimum)
                    safeMinimum
                } else {
                    val newAttempts = (current - 1).coerceAtLeast(safeMinimum)
                    transaction.update(assignmentRef, "intentos_restantes", newAttempts)
                    newAttempts
                }
            }.await()
            Result.success(updatedAttempts)
        } catch (e: Exception) {
            Log.e("AssignmentRepo", "Error decrementing attempts for assignment $assignmentId", e)
            Result.failure(e)
        }
    }

    override suspend fun completeAssignment(
        assignmentId: String,
        studentId: String,
        rewardCoins: Int
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val assignmentRef = asignacionesCol.document(assignmentId)
                val studentRef = estudiantesCol.document(studentId)
                val snapshot = transaction.get(assignmentRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Asignación no encontrada")
                }
                val estadoActual = snapshot.getString("estado") ?: "PENDIENTE"
                if (estadoActual != "COMPLETADA") {
                    val attempts = (snapshot.getLong("intentos_restantes") ?: DEFAULT_ATTEMPTS.toLong()).toInt()
                    val normalizedAttempts = attempts.coerceAtLeast(0)
                    val updates = mapOf(
                        "estado" to "COMPLETADA",
                        "intentos_restantes" to normalizedAttempts,
                        "fecha_completado" to FieldValue.serverTimestamp()
                    )
                    transaction.set(assignmentRef, updates, SetOptions.merge())
                    if (rewardCoins > 0) {
                        transaction.update(studentRef, "coins", FieldValue.increment(rewardCoins.toLong()))
                    }
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AssignmentRepo", "Error completing assignment $assignmentId", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val DEFAULT_ATTEMPTS = 3
    }
}
