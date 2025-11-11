package com.example.alphakids.domain.models

data class Teacher(
    val id: String,
    val nombre: String,
    val apellido: String,
    val institucionId: String,
    val grado: String,
    val seccion: String
)
