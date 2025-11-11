package com.example.alphakids.data.firebase.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Docente(
    @DocumentId
    val uid: String = "",

    val nombre: String = "",
    val apellido: String = "",

    @get:PropertyName("institucionId")
    @set:PropertyName("institucionId")
    var institucionId: String? = null,

    @get:PropertyName("idInstitucion")
    @set:PropertyName("idInstitucion")
    var idInstitucionCompat: String? = null,

    val seccion: String = "",
    val grado: String = "",

    @PropertyName("fechaRegistro")
    @ServerTimestamp
    val fechaRegistro: Timestamp? = null
)
