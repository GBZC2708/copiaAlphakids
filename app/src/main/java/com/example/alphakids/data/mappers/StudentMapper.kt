package com.example.alphakids.data.mappers

import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.domain.models.Student

object StudentMapper {

    fun toDomain(dto: Estudiante): Student {
        return Student(
            id = dto.id,
            nombre = dto.nombre,
            apellido = dto.apellido,
            edad = dto.edad,
            grado = dto.grado,
            seccion = dto.seccion,
            idTutor = dto.idTutor,
            idDocente = dto.idDocente,
            idInstitucion = dto.idInstitucion,
            fotoPerfilUrl = dto.fotoPerfil,
            fechaRegistroMillis = dto.fechaRegistro?.toDate()?.time,
            coins = dto.coins
        )
    }

    fun fromDomain(model: Student): Estudiante {
        return Estudiante(
            id = model.id,
            nombre = model.nombre,
            apellido = model.apellido,
            edad = model.edad,
            grado = model.grado,
            seccion = model.seccion,
            idTutor = model.idTutor,
            idDocente = model.idDocente,
            idInstitucion = model.idInstitucion,
            fotoPerfil = model.fotoPerfilUrl,
            coins = model.coins,
            fechaRegistro = null
        )
    }
}
