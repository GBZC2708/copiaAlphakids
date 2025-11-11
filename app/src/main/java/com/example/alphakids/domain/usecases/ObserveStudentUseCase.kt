package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.StudentRepository
import javax.inject.Inject

class ObserveStudentUseCase @Inject constructor(
    private val repository: StudentRepository
) {
    operator fun invoke(studentId: String) = repository.observeStudent(studentId)
}
