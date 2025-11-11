package com.example.alphakids.ui.screens.student.store

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.StoreItem
import com.example.alphakids.domain.models.StoreItemMeta
import com.example.alphakids.domain.models.StoreItemType
import com.example.alphakids.domain.models.StudentInventoryItem
import com.example.alphakids.domain.models.StudentPet
import com.example.alphakids.domain.usecases.ObserveStoreItemsUseCase
import com.example.alphakids.domain.usecases.ObserveStudentInventoryUseCase
import com.example.alphakids.domain.usecases.ObserveStudentPetUseCase
import com.example.alphakids.domain.usecases.ObserveStudentUseCase
import com.example.alphakids.domain.usecases.PurchaseStoreItemUseCase
import com.example.alphakids.data.tts.TtsService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StudentStoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeStudentUseCase: ObserveStudentUseCase,
    observeStoreItemsUseCase: ObserveStoreItemsUseCase,
    observeStudentInventoryUseCase: ObserveStudentInventoryUseCase,
    observeStudentPetUseCase: ObserveStudentPetUseCase,
    private val purchaseStoreItemUseCase: PurchaseStoreItemUseCase,
    private val ttsService: TtsService
) : ViewModel() {

    private val studentId: String = savedStateHandle.get<String>("studentId").orEmpty()

    private val _events = MutableSharedFlow<StudentStoreEvent>()
    val events = _events.asSharedFlow()

    private val processingItemId = MutableStateFlow<String?>(null)

    private sealed interface HeaderResult {
        data object Loading : HeaderResult
        data class Success(val header: StudentStoreHeader) : HeaderResult
        data class Error(val message: String) : HeaderResult
    }

    private sealed interface StoreResult {
        data object Loading : StoreResult
        data class Success(
            val items: List<StoreItem>,
            val inventory: List<StudentInventoryItem>,
            val pet: StudentPet?
        ) : StoreResult
        data class Error(val message: String) : StoreResult
    }

    private val headerResult: StateFlow<HeaderResult> = if (studentId.isBlank()) {
        MutableStateFlow<HeaderResult>(HeaderResult.Error("No se encontró el estudiante."))
    } else {
        observeStudentUseCase(studentId)
            .map<HeaderResult> { student ->
                if (student == null) {
                    HeaderResult.Error("No se encontró el estudiante.")
                } else {
                    val fullName = listOf(student.nombre, student.apellido)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { "Estudiante" }
                    HeaderResult.Success(
                        StudentStoreHeader(
                            id = student.id,
                            fullName = fullName,
                            coins = student.coins.coerceAtLeast(0)
                        )
                    )
                }
            }
            .onStart { emit(HeaderResult.Loading) }
            .catch { emit(HeaderResult.Error(it.message ?: "Error al cargar el estudiante.")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HeaderResult.Loading
            )
    }

    private val storeResult: StateFlow<StoreResult> = if (studentId.isBlank()) {
        MutableStateFlow<StoreResult>(StoreResult.Error("No se encontró el estudiante."))
    } else {
        combine(
            observeStoreItemsUseCase(),
            observeStudentInventoryUseCase(studentId),
            observeStudentPetUseCase(studentId)
        ) { items, inventory, pet ->
            StoreResult.Success(items, inventory, pet)
        }
            .map<StoreResult> { it }
            .onStart { emit(StoreResult.Loading) }
            .catch { emit(StoreResult.Error(it.message ?: "Error al cargar la tienda.")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StoreResult.Loading
            )
    }

    val uiState: StateFlow<StudentStoreUiState> = combine(
        headerResult,
        storeResult,
        processingItemId
    ) { header, store, processingId ->
        when {
            header is HeaderResult.Loading || store is StoreResult.Loading -> StudentStoreUiState.Loading
            header is HeaderResult.Error -> StudentStoreUiState.Error(null, header.message)
            store is StoreResult.Error ->
                if (header is HeaderResult.Success) {
                    StudentStoreUiState.Error(header.header, store.message)
                } else {
                    StudentStoreUiState.Error(null, store.message)
                }
            header is HeaderResult.Success && store is StoreResult.Success -> {
                val sections = buildSections(header.header, store, processingId)
                val hasItems = sections.any { it.items.isNotEmpty() }
                if (hasItems) {
                    StudentStoreUiState.Success(header.header, sections)
                } else {
                    StudentStoreUiState.Empty(header.header, "No hay artículos disponibles en la tienda.")
                }
            }
            else -> StudentStoreUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudentStoreUiState.Loading
    )

    fun retry() {
        if (studentId.isBlank()) {
            viewModelScope.launch {
                _events.emit(StudentStoreEvent.Message("No se encontró el estudiante."))
            }
        }
    }

    fun purchase(itemId: String) {
        val currentStudent = studentId
        if (currentStudent.isBlank()) {
            viewModelScope.launch {
                _events.emit(StudentStoreEvent.Message("No se encontró el estudiante."))
            }
            return
        }
        if (processingItemId.value != null) return
        processingItemId.value = itemId
        viewModelScope.launch {
            val result = purchaseStoreItemUseCase(currentStudent, itemId)
            result.onSuccess {
                val message = "Compra realizada con éxito."
                _events.emit(StudentStoreEvent.Message(message))
                ttsService.speak("store_$itemId", message)
            }.onFailure {
                val message = it.message ?: "No se pudo completar la compra."
                _events.emit(StudentStoreEvent.Message(message))
            }
            processingItemId.value = null
        }
    }

    private fun buildSections(
        header: StudentStoreHeader,
        store: StoreResult.Success,
        processingId: String?
    ): List<StudentStoreSection> {
        val inventoryMap = store.inventory.associateBy { it.itemId }
        return StoreSection.values().map { section ->
            val sectionItems = store.items.filter { it.toSection() == section }.map { item ->
                val meta = item.meta
                val canAfford = header.coins >= item.price.coerceAtLeast(0)
                val restriction = when (section) {
                    StoreSection.PETS -> petRestriction(store.pet, meta)
                    StoreSection.ACCESSORIES -> accessoryRestriction(store.pet, meta)
                    StoreSection.CONSUMABLES -> null
                }
                val reason = when {
                    restriction != null -> restriction
                    !canAfford -> "Monedas insuficientes"
                    meta is StoreItemMeta.Unknown -> "No disponible"
                    else -> null
                }
                val allowed = restriction == null && canAfford && meta !is StoreItemMeta.Unknown
                val quantity = inventoryMap[item.id]?.quantity?.coerceAtLeast(0) ?: 0
                StudentStoreItemUi(
                    id = item.id,
                    name = item.name,
                    price = item.price.coerceAtLeast(0),
                    section = section,
                    meta = meta,
                    canPurchase = allowed,
                    restrictionMessage = reason,
                    quantity = quantity,
                    isProcessing = processingId == item.id
                )
            }
            StudentStoreSection(section, sectionItems)
        }
    }

    private fun StoreItem.toSection(): StoreSection = when (type) {
        StoreItemType.PET -> StoreSection.PETS
        StoreItemType.ACCESSORY -> StoreSection.ACCESSORIES
        StoreItemType.CONSUMABLE -> StoreSection.CONSUMABLES
    }

    private fun petRestriction(pet: StudentPet?, meta: StoreItemMeta): String? {
        val petMeta = meta as? StoreItemMeta.Pet ?: return "No disponible"
        val currentKind = pet?.petKind
        return if (currentKind != null && currentKind == petMeta.petKind) {
            "Ya tienes esta mascota"
        } else null
    }

    private fun accessoryRestriction(pet: StudentPet?, meta: StoreItemMeta): String? {
        val accessoryMeta = meta as? StoreItemMeta.Accessory ?: return "No disponible"
        val currentPet = pet ?: return "Necesitas una mascota"
        val slot = accessoryMeta.slot
        val equipped = currentPet.equippedAccessories[slot]
        return if (!equipped.isNullOrBlank()) {
            "Espacio ocupado"
        } else null
    }
}
