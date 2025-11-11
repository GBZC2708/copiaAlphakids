package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.AssignmentRepository
import javax.inject.Inject

class DecrementAssignmentAttemptsUseCase @Inject constructor(
    private val repository: AssignmentRepository
) {
    suspend operator fun invoke(assignmentId: String, minimum: Int = 0) =
        repository.decrementAssignmentAttempts(assignmentId, minimum)
}
