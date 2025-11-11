package com.example.alphakids.ui.screens.student.pets

import com.example.alphakids.domain.models.AccessorySlot
import com.example.alphakids.domain.models.ConsumableKind
import com.example.alphakids.domain.models.PetKind

data class StudentPetsHeader(
    val id: String,
    val fullName: String,
    val coins: Int
)

data class EquippedAccessoryUi(
    val slot: AccessorySlot,
    val name: String?
)

data class StudentPetCardUi(
    val petKind: PetKind,
    val hunger: Int,
    val happiness: Int,
    val equipped: List<EquippedAccessoryUi>
)

data class ConsumableActionUi(
    val itemId: String,
    val name: String,
    val quantity: Int,
    val hungerDelta: Int,
    val happinessDelta: Int,
    val kind: ConsumableKind,
    val isProcessing: Boolean
)

data class AccessoryOptionUi(
    val itemId: String,
    val name: String,
    val slot: AccessorySlot,
    val isEquipped: Boolean,
    val isProcessing: Boolean
)

sealed interface StudentPetsUiState {
    data object Loading : StudentPetsUiState
    data class Error(val header: StudentPetsHeader?, val message: String) : StudentPetsUiState
    data class Empty(val header: StudentPetsHeader, val message: String) : StudentPetsUiState
    data class Success(
        val header: StudentPetsHeader,
        val pets: List<StudentPetCardUi>,
        val consumables: List<ConsumableActionUi>,
        val accessories: List<AccessoryOptionUi>
    ) : StudentPetsUiState
}

sealed interface StudentPetsEvent {
    data class Message(val text: String) : StudentPetsEvent
}
