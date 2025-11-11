package com.example.alphakids.domain.repository

import com.example.alphakids.data.firebase.models.Estudiante
import kotlinx.coroutines.flow.Flow

typealias CreateStudentResult = Result<String>

interface StudentRepository {
    suspend fun createStudent(estudiante: Estudiante): CreateStudentResult
    fun getStudentsForTutor(tutorId: String): Flow<List<Estudiante>>
    fun observeStudent(studentId: String): Flow<Estudiante?>
    suspend fun updateStudent(estudiante: Estudiante): Result<Unit>

}
