package com.example.alphakids.data.firebase.repository

import com.example.alphakids.data.firebase.FirestoreTransactionHelper
import com.example.alphakids.data.firebase.models.EstudianteInventario
import com.example.alphakids.data.firebase.models.EstudianteMascota
import com.example.alphakids.data.firebase.models.TiendaItem
import com.example.alphakids.data.mappers.toDomain
import com.example.alphakids.domain.models.AccessorySlot
import com.example.alphakids.domain.models.StoreItem
import com.example.alphakids.domain.models.StudentInventoryItem
import com.example.alphakids.domain.models.StudentPet
import com.example.alphakids.domain.repository.StoreRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val transactionHelper: FirestoreTransactionHelper
) : StoreRepository {

    private val storeCollection = firestore.collection("tiendaItems")
    private val inventoryCollection = firestore.collection("estudianteInventario")
    private val petsCollection = firestore.collection("estudianteMascotas")

    override fun observeStoreItems(): Flow<List<StoreItem>> {
        return storeCollection.orderBy("price", Query.Direction.ASCENDING).snapshots().map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(TiendaItem::class.java)?.toDomain(doc.id)
            }
        }.catch { emit(emptyList()) }
    }

    override fun observeInventory(estudianteId: String): Flow<List<StudentInventoryItem>> {
        return inventoryCollection
            .whereEqualTo("estudianteId", estudianteId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(EstudianteInventario::class.java)?.toDomain()
                }.filter { it.quantity > 0 }
            }
            .catch { emit(emptyList()) }
    }

    override fun observeStudentPet(estudianteId: String): Flow<StudentPet?> {
        return petsCollection
            .whereEqualTo("estudianteId", estudianteId)
            .limit(1)
            .snapshots()
            .map { snapshot ->
                val document = snapshot.documents.firstOrNull()
                document?.toObject(EstudianteMascota::class.java)?.toDomain(document.id)
            }
            .catch { emit(null) }
    }

    override suspend fun purchaseItem(
        estudianteId: String,
        itemId: String,
        quantity: Int
    ): Result<Unit> {
        return runCatching {
            transactionHelper.purchaseStoreItem(estudianteId, itemId, quantity)
        }
    }

    override suspend fun equipAccessory(
        estudianteId: String,
        itemId: String,
        slot: AccessorySlot
    ): Result<Unit> {
        return runCatching {
            firestore.runTransaction { transaction ->
                val petRef = petsCollection.document(estudianteId)
                val inventoryRef = inventoryCollection.document("${estudianteId}_$itemId")
                val snapshot = transaction.get(petRef)
                val inventorySnapshot = transaction.get(inventoryRef)
                if (!snapshot.exists()) throw IllegalStateException("Mascota no encontrada")
                if (!inventorySnapshot.exists()) throw IllegalStateException("No tienes este accesorio")
                val currentQty = inventorySnapshot.getLong("qty") ?: 0L
                if (currentQty <= 0L) throw IllegalStateException("No tienes este accesorio")
                val equipped = (snapshot.get("equippedAccessories") as? Map<String, String?>)?.toMutableMap() ?: mutableMapOf()
                val slotKey = slot.name
                equipped[slotKey] = itemId
                transaction.update(petRef, mapOf("equippedAccessories" to equipped))
                null
            }.await()
        }
    }

    override suspend fun consumeItem(
        estudianteId: String,
        itemId: String,
        quantity: Int
    ): Result<Unit> {
        return runCatching {
            transactionHelper.consumeConsumable(estudianteId, itemId, quantity)
        }
    }

    override suspend fun feedPet(
        estudianteId: String,
        itemId: String,
        hungerDelta: Int,
        happinessDelta: Int
    ): Result<Unit> {
        return runCatching {
            firestore.runTransaction { transaction ->
                val petRef = petsCollection.document(estudianteId)
                val inventoryRef = inventoryCollection.document("${estudianteId}_$itemId")
                val petSnapshot = transaction.get(petRef)
                if (!petSnapshot.exists()) throw IllegalStateException("Mascota no encontrada")
                val inventorySnapshot = transaction.get(inventoryRef)
                if (!inventorySnapshot.exists()) throw IllegalStateException("No tienes este artículo")
                val currentQty = inventorySnapshot.getLong("qty") ?: 0L
                if (currentQty <= 0L) throw IllegalStateException("No tienes este artículo")
                val currentHunger = petSnapshot.getLong("hunger")?.toInt() ?: 0
                val currentHappiness = petSnapshot.getLong("happiness")?.toInt() ?: 0
                if (currentHunger >= 100 && currentHappiness >= 100) {
                    throw IllegalStateException("Mascota llena")
                }
                val newHunger = (currentHunger + hungerDelta).coerceIn(0, 100)
                val newHappiness = (currentHappiness + happinessDelta).coerceIn(0, 100)
                transaction.update(
                    petRef,
                    mapOf(
                        "hunger" to newHunger,
                        "happiness" to newHappiness
                    )
                )
                transaction.update(inventoryRef, "qty", FieldValue.increment(-1))
                null
            }.await()
        }
    }
}
