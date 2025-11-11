package com.example.alphakids.ui.screens.student.store

import com.example.alphakids.domain.models.StoreItemMeta

enum class StoreSection(val title: String) {
    PETS("Mascotas"),
    ACCESSORIES("Accesorios"),
    CONSUMABLES("Consumibles")
}

data class StudentStoreHeader(
    val id: String,
    val fullName: String,
    val coins: Int
)

data class StudentStoreItemUi(
    val id: String,
    val name: String,
    val price: Int,
    val section: StoreSection,
    val meta: StoreItemMeta,
    val canPurchase: Boolean,
    val restrictionMessage: String?,
    val quantity: Int,
    val isProcessing: Boolean
)

data class StudentStoreSection(
    val section: StoreSection,
    val items: List<StudentStoreItemUi>
)

sealed interface StudentStoreUiState {
    data object Loading : StudentStoreUiState
    data class Error(val header: StudentStoreHeader?, val message: String) : StudentStoreUiState
    data class Empty(val header: StudentStoreHeader, val message: String) : StudentStoreUiState
    data class Success(
        val header: StudentStoreHeader,
        val sections: List<StudentStoreSection>
    ) : StudentStoreUiState
}

sealed interface StudentStoreEvent {
    data class Message(val text: String) : StudentStoreEvent
}
