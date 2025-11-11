package com.example.alphakids.data.firebase.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Palabra(
    @DocumentId
    val id: String = "",

    val texto: String = "",
    val categoria: String = "",

    @get:PropertyName("nivelDificultad")
    val nivelDificultad: String = "",

    val imagen: String = "",
    val audio: String = "",

    @get:PropertyName("reward_coins") @set:PropertyName("reward_coins")
    var rewardCoins: Int = 0,

    @get:PropertyName("fechaCreacion")
    @ServerTimestamp
    val fechaCreacion: Timestamp? = null,

    @get:PropertyName("creadoPor")
    val creadoPor: String? = null
)
