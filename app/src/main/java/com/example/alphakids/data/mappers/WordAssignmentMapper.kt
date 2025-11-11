package com.example.alphakids.data.mappers

import com.example.alphakids.data.firebase.models.AsignacionPalabra
import com.example.alphakids.domain.models.WordAssignment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.util.Date

private const val DEFAULT_ATTEMPTS = 3

object WordAssignmentMapper {

    fun toDomain(dto: AsignacionPalabra): WordAssignment {
        return WordAssignment(
            id = dto.id,
            idDocente = dto.idDocente,
            idEstudiante = dto.idEstudiante,
            idPalabra = dto.idPalabra,
            palabraTexto = dto.palabraTexto,
            palabraImagenUrl = dto.palabraImagen,
            palabraAudioUrl = dto.palabraAudio,
            palabraDificultad = dto.palabraDificultad,
            estudianteNombre = dto.estudianteNombre,
            rewardCoins = dto.rewardCoins.coerceAtLeast(0),
            attemptsRemaining = (dto.intentosRestantes ?: DEFAULT_ATTEMPTS).coerceAtLeast(0),
            fechaAsignacionMillis = dto.fechaAsignacion?.toDate()?.time,
            fechaLimiteMillis = dto.fechaLimite?.toDate()?.time,
            estado = dto.estado
        )
    }

    fun fromDomain(model: WordAssignment): Map<String, Any?> {
        val data = mutableMapOf<String, Any?>(
            "id_docente" to model.idDocente,
            "id_estudiante" to model.idEstudiante,
            "id_palabra" to model.idPalabra,
            "palabra_texto" to model.palabraTexto,
            "palabra_imagen" to model.palabraImagenUrl,
            "palabra_audio" to model.palabraAudioUrl,
            "palabra_dificultad" to model.palabraDificultad.ifEmpty { "Desconocida" },
            "estudiante_nombre" to model.estudianteNombre,
            "reward_coins" to model.rewardCoins.coerceAtLeast(0),
            "intentos_restantes" to (model.attemptsRemaining ?: DEFAULT_ATTEMPTS).coerceAtLeast(0),
            "estado" to model.estado
        )

        if (model.fechaAsignacionMillis == null) {
            data["fecha_asignacion"] = FieldValue.serverTimestamp()
        } else {
            data["fecha_asignacion"] = Timestamp(Date(model.fechaAsignacionMillis))
        }

        if (model.fechaLimiteMillis != null) {
            data["fecha_limite"] = Timestamp(Date(model.fechaLimiteMillis))
        }

        return data
    }
}
