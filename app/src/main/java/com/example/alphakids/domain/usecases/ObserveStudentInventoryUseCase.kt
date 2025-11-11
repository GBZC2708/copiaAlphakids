package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.models.StudentInventoryItem
import com.example.alphakids.domain.repository.StoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStudentInventoryUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    operator fun invoke(estudianteId: String): Flow<List<StudentInventoryItem>> =
        repository.observeInventory(estudianteId)
}
