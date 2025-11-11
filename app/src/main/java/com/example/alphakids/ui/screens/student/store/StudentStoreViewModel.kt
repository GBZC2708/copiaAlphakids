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

    // Result wrappers
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

    // ─────────────────────────────────────────────────────────────────────────────
    // Helpers sin kotlin-reflect para extraer datos con nombres variables
    // ─────────────────────────────────────────────────────────────────────────────
    private data class SafeStudent(
        val id: String,
        val fullName: String,
        val coins: Int
    )

    private fun Any.getFieldValueOrNull(name: String): Any? {
        return runCatching {
            val cls = this::class.java
            val field = cls.declaredFields.firstOrNull { it.name == name } ?: return null
            field.isAccessible = true
            field.get(this)
        }.getOrNull()
    }

    private fun Any.readString(vararg names: String): String? {
        for (n in names) {
            val v = getFieldValueOrNull(n)
            if (v is String && v.isNotBlank()) return v
        }
        return null
    }

    private fun Any.readInt(vararg names: String): Int? {
        for (n in names) {
            when (val v = getFieldValueOrNull(n)) {
                is Int -> return v
                is Long -> return v.toInt()
                is Double -> return v.toInt()
                is Float -> return v.toInt()
                is Number -> return v.toInt()
                is String -> v.toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun toSafeStudent(raw: Any?, fallbackId: String): SafeStudent? {
        if (raw == null) return null

        val id = (raw.readString("id", "uid", "studentId", "documentId") ?: fallbackId).ifBlank { fallbackId }

        val display = raw.readString(
            "fullName", "displayName", "name", "nombreCompleto", "nombres"
        )
        val last = raw.readString("apellidos", "apellido", "lastName")
        val combined = listOfNotNull(display, last).filter { it.isNotBlank() }.joinToString(" ")
        val resolvedName = when {
            combined.isNotBlank() -> combined
            display?.isNotBlank() == true -> display
            else -> "Estudiante"
        }

        val coins = raw.readInt("coins", "monedas", "walletCoins", "balance", "puntos") ?: 0

        return SafeStudent(
            id = id,
            fullName = resolvedName,
            coins = coins.coerceAtLeast(0)
        )
    }
    // ─────────────────────────────────────────────────────────────────────────────

    // Header flow
    private val headerResult: StateFlow<HeaderResult> =
        if (studentId.isBlank()) {
            MutableStateFlow<HeaderResult>(HeaderResult.Error("No se encontró el estudiante."))
        } else {
            observeStudentUseCase(studentId)
                .map { student ->
                    val safe = toSafeStudent(student, studentId)
                    if (safe == null) {
                        HeaderResult.Error("No se encontró el estudiante.")
                    } else {
                        HeaderResult.Success(
                            StudentStoreHeader(
                                id = safe.id,
                                fullName = safe.fullName,
                                coins = safe.coins
                            )
                        )
                    }
                }
                .onStart { emit(HeaderResult.Loading) } // suspend lambda OK
                .catch { emit(HeaderResult.Error(it.message ?: "Error al cargar el estudiante.")) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = HeaderResult.Loading
                )
        }

    // Store data flow
    private val storeResult: StateFlow<StoreResult> =
        if (studentId.isBlank()) {
            MutableStateFlow<StoreResult>(StoreResult.Error("No se encontró el estudiante."))
        } else {
            combine(
                observeStoreItemsUseCase(),
                observeStudentInventoryUseCase(studentId),
                observeStudentPetUseCase(studentId)
            ) { items, inventory, pet ->
                StoreResult.Success(items, inventory, pet) as StoreResult
            }
                .onStart { emit(StoreResult.Loading) }
                .catch { emit(StoreResult.Error(it.message ?: "Error al cargar la tienda.")) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = StoreResult.Loading
                )
        }

    // UI state
    val uiState: StateFlow<StudentStoreUiState> = combine(
        headerResult,
        storeResult,
        processingItemId
    ) { header, store, processingId ->
        when (header) {
            is HeaderResult.Loading -> StudentStoreUiState.Loading
            is HeaderResult.Error -> StudentStoreUiState.Error(null, header.message)
            is HeaderResult.Success -> {
                when (store) {
                    is StoreResult.Loading -> StudentStoreUiState.Loading
                    is StoreResult.Error -> StudentStoreUiState.Error(header.header, store.message)
                    is StoreResult.Success -> {
                        val sections = buildSections(header.header, store, processingId)
                        val hasItems = sections.any { it.items.isNotEmpty() }
                        if (hasItems) {
                            StudentStoreUiState.Success(header.header, sections)
                        } else {
                            StudentStoreUiState.Empty(header.header, "No hay artículos disponibles en la tienda.")
                        }
                    }
                }
            }
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
            val sectionItems = store.items
                .filter { it.toSection() == section }
                .map { item ->
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
