package com.example.alphakids.data.firebase.repository

import android.util.Log
import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.domain.repository.CreateStudentResult
import com.example.alphakids.domain.repository.StudentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StudentRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : StudentRepository {

    private val estudiantesCol = db.collection("estudiantes")

    override suspend fun createStudent(estudiante: Estudiante): CreateStudentResult {
        return try {
            val documentReference = estudiantesCol.add(estudiante).await()
            Log.d("StudentRepo", "Estudiante creado con ID: ${documentReference.id}")
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Log.e("StudentRepo", "Error al crear estudiante", e)
            Result.failure(e)
        }
    }

    override fun getStudentsForTutor(tutorId: String): Flow<List<Estudiante>> {
        Log.d("StudentRepo", "Fetching students for tutor ID: $tutorId")
        val query: Query = estudiantesCol.whereEqualTo("id_tutor", tutorId)
        return query.snapshots().map { querySnapshot ->
            Log.d("StudentRepo", "Snapshot received. Documents found: ${querySnapshot.size()}")
            if (querySnapshot.metadata.hasPendingWrites()) {
                Log.d("StudentRepo", "Snapshot has pending writes.")
            }
            querySnapshot.toObjects(Estudiante::class.java)
        }
    }

    override fun observeStudent(studentId: String): Flow<Estudiante?> {
        return estudiantesCol.document(studentId).snapshots().map { snapshot ->
            snapshot.toObject(Estudiante::class.java)
        }
    }

    override suspend fun updateStudent(estudiante: Estudiante): Result<Unit> {
        return try {
            estudiantesCol.document(estudiante.id).set(estudiante, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("StudentRepo", "Error updating student ${estudiante.id}", e)
            Result.failure(e)
        }
    }
}
