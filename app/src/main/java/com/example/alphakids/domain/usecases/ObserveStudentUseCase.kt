package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.models.Student
import com.example.alphakids.domain.repository.StudentRepository
import com.example.alphakids.data.firebase.models.Estudiante
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveStudentUseCase @Inject constructor(
    private val repository: StudentRepository
) {
    operator fun invoke(studentId: String): Flow<Student?> {
        // repository.observeStudent(...) devuelve Flow<Estudiante?>
        return repository.observeStudent(studentId)
            .map { estudiante -> estudiante?.toDomain() }
    }
}

/** Conversión data (Estudiante) -> domain (Student) */
private fun Estudiante.toDomain(): Student = Student(
    id = id ?: "",
    nombre = nombre ?: "",
    apellido = apellido ?: "",
    edad = edad ?: 0,
    grado = grado ?: "",
    seccion = seccion ?: "",
    idTutor = idTutor ?: "",
    idDocente = idDocente ?: "",
    idInstitucion = idInstitucion ?: "",
    // Estos campos no existen en Estudiante (capa data), así que los ponemos en null
    fotoPerfilUrl = null,
    fechaRegistroMillis = null,
    // Ajusta si tu Estudiante.coins es no-nulo; si es nullable, protegemos con elvis
    coins = coins ?: 0
)
