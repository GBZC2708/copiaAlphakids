package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.models.StudentPet
import com.example.alphakids.domain.repository.StoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStudentPetUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    operator fun invoke(estudianteId: String): Flow<StudentPet?> =
        repository.observeStudentPet(estudianteId)
}
