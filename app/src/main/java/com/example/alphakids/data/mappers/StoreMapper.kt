package com.example.alphakids.data.mappers

import com.example.alphakids.data.firebase.models.EstudianteInventario
import com.example.alphakids.data.firebase.models.EstudianteMascota
import com.example.alphakids.data.firebase.models.TiendaItem
import com.example.alphakids.domain.models.AccessorySlot
import com.example.alphakids.domain.models.ConsumableKind
import com.example.alphakids.domain.models.PetKind
import com.example.alphakids.domain.models.StoreItem
import com.example.alphakids.domain.models.StoreItemMeta
import com.example.alphakids.domain.models.StoreItemType
import com.example.alphakids.domain.models.StudentInventoryItem
import com.example.alphakids.domain.models.StudentPet

fun TiendaItem.toDomain(id: String): StoreItem {
    val safePrice = price.coerceAtLeast(0)
    val metaMap = meta.orEmpty()
    val itemType = runCatching { StoreItemType.valueOf(type) }.getOrElse { StoreItemType.CONSUMABLE }
    val meta = when (itemType) {
        StoreItemType.PET -> metaMap["petKind"]
            ?.toString()
            ?.let { runCatching { PetKind.valueOf(it) }.getOrNull() }
            ?.let(StoreItemMeta::Pet)
            ?: StoreItemMeta.Unknown
        StoreItemType.ACCESSORY -> {
            val petKind = metaMap["petKind"]
                ?.toString()
                ?.let { runCatching { PetKind.valueOf(it) }.getOrNull() }
                ?: PetKind.UNIVERSAL
            val slot = metaMap["accessorySlot"]
                ?.toString()
                ?.let { runCatching { AccessorySlot.valueOf(it) }.getOrNull() }
            if (slot != null) {
                StoreItemMeta.Accessory(petKind, slot)
            } else {
                StoreItemMeta.Unknown
            }
        }
        StoreItemType.CONSUMABLE -> metaMap["consumableKind"]
            ?.toString()
            ?.let { runCatching { ConsumableKind.valueOf(it) }.getOrNull() }
            ?.let(StoreItemMeta::Consumable)
            ?: StoreItemMeta.Unknown
    }
    return StoreItem(
        id = id,
        name = name,
        price = safePrice,
        type = itemType,
        meta = meta
    )
}

fun EstudianteInventario.toDomain(): StudentInventoryItem {
    val safeQty = qty.coerceAtLeast(0)
    return StudentInventoryItem(
        estudianteId = estudianteId,
        itemId = itemId,
        quantity = safeQty
    )
}

fun EstudianteMascota.toDomain(id: String): StudentPet? {
    if (petKind.isBlank()) return null
    val kind = runCatching { PetKind.valueOf(petKind) }.getOrElse { return null }
    val hungerValue = hunger.coerceIn(0, 100)
    val happinessValue = happiness.coerceIn(0, 100)
    val accessories = equippedAccessories.orEmpty().mapNotNull { (slot, itemId) ->
        val accessorySlot = runCatching { AccessorySlot.valueOf(slot) }.getOrNull()
        if (accessorySlot != null && !itemId.isNullOrBlank()) {
            accessorySlot to itemId
        } else {
            null
        }
    }.toMap()
    return StudentPet(
        estudianteId = id,
        petKind = kind,
        hunger = hungerValue,
        happiness = happinessValue,
        equippedAccessories = accessories
    )
}
