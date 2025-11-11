package com.example.alphakids.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTransactionHelper @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun rewardStudentCoins(studentId: String, amount: Int) {
        require(amount >= 0)
        firestore.runTransaction { transaction ->
            val studentRef = firestore.collection("estudiantes").document(studentId)
            val snapshot = transaction.get(studentRef)
            val currentCoins = snapshot.getLong("coins") ?: 0L
            val updated = currentCoins + amount
            transaction.update(studentRef, "coins", FieldValue.increment(amount.toLong()))
            null
        }.await()
    }

    suspend fun purchaseStoreItem(studentId: String, itemId: String, quantity: Int = 1) {
        require(quantity > 0)
        firestore.runTransaction { transaction ->
            val studentRef = firestore.collection("estudiantes").document(studentId)
            val itemRef = firestore.collection("tiendaItems").document(itemId)
            val studentSnapshot = transaction.get(studentRef)
            val itemSnapshot = transaction.get(itemRef)
            if (!itemSnapshot.exists()) throw IllegalStateException("Ítem no encontrado")
            val price = itemSnapshot.getLong("price")?.toInt() ?: throw IllegalStateException("Precio inválido")
            val type = itemSnapshot.getString("type") ?: throw IllegalStateException("Tipo inválido")
            val meta = itemSnapshot.get("meta") as? Map<*, *> ?: emptyMap<String, Any>()
            val currentCoins = studentSnapshot.getLong("coins")?.toInt() ?: 0
            val totalCost = price * quantity
            val updatedCoins = currentCoins - totalCost
            if (updatedCoins < 0) throw IllegalStateException("Monedas insuficientes")
            when (type) {
                "PET" -> handlePetPurchase(transaction, studentId, meta)
                "ACCESSORY" -> handleAccessoryPurchase(transaction, studentId, itemId, meta)
                "CONSUMABLE" -> handleConsumablePurchase(transaction, studentId, itemId, quantity)
                else -> throw IllegalStateException("Tipo de ítem desconocido")
            }
            transaction.update(studentRef, "coins", FieldValue.increment(-totalCost.toLong()))
            null
        }.await()
    }

    suspend fun consumeConsumable(studentId: String, itemId: String, quantity: Int = 1) {
        require(quantity > 0)
        firestore.runTransaction { transaction ->
            val inventoryRef = firestore.collection("estudianteInventario").document("${studentId}_$itemId")
            val snapshot = transaction.get(inventoryRef)
            if (!snapshot.exists()) throw IllegalStateException("Inventario inexistente")
            val currentQty = snapshot.getLong("qty") ?: 0L
            val newQty = currentQty - quantity
            if (newQty < 0) throw IllegalStateException("Inventario insuficiente")
            transaction.update(inventoryRef, "qty", FieldValue.increment(-quantity.toLong()))
            null
        }.await()
    }

    private fun handlePetPurchase(
        transaction: com.google.firebase.firestore.Transaction,
        studentId: String,
        meta: Map<*, *>
    ) {
        val petKind = meta["petKind"] as? String ?: throw IllegalStateException("petKind faltante")
        val petRef = firestore.collection("estudianteMascotas").document(studentId)
        val snapshot = transaction.get(petRef)
        if (snapshot.exists()) {
            val existingKind = snapshot.getString("petKind")
            if (existingKind == petKind) throw IllegalStateException("Mascota duplicada")
        }
        val equipped = snapshot.get("equippedAccessories") as? Map<String, String?> ?: emptyMap()
        val hunger = (snapshot.getLong("hunger") ?: 50L).coerceIn(0, 100)
        val happiness = (snapshot.getLong("happiness") ?: 60L).coerceIn(0, 100)
        transaction.set(
            petRef,
            mapOf(
                "estudianteId" to studentId,
                "petKind" to petKind,
                "hunger" to hunger,
                "happiness" to happiness,
                "equippedAccessories" to equipped
            ),
            SetOptions.merge()
        )
    }

    private fun handleAccessoryPurchase(
        transaction: com.google.firebase.firestore.Transaction,
        studentId: String,
        itemId: String,
        meta: Map<*, *>
    ) {
        val slot = meta["accessorySlot"] as? String ?: throw IllegalStateException("Slot faltante")
        val petRef = firestore.collection("estudianteMascotas").document(studentId)
        val snapshot = transaction.get(petRef)
        if (!snapshot.exists()) throw IllegalStateException("Mascota no encontrada")
        val equipped = (snapshot.get("equippedAccessories") as? Map<String, String?>)?.toMutableMap() ?: mutableMapOf()
        if (!equipped[slot].isNullOrEmpty()) throw IllegalStateException("Slot ocupado")
        equipped[slot] = itemId
        transaction.update(petRef, mapOf("equippedAccessories" to equipped))
    }

    private fun handleConsumablePurchase(
        transaction: com.google.firebase.firestore.Transaction,
        studentId: String,
        itemId: String,
        quantity: Int
    ) {
        val inventoryRef = firestore.collection("estudianteInventario").document("${studentId}_$itemId")
        val snapshot = transaction.get(inventoryRef)
        val currentQty = snapshot.getLong("qty") ?: 0L
        val newQty = currentQty + quantity
        if (snapshot.exists()) {
            transaction.update(inventoryRef, "qty", FieldValue.increment(quantity.toLong()))
        } else {
            transaction.set(
                inventoryRef,
                mapOf(
                    "estudianteId" to studentId,
                    "itemId" to itemId,
                    "qty" to newQty
                ),
                SetOptions.merge()
            )
        }
    }
}
