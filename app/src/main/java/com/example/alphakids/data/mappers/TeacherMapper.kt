package com.example.alphakids.data.mappers

import com.example.alphakids.data.firebase.models.Docente
import com.example.alphakids.domain.models.Teacher

object TeacherMapper {
    fun toDomain(dto: Docente): Teacher {
        val institucionId = dto.institucionId ?: dto.idInstitucionCompat ?: ""
        return Teacher(
            id = dto.uid,
            nombre = dto.nombre,
            apellido = dto.apellido,
            institucionId = institucionId,
            grado = dto.grado,
            seccion = dto.seccion
        )
    }
}
