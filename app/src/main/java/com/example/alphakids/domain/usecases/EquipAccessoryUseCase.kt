package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.models.AccessorySlot
import com.example.alphakids.domain.repository.StoreRepository
import javax.inject.Inject

class EquipAccessoryUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    suspend operator fun invoke(
        estudianteId: String,
        itemId: String,
        slot: AccessorySlot
    ): Result<Unit> {
        return repository.equipAccessory(estudianteId, itemId, slot)
    }
}
