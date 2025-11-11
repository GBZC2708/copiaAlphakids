package com.example.alphakids.domain.repository

import com.example.alphakids.domain.models.AccessorySlot
import com.example.alphakids.domain.models.StoreItem
import com.example.alphakids.domain.models.StudentInventoryItem
import com.example.alphakids.domain.models.StudentPet
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun observeStoreItems(): Flow<List<StoreItem>>
    fun observeInventory(estudianteId: String): Flow<List<StudentInventoryItem>>
    fun observeStudentPet(estudianteId: String): Flow<StudentPet?>
    suspend fun purchaseItem(estudianteId: String, itemId: String, quantity: Int = 1): Result<Unit>
    suspend fun equipAccessory(estudianteId: String, itemId: String, slot: AccessorySlot): Result<Unit>
    suspend fun consumeItem(estudianteId: String, itemId: String, quantity: Int = 1): Result<Unit>
}
