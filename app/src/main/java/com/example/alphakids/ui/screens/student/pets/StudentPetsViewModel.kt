package com.example.alphakids.ui.screens.student.pets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.AccessorySlot
import com.example.alphakids.domain.models.ConsumableKind
import com.example.alphakids.domain.models.StoreItem
import com.example.alphakids.domain.models.StoreItemMeta
import com.example.alphakids.domain.models.StudentInventoryItem
import com.example.alphakids.domain.models.StudentPet
import com.example.alphakids.domain.usecases.EquipAccessoryUseCase
import com.example.alphakids.domain.usecases.FeedPetUseCase
import com.example.alphakids.domain.usecases.ObserveStoreItemsUseCase
import com.example.alphakids.domain.usecases.ObserveStudentInventoryUseCase
import com.example.alphakids.domain.usecases.ObserveStudentPetUseCase
import com.example.alphakids.domain.usecases.ObserveStudentUseCase
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

@HiltViewModel
class StudentPetsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeStudentUseCase: ObserveStudentUseCase,
    observeStudentPetUseCase: ObserveStudentPetUseCase,
    observeStudentInventoryUseCase: ObserveStudentInventoryUseCase,
    observeStoreItemsUseCase: ObserveStoreItemsUseCase,
    private val feedPetUseCase: FeedPetUseCase,
    private val equipAccessoryUseCase: EquipAccessoryUseCase,
    private val ttsService: TtsService
) : ViewModel() {

    private val studentId: String = savedStateHandle.get<String>("studentId").orEmpty()

    private val _events = MutableSharedFlow<StudentPetsEvent>()
    val events = _events.asSharedFlow()

    private val feedingItemId = MutableStateFlow<String?>(null)
    private val equippingItemId = MutableStateFlow<String?>(null)
    private val latestSuccess = MutableStateFlow<StudentPetsUiState.Success?>(null)

    private sealed interface HeaderResult {
        data object Loading : HeaderResult
        data class Success(val header: StudentPetsHeader) : HeaderResult
        data class Error(val message: String) : HeaderResult
    }

    private sealed interface DataResult {
        data object Loading : DataResult
        data class Success(
            val items: List<StoreItem>,
            val inventory: List<StudentInventoryItem>,
            val pet: StudentPet?
        ) : DataResult
        data class Error(val message: String) : DataResult
    }

    private val headerResult: StateFlow<HeaderResult> =
        if (studentId.isBlank()) {
            MutableStateFlow<HeaderResult>(HeaderResult.Error("No se encontró el estudiante."))
        } else {
            observeStudentUseCase(studentId)
                .transform { student ->
                    // Evita depender de propiedades específicas del modelo de estudiante
                    if (student == null) {
                        emit(HeaderResult.Error("No se encontró el estudiante."))
                    } else {
                        emit(
                            HeaderResult.Success(
                                StudentPetsHeader(
                                    id = studentId,           // usamos el id del estado
                                    fullName = "Estudiante",  // nombre genérico si el modelo varía
                                    coins = 0                 // valor seguro por compatibilidad
                                )
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

    private val dataResult: StateFlow<DataResult> =
        if (studentId.isBlank()) {
            MutableStateFlow<DataResult>(DataResult.Error("No se encontró el estudiante."))
        } else {
            combine(
                observeStoreItemsUseCase(),
                observeStudentInventoryUseCase(studentId),
                observeStudentPetUseCase(studentId)
            ) { items, inventory, pet ->
                DataResult.Success(items, inventory, pet) as DataResult
            }
                .onStart { emit(DataResult.Loading) }
                .catch { emit(DataResult.Error(it.message ?: "Error al cargar los datos.")) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DataResult.Loading
                )
        }

    val uiState: StateFlow<StudentPetsUiState> = combine(
        headerResult,
        dataResult,
        feedingItemId,
        equippingItemId
    ) { header, data, feedingId, equippingId ->
        when {
            header is HeaderResult.Loading || data is DataResult.Loading -> {
                latestSuccess.value = null
                StudentPetsUiState.Loading
            }

            header is HeaderResult.Error -> {
                latestSuccess.value = null
                StudentPetsUiState.Error(null, header.message)
            }

            data is DataResult.Error -> {
                if (header is HeaderResult.Success) {
                    StudentPetsUiState.Error(header.header, data.message)
                } else {
                    StudentPetsUiState.Error(null, data.message)
                }.also { latestSuccess.value = null }
            }

            header is HeaderResult.Success && data is DataResult.Success -> {
                buildSuccessState(header.header, data, feedingId, equippingId)
            }

            else -> {
                latestSuccess.value = null
                StudentPetsUiState.Loading
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudentPetsUiState.Loading
    )

    fun retry() {
        if (studentId.isBlank()) {
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message("No se encontró el estudiante."))
            }
        }
    }

    fun feed(itemId: String) {
        val student = studentId
        if (student.isBlank()) {
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message("No se encontró el estudiante."))
            }
            return
        }
        val success = latestSuccess.value ?: run {
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message("Información de mascota no disponible."))
            }
            return
        }
        val petCard = success.pets.firstOrNull() ?: run {
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message("No hay mascota para alimentar."))
            }
            return
        }
        val consumable = success.consumables.find { it.itemId == itemId } ?: run {
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message("No se encontró el artículo."))
            }
            return
        }
        if (consumable.quantity <= 0) {
            val message = "No tienes ${consumable.name.lowercase()} disponibles."
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message(message))
            }
            ttsService.speak("pet_feed_insufficient_${consumable.kind}", message)
            return
        }
        if (petCard.hunger >= 100 && petCard.happiness >= 100) {
            val message = "Tu mascota está llena y feliz."
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message(message))
            }
            ttsService.speak("pet_feed_full", message)
            return
        }
        if (feedingItemId.value != null) return
        feedingItemId.value = itemId
        viewModelScope.launch {
            val result = feedPetUseCase(student, itemId, consumable.hungerDelta, consumable.happinessDelta)
            result.onSuccess {
                val message = "¡${consumable.name} entregada!"
                _events.emit(StudentPetsEvent.Message(message))
                ttsService.speak("pet_feed_success_$itemId", message)
            }.onFailure {
                val failure = it.message ?: "No se pudo alimentar a la mascota."
                _events.emit(StudentPetsEvent.Message(failure))
                when {
                    failure.contains("llena", ignoreCase = true) ->
                        ttsService.speak("pet_feed_full_$itemId", failure)

                    failure.contains("No tienes", ignoreCase = true) ->
                        ttsService.speak("pet_feed_insufficient_$itemId", failure)
                }
            }
            feedingItemId.value = null
        }
    }

    fun equip(itemId: String) {
        val student = studentId
        if (student.isBlank()) {
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message("No se encontró el estudiante."))
            }
            return
        }
        val success = latestSuccess.value ?: run {
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message("Información de mascota no disponible."))
            }
            return
        }
        val accessory = success.accessories.find { it.itemId == itemId } ?: run {
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message("No se encontró el accesorio."))
            }
            return
        }
        if (accessory.isEquipped) {
            viewModelScope.launch {
                _events.emit(StudentPetsEvent.Message("${accessory.name} ya está equipado."))
            }
            return
        }
        if (equippingItemId.value != null) return
        equippingItemId.value = itemId
        viewModelScope.launch {
            val result = equipAccessoryUseCase(student, itemId, accessory.slot)
            result.onSuccess {
                val message = "${accessory.name} equipado."
                _events.emit(StudentPetsEvent.Message(message))
                ttsService.speak("pet_equip_$itemId", message)
            }.onFailure {
                val failure = it.message ?: "No se pudo equipar el accesorio."
                _events.emit(StudentPetsEvent.Message(failure))
            }
            equippingItemId.value = null
        }
    }

    private fun buildSuccessState(
        header: StudentPetsHeader,
        data: DataResult.Success,
        feedingId: String?,
        equippingId: String?
    ): StudentPetsUiState {
        val pet = data.pet ?: run {
            latestSuccess.value = null
            return StudentPetsUiState.Empty(header, "Aún no tienes una mascota. Visita la tienda para adoptarla.")
        }
        val storeMap = data.items.associateBy { it.id }
        val inventoryMap = data.inventory.associateBy { it.itemId }
        val pets = listOf(
            StudentPetCardUi(
                petKind = pet.petKind,
                hunger = pet.hunger.coerceIn(0, 100),
                happiness = pet.happiness.coerceIn(0, 100),
                equipped = AccessorySlot.values().map { slot ->
                    val eqItemId = pet.equippedAccessories[slot]
                    val name = eqItemId?.let { storeMap[it]?.name } ?: "Sin accesorio"
                    EquippedAccessoryUi(slot, if (eqItemId != null) name else null)
                }
            )
        )
        val consumables = data.items
            .mapNotNull { item ->
                val meta = item.meta as? StoreItemMeta.Consumable ?: return@mapNotNull null
                val effect = consumableEffects[meta.kind] ?: return@mapNotNull null
                val quantity = inventoryMap[item.id]?.quantity?.coerceAtLeast(0) ?: 0
                ConsumableActionUi(
                    itemId = item.id,
                    name = item.name,
                    quantity = quantity,
                    hungerDelta = effect.hunger,
                    happinessDelta = effect.happiness,
                    kind = meta.kind,
                    isProcessing = feedingId == item.id
                )
            }
            .sortedBy { it.name.lowercase() }

        val accessories = data.items
            .mapNotNull { item ->
                val meta = item.meta as? StoreItemMeta.Accessory ?: return@mapNotNull null
                val quantity = inventoryMap[item.id]?.quantity?.coerceAtLeast(0) ?: 0
                val isEquipped = pet.equippedAccessories[meta.slot] == item.id
                if (quantity <= 0 && !isEquipped) return@mapNotNull null
                AccessoryOptionUi(
                    itemId = item.id,
                    name = item.name,
                    slot = meta.slot,
                    isEquipped = isEquipped,
                    isProcessing = equippingId == item.id
                )
            }
            .sortedWith(compareBy({ it.slot.ordinal }, { it.name.lowercase() }))

        val success = StudentPetsUiState.Success(
            header = header,
            pets = pets,
            consumables = consumables,
            accessories = accessories
        )
        latestSuccess.value = success
        return success
    }

    private data class ConsumableEffect(val hunger: Int, val happiness: Int)

    private val consumableEffects = mapOf(
        ConsumableKind.CROQUETA to ConsumableEffect(hunger = 25, happiness = 10),
        ConsumableKind.HUESO to ConsumableEffect(hunger = 15, happiness = 20)
    )
}
