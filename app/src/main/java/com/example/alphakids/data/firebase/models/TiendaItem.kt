package com.example.alphakids.data.firebase.models

data class TiendaItem(
    val name: String = "",
    val price: Int = 0,
    val type: String = "",
    val meta: Map<String, Any?>? = null
)

data class EstudianteInventario(
    val estudianteId: String = "",
    val itemId: String = "",
    val qty: Int = 0
)

data class EstudianteMascota(
    val estudianteId: String = "",
    val petKind: String = "",
    val hunger: Int = 0,
    val happiness: Int = 0,
    val equippedAccessories: Map<String, String>? = null
)
