package com.example.alphakids.ui.auth

data class RegisterFormState(
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val password: String = "",
    val telefono: String = "",
    val nombreError: String? = null,
    val apellidoError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val telefonoError: String? = null
)
