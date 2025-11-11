package com.example.alphakids.domain.usecases

import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.domain.repository.StudentRepository
import javax.inject.Inject

class UpdateStudentUseCase @Inject constructor(
    private val repository: StudentRepository
) {
    suspend operator fun invoke(estudiante: Estudiante) = repository.updateStudent(estudiante)
}
