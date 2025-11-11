package com.example.alphakids.domain.models

data class Student(
    val id: String,
    val nombre: String,
    val apellido: String,
    val edad: Int,
    val grado: String,
    val seccion: String,
    val idTutor: String,
    val idDocente: String,
    val idInstitucion: String,
    val fotoPerfilUrl: String?,
    val fechaRegistroMillis: Long?,
    val coins: Int
)
