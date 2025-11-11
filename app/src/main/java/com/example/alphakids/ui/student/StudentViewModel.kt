package com.example.alphakids.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.domain.usecases.CreateStudentUseCase
import com.example.alphakids.domain.usecases.GetCurrentUserUseCase
import com.example.alphakids.domain.usecases.GetStudentsUseCase
import com.example.alphakids.domain.usecases.ObserveStudentUseCase
import com.example.alphakids.domain.usecases.ObserveTeacherByIdUseCase
import com.example.alphakids.domain.usecases.ObserveTeachersUseCase
import com.example.alphakids.domain.usecases.UpdateStudentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val createStudentUseCase: CreateStudentUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getStudentsUseCase: GetStudentsUseCase,
    private val observeTeachersUseCase: ObserveTeachersUseCase,
    private val observeTeacherByIdUseCase: ObserveTeacherByIdUseCase,
    private val observeStudentUseCase: ObserveStudentUseCase,
    private val updateStudentUseCase: UpdateStudentUseCase
) : ViewModel() {

    private val _createUiState = MutableStateFlow<StudentUiState>(StudentUiState.Idle)
    val createUiState: StateFlow<StudentUiState> = _createUiState

    private val _updateUiState = MutableStateFlow<StudentUiState>(StudentUiState.Idle)
    val updateUiState: StateFlow<StudentUiState> = _updateUiState

    private val editingStudentId = MutableStateFlow<String?>(null)

    private val tutorIdFlow: Flow<String?> = getCurrentUserUseCase().map { it?.uid }

    private sealed interface StudentDataResult {
        object Loading : StudentDataResult
        data class Success(val data: List<Estudiante>) : StudentDataResult
        data class Error(val throwable: Throwable) : StudentDataResult
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val studentsResult: StateFlow<StudentDataResult> = tutorIdFlow
        .flatMapLatest { tutorId ->
            if (tutorId.isNullOrEmpty()) {
                flowOf(StudentDataResult.Error(IllegalStateException("No se encontró tutor activo.")))
            } else {
                getStudentsUseCase(tutorId)
                    .map<StudentDataResult> { StudentDataResult.Success(it) }
                    .onStart { emit(StudentDataResult.Loading) }
                    .catch { emit(StudentDataResult.Error(it)) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudentDataResult.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val students: StateFlow<List<Estudiante>> = studentsResult
        .map { result ->
            when (result) {
                StudentDataResult.Loading -> emptyList()
                is StudentDataResult.Error -> emptyList()
                is StudentDataResult.Success -> result.data
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val studentListUiState: StateFlow<StudentListUiState> = studentsResult
        .map { result ->
            when (result) {
                StudentDataResult.Loading -> StudentListUiState.Loading
                is StudentDataResult.Error -> StudentListUiState.Error(
                    result.throwable.message ?: "Error al cargar estudiantes."
                )
                is StudentDataResult.Success -> {
                    if (result.data.isEmpty()) {
                        StudentListUiState.Empty
                    } else {
                        StudentListUiState.Success(result.data.map { it.toStudentSummary() })
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudentListUiState.Loading
        )

    val teacherListUiState: StateFlow<TeacherListUiState> = observeTeachersUseCase()
        .map<TeacherListUiState> { teachers ->
            if (teachers.isEmpty()) {
                TeacherListUiState.Empty
            } else {
                TeacherListUiState.Success(
                    teachers.map { teacher ->
                        val fullName = listOf(teacher.nombre, teacher.apellido)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank { "Docente" }
                        TeacherListItem(
                            id = teacher.id,
                            fullName = fullName,
                            institucionId = teacher.institucionId
                        )
                    }
                )
            }
        }
        .onStart { emit(TeacherListUiState.Loading) }
        .catch { emit(TeacherListUiState.Error(it.message ?: "Error al cargar docentes.")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TeacherListUiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val studentDetailUiState: StateFlow<StudentDetailUiState> = editingStudentId
        .flatMapLatest { studentId ->
            if (studentId.isNullOrEmpty()) {
                flowOf(StudentDetailUiState.Idle)
            } else {
                observeStudentUseCase(studentId)
                    .map<StudentDetailUiState> { estudiante ->
                        if (estudiante == null) {
                            StudentDetailUiState.Error("No se encontró el perfil.")
                        } else {
                            StudentDetailUiState.Success(estudiante)
                        }
                    }
                    .onStart { emit(StudentDetailUiState.Loading) }
                    .catch { emit(StudentDetailUiState.Error(it.message ?: "Error al cargar el perfil.")) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudentDetailUiState.Idle
        )

    fun setEditingStudentId(studentId: String?) {
        editingStudentId.value = studentId
    }

    fun createStudent(
        nombre: String,
        apellido: String,
        edad: Int,
        grado: String,
        seccion: String,
        docenteId: String
    ) {
        viewModelScope.launch {
            _createUiState.value = StudentUiState.Loading

            val currentUser = getCurrentUserUseCase().firstOrNull()
            if (currentUser == null) {
                _createUiState.value = StudentUiState.Error("No se pudo obtener el usuario actual.")
                return@launch
            }

            if (docenteId.isBlank()) {
                _createUiState.value = StudentUiState.Error("Selecciona un docente.")
                return@launch
            }

            val selectedTeacher = observeTeacherByIdUseCase(docenteId).firstOrNull()
            if (selectedTeacher == null) {
                _createUiState.value = StudentUiState.Error("El docente seleccionado no existe.")
                return@launch
            }

            val nuevoEstudiante = Estudiante(
                nombre = nombre,
                apellido = apellido,
                edad = edad,
                grado = grado,
                seccion = seccion,
                idTutor = currentUser.uid,
                idDocente = docenteId,
                idInstitucion = selectedTeacher.institucionId,
                coins = 0,
                fotoPerfil = null
            )

            val result = createStudentUseCase(nuevoEstudiante)
            if (result.isSuccess) {
                _createUiState.value = StudentUiState.Success(result.getOrNull() ?: "")
            } else {
                _createUiState.value = StudentUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido al crear perfil."
                )
            }
        }
    }

    fun updateStudent(
        studentId: String,
        nombre: String,
        apellido: String,
        edad: Int,
        grado: String,
        seccion: String,
        docenteId: String
    ) {
        viewModelScope.launch {
            _updateUiState.value = StudentUiState.Loading

            val currentUser = getCurrentUserUseCase().firstOrNull()
            if (currentUser == null) {
                _updateUiState.value = StudentUiState.Error("No se pudo obtener el usuario actual.")
                return@launch
            }

            if (docenteId.isBlank()) {
                _updateUiState.value = StudentUiState.Error("Selecciona un docente.")
                return@launch
            }

            val existingStudent = observeStudentUseCase(studentId).firstOrNull()
            if (existingStudent == null) {
                _updateUiState.value = StudentUiState.Error("No se encontró el perfil.")
                return@launch
            }

            val selectedTeacher = observeTeacherByIdUseCase(docenteId).firstOrNull()
            if (selectedTeacher == null) {
                _updateUiState.value = StudentUiState.Error("El docente seleccionado no existe.")
                return@launch
            }

            val updatedStudent = existingStudent.copy(
                nombre = nombre,
                apellido = apellido,
                edad = edad,
                grado = grado,
                seccion = seccion,
                idTutor = currentUser.uid,
                idDocente = docenteId,
                idInstitucion = selectedTeacher.institucionId
            )

            val result = updateStudentUseCase(updatedStudent)
            if (result.isSuccess) {
                _updateUiState.value = StudentUiState.Success(studentId)
            } else {
                _updateUiState.value = StudentUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido al actualizar perfil."
                )
            }
        }
    }

    fun resetCreateState() {
        _createUiState.value = StudentUiState.Idle
    }

    fun resetUpdateState() {
        _updateUiState.value = StudentUiState.Idle
    }

    private fun Estudiante.toStudentSummary(): StudentSummaryUi {
        val fullName = listOf(nombre, apellido)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { nombre.ifBlank { "Estudiante" } }
        return StudentSummaryUi(
            id = id,
            fullName = fullName,
            grado = grado,
            coins = coins
        )
    }
}
