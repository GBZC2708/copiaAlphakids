package com.example.alphakids.domain.models

data class Achievement(
    val id: String,
    val studentId: String,
    val name: String,
    val description: String,
    val coins: Int,
    val createdAtMillis: Long,
    val imageUrl: String? = null
)
