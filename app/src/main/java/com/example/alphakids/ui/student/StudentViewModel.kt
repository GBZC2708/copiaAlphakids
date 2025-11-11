package com.example.alphakids.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.domain.models.Student
import com.example.alphakids.domain.models.Teacher
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
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

    /** Flujo con el uid del tutor autenticado */
    private val tutorIdFlow: Flow<String?> = getCurrentUserUseCase().map { user ->
        user?.uid
    }

    /** Wrapper interno para manejar estados del fetch de estudiantes */
    private sealed interface StudentDataResult {
        object Loading : StudentDataResult
        data class Success(val data: List<Estudiante>) : StudentDataResult
        data class Error(val throwable: Throwable) : StudentDataResult
    }

    /** Estudiantes del tutor actual como resultado con Loading/Error */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val studentsResult: StateFlow<StudentDataResult> = tutorIdFlow
        .flatMapLatest { tutorId ->
            if (tutorId.isNullOrEmpty()) {
                flowOf<StudentDataResult>(
                    StudentDataResult.Error(IllegalStateException("No se encontró tutor activo."))
                )
            } else {
                // Asumido: Flow<List<Estudiante>>
                getStudentsUseCase(tutorId)
                    .map { list: List<Estudiante> ->
                        StudentDataResult.Success(list) as StudentDataResult
                    }
                    .onStart { emit(StudentDataResult.Loading) }
                    .catch { e -> emit(StudentDataResult.Error(e)) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudentDataResult.Loading
        )

    /** Lista cruda de Estudiante para quien la necesite */
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

    /** Estado de UI para lista de estudiantes */
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

    /** Estado de UI para lista de docentes (usa domain.models.Teacher) */
    val teacherListUiState: StateFlow<TeacherListUiState> = observeTeachersUseCase()
        .map { teachers: List<Teacher> ->
            if (teachers.isEmpty()) {
                TeacherListUiState.Empty
            } else {
                val items: List<TeacherListItem> = teachers.map { teacher ->
                    val fullName = listOf(teacher.nombre, teacher.apellido)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { "Docente" }

                    val institucionId = teacher.institucionId ?: ""

                    TeacherListItem(
                        id = teacher.id,
                        fullName = fullName,
                        institucionId = institucionId
                    )
                }
                TeacherListUiState.Success(items)
            }
        }
        .onStart { emit(TeacherListUiState.Loading) }
        .catch { e -> emit(TeacherListUiState.Error(e.message ?: "Error al cargar docentes.")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TeacherListUiState.Loading
        )

    /** Detalle del estudiante en edición (UI exige Estudiante) */
    @OptIn(ExperimentalCoroutinesApi::class)
    val studentDetailUiState: StateFlow<StudentDetailUiState> = editingStudentId
        .flatMapLatest { studentId ->
            if (studentId.isNullOrEmpty()) {
                flowOf<StudentDetailUiState>(StudentDetailUiState.Idle)
            } else {
                // ObserveStudentUseCase devuelve Flow<Student?> (domain)
                observeStudentUseCase(studentId)
                    .map { student: Student? ->
                        if (student == null) {
                            StudentDetailUiState.Error("No se encontró el perfil.")
                        } else {
                            StudentDetailUiState.Success(student.toDataEstudiante())
                        }
                    }
                    .onStart { emit(StudentDetailUiState.Loading) }
                    .catch { e -> emit(StudentDetailUiState.Error(e.message ?: "Error al cargar el perfil.")) }
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

            // domain.models.Teacher?
            val selectedTeacher: Teacher? = observeTeacherByIdUseCase(docenteId).firstOrNull()
            if (selectedTeacher == null) {
                _createUiState.value = StudentUiState.Error("El docente seleccionado no existe.")
                return@launch
            }

            val institucionIdResolved = selectedTeacher.institucionId ?: ""

            val nuevoEstudiante = Estudiante(
                id = "",
                nombre = nombre,
                apellido = apellido,
                edad = edad,
                grado = grado,
                seccion = seccion,
                idTutor = currentUser.uid,
                idDocente = docenteId,
                idInstitucion = institucionIdResolved,
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

            val existingStudent: Student? = observeStudentUseCase(studentId).firstOrNull()
            if (existingStudent == null) {
                _updateUiState.value = StudentUiState.Error("No se encontró el perfil.")
                return@launch
            }

            val selectedTeacher: Teacher? = observeTeacherByIdUseCase(docenteId).firstOrNull()
            if (selectedTeacher == null) {
                _updateUiState.value = StudentUiState.Error("El docente seleccionado no existe.")
                return@launch
            }

            val institucionIdResolved = selectedTeacher.institucionId ?: ""

            val updatedStudentData = Estudiante(
                id = existingStudent.id,
                nombre = nombre,
                apellido = apellido,
                edad = edad,
                grado = grado,
                seccion = seccion,
                idTutor = currentUser.uid,
                idDocente = docenteId,
                idInstitucion = institucionIdResolved,
                fotoPerfil = existingStudent.fotoPerfilUrl,
                coins = existingStudent.coins
            )

            val result = updateStudentUseCase(updatedStudentData)
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

    /** Helpers de mapeo para UI */
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

    /** Convertir domain -> data para cumplir StudentDetailUiState.Success(Estudiante) */
    private fun Student.toDataEstudiante(): Estudiante =
        Estudiante(
            id = id,
            nombre = nombre,
            apellido = apellido,
            edad = edad,
            grado = grado,
            seccion = seccion,
            idTutor = idTutor,
            idDocente = idDocente,
            idInstitucion = idInstitucion,
            fotoPerfil = fotoPerfilUrl,
            coins = coins
            // fechaRegistro: lo maneja Firestore con @ServerTimestamp en data layer
        )
}
