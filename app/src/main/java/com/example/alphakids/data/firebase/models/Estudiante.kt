package com.example.alphakids.data.firebase.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Estudiante(
    @DocumentId
    val id: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val edad: Int = 0,
    val grado: String = "",
    val seccion: String = "",
    @get:PropertyName("id_tutor") @set:PropertyName("id_tutor")
    var idTutor: String = "",
    @get:PropertyName("id_docente") @set:PropertyName("id_docente")
    var idDocente: String = "",
    @get:PropertyName("id_institucion") @set:PropertyName("id_institucion")
    var idInstitucion: String = "",
    @get:PropertyName("foto_perfil") @set:PropertyName("foto_perfil")
    var fotoPerfil: String? = null,
    var coins: Int = 0,
    @get:PropertyName("fecha_registro") @ServerTimestamp @set:PropertyName("fecha_registro")
    var fechaRegistro: Timestamp? = null
)
