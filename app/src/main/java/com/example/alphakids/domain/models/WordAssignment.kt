package com.example.alphakids.domain.models

data class WordAssignment(
    val id: String,
    val idDocente: String,
    val idEstudiante: String,
    val idPalabra: String,
    val palabraTexto: String,
    val palabraImagenUrl: String?,
    val palabraAudioUrl: String?,
    val palabraDificultad: String,
    val estudianteNombre: String?,
    val rewardCoins: Int,
    val attemptsRemaining: Int?,
    val fechaAsignacionMillis: Long?,
    val fechaLimiteMillis: Long?,
    val estado: String
)