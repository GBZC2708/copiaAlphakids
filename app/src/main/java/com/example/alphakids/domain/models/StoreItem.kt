package com.example.alphakids.domain.models

enum class StoreItemType {
    PET,
    ACCESSORY,
    CONSUMABLE
}

enum class PetKind {
    GATO,
    PERRO,
    UNIVERSAL
}

enum class AccessorySlot {
    HEAD,
    NECK,
    BACK
}

enum class ConsumableKind {
    CROQUETA,
    HUESO
}

sealed class StoreItemMeta {
    data class Pet(val petKind: PetKind) : StoreItemMeta()
    data class Accessory(
        val petKind: PetKind,
        val slot: AccessorySlot
    ) : StoreItemMeta()
    data class Consumable(val kind: ConsumableKind) : StoreItemMeta()
    object Unknown : StoreItemMeta()
}

data class StoreItem(
    val id: String,
    val name: String,
    val price: Int,
    val type: StoreItemType,
    val meta: StoreItemMeta
)

data class StudentInventoryItem(
    val estudianteId: String,
    val itemId: String,
    val quantity: Int
)

data class StudentPet(
    val estudianteId: String,
    val petKind: PetKind,
    val hunger: Int,
    val happiness: Int,
    val equippedAccessories: Map<AccessorySlot, String>
) {
    init {
        require(hunger in 0..100)
        require(happiness in 0..100)
        require(equippedAccessories.values.none { it.isEmpty() })
    }
}
