package com.example.alphakids.data.firebase.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class AsignacionPalabra(
    @DocumentId
    val id: String = "",
    @get:PropertyName("id_docente") @set:PropertyName("id_docente")
    var idDocente: String = "",
    @get:PropertyName("id_estudiante") @set:PropertyName("id_estudiante")
    var idEstudiante: String = "",
    @get:PropertyName("id_palabra") @set:PropertyName("id_palabra")
    var idPalabra: String = "",
    @get:PropertyName("palabra_texto") @set:PropertyName("palabra_texto")
    var palabraTexto: String = "",
    @get:PropertyName("palabra_imagen") @set:PropertyName("palabra_imagen")
    var palabraImagen: String? = null,
    @get:PropertyName("palabra_audio") @set:PropertyName("palabra_audio")
    var palabraAudio: String? = null,
    @get:PropertyName("palabra_dificultad") @set:PropertyName("palabra_dificultad")
    var palabraDificultad: String = "",
    @get:PropertyName("estudiante_nombre") @set:PropertyName("estudiante_nombre")
    var estudianteNombre: String? = null,
    @get:PropertyName("reward_coins") @set:PropertyName("reward_coins")
    var rewardCoins: Int = 0,
    @get:PropertyName("intentos_restantes") @set:PropertyName("intentos_restantes")
    var intentosRestantes: Int? = null,
    @get:PropertyName("fecha_asignacion") @ServerTimestamp @set:PropertyName("fecha_asignacion")
    var fechaAsignacion: Timestamp? = null,
    @get:PropertyName("fecha_limite") @set:PropertyName("fecha_limite")
    var fechaLimite: Timestamp? = null,
    var estado: String = ""
)