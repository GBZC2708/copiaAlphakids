package com.example.alphakids.ui.word

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.Word
import com.example.alphakids.domain.repository.WordSortOrder
import com.example.alphakids.domain.usecases.CreateWordUseCase
import com.example.alphakids.domain.usecases.DeleteWordUseCase
import com.example.alphakids.domain.usecases.GetCurrentUserUseCase
import com.example.alphakids.domain.usecases.GetFilteredWordsUseCase // Importante
import com.example.alphakids.domain.usecases.UpdateWordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WordViewModel @Inject constructor(
    private val createWordUseCase: CreateWordUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val deleteWordUseCase: DeleteWordUseCase,
    private val getFilteredWordsUseCase: GetFilteredWordsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<WordUiState>(WordUiState.Idle)
    val uiState: StateFlow<WordUiState> = _uiState.asStateFlow()

    private val _sortOrder = MutableStateFlow(WordSortOrder.TEXT_ASC)
    val sortOrder: StateFlow<WordSortOrder> = _sortOrder.asStateFlow()

    private val _filterDifficulty = MutableStateFlow<String?>(null)
    val filterDifficulty: StateFlow<String?> = _filterDifficulty.asStateFlow()

    private val docenteIdFlow: Flow<String?> = getCurrentUserUseCase()
        .map { it?.uid }

    @OptIn(ExperimentalCoroutinesApi::class)
    val words: StateFlow<List<Word>> = combine(
        docenteIdFlow,
        _sortOrder,
        _filterDifficulty
    ) { docenteId, sortOrder, difficulty ->
        Triple(docenteId, sortOrder, difficulty)
    }.flatMapLatest { (docenteId, sortOrder, difficulty) ->
        if (docenteId != null) {
            // Usamos el nuevo UseCase para filtrar
            getFilteredWordsUseCase(
                docenteId = docenteId,
                dificultad = difficulty,
                sortBy = sortOrder
            ).catch { e ->
                Log.e("WordViewModel", "Error fetching filtered words", e)
                emit(emptyList())
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createWord(
        texto: String,
        categoria: String,
        nivelDificultad: String,
        imagenUrl: String,
        audioUrl: String,
        rewardCoins: Int
    ) {
        viewModelScope.launch {
            _uiState.value = WordUiState.Loading
            val currentUser = getCurrentUserUseCase().firstOrNull()
            if (currentUser == null) {
                _uiState.value = WordUiState.Error("No se pudo obtener el usuario.")
                return@launch
            }
            val newWord = Word(
                id = "",
                texto = texto,
                categoria = categoria,
                nivelDificultad = nivelDificultad,
                imagenUrl = imagenUrl,
                audioUrl = audioUrl,
                rewardCoins = rewardCoins.coerceAtLeast(0),
                fechaCreacionMillis = null,
                creadoPor = currentUser.uid
            )
            val result = createWordUseCase(newWord)
            _uiState.value = if (result.isSuccess) {
                WordUiState.Success("Palabra creada con éxito", result.getOrNull())
            } else {
                WordUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun updateWord(wordToUpdate: Word) {
        viewModelScope.launch {
            _uiState.value = WordUiState.Loading
            val result = updateWordUseCase(wordToUpdate)
            _uiState.value = if (result.isSuccess) {
                WordUiState.Success("Palabra actualizada con éxito")
            } else {
                WordUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun deleteWord(wordId: String) {
        viewModelScope.launch {
            _uiState.value = WordUiState.Loading
            if (wordId.isEmpty()) {
                _uiState.value = WordUiState.Error("ID de palabra inválido.")
                return@launch
            }
            val result = deleteWordUseCase(wordId)
            _uiState.value = if (result.isSuccess) {
                WordUiState.Success("Palabra eliminada con éxito")
            } else {
                WordUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun setDifficultyFilter(difficulty: String?) {
        _filterDifficulty.value = if (_filterDifficulty.value == difficulty) null else difficulty
    }

    fun setSortOrder(sortOrder: WordSortOrder) {
        _sortOrder.value = sortOrder
    }

    fun resetUiState() {
        _uiState.value = WordUiState.Idle
    }
}

