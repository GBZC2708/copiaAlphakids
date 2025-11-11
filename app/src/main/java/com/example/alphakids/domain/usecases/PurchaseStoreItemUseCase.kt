package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.repository.StoreRepository
import javax.inject.Inject

class PurchaseStoreItemUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    suspend operator fun invoke(estudianteId: String, itemId: String, quantity: Int = 1): Result<Unit> {
        require(quantity > 0)
        return repository.purchaseItem(estudianteId, itemId, quantity)
    }
}
