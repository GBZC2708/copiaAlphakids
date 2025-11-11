package com.example.alphakids.data.firebase.repository

import com.example.alphakids.data.firebase.models.Docente
import com.example.alphakids.data.mappers.TeacherMapper
import com.example.alphakids.domain.models.Teacher
import com.example.alphakids.domain.repository.TeacherRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TeacherRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : TeacherRepository {

    private val docentesCol = db.collection("docentes")

    override fun observeTeachers(): Flow<List<Teacher>> {
        return docentesCol.snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { document ->
                document.toObject(Docente::class.java)?.let(TeacherMapper::toDomain)
            }
        }
    }

    override fun observeTeacherById(id: String): Flow<Teacher?> {
        return docentesCol.document(id).snapshots().map { document ->
            document.toObject(Docente::class.java)?.let(TeacherMapper::toDomain)
        }
    }
}
