package com.example.alphakids.domain.models

data class TeacherDictionaryWord(
    val id: String,
    val palabra: String,
    val uso: String?,
    val rewardCoins: Int,
    val categoria: String?,
    val dificultad: String?,
    val imagenUrl: String?,
    val audioUrl: String?,
    val docenteId: String
)
