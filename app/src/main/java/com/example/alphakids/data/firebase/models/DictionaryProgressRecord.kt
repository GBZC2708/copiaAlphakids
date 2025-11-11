package com.example.alphakids.data.firebase.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class DictionaryProgressRecord(
    @DocumentId
    val id: String = "",
    val studentId: String = "",
    val wordId: String = "",
    val teacherId: String = "",
    @get:PropertyName("completedAt")
    @ServerTimestamp
    val completedAt: Timestamp? = null
)
