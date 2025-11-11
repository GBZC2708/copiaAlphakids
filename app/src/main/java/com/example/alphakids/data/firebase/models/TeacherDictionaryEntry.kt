package com.example.alphakids.data.firebase.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class TeacherDictionaryEntry(
    @DocumentId
    val id: String = "",
    val palabra: String = "",
    val uso: String? = null,
    val docenteId: String = "",
    @get:PropertyName("categoria")
    val categoria: String? = null,
    @get:PropertyName("nivelDificultad")
    val nivelDificultad: String? = null,
    @get:PropertyName("reward_coins")
    val rewardCoins: Int = 0,
    val imagen: String? = null,
    val audio: String? = null
)
