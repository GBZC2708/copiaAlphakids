package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.AssignmentRepository
import javax.inject.Inject

class CompleteAssignmentUseCase @Inject constructor(
    private val repository: AssignmentRepository
) {
    suspend operator fun invoke(
        assignmentId: String,
        studentId: String,
        rewardCoins: Int
    ) = repository.completeAssignment(assignmentId, studentId, rewardCoins)
}
