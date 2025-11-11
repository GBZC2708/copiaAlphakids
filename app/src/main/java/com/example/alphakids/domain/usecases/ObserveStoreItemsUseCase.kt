package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.models.StoreItem
import com.example.alphakids.domain.repository.StoreRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStoreItemsUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    operator fun invoke(): Flow<List<StoreItem>> = repository.observeStoreItems()
}
