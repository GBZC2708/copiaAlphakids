package com.example.alphakids.data.mappers

import com.example.alphakids.data.firebase.models.TeacherDictionaryEntry
import com.example.alphakids.domain.models.TeacherDictionaryWord

object TeacherDictionaryMapper {
    fun toDomain(dto: TeacherDictionaryEntry): TeacherDictionaryWord {
        return TeacherDictionaryWord(
            id = dto.id,
            palabra = dto.palabra,
            uso = dto.uso,
            rewardCoins = dto.rewardCoins.coerceAtLeast(0),
            categoria = dto.categoria,
            dificultad = dto.nivelDificultad,
            imagenUrl = dto.imagen,
            audioUrl = dto.audio,
            docenteId = dto.docenteId
        )
    }
}
