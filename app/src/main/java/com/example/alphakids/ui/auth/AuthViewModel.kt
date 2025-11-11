package com.example.alphakids.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.User
import com.example.alphakids.domain.usecases.GetCurrentUserUseCase
import com.example.alphakids.domain.usecases.LoginUserUseCase
import com.example.alphakids.domain.usecases.LogoutUserUseCase
import com.example.alphakids.domain.usecases.RegisterUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val logoutUserUseCase: LogoutUserUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Empty)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _loginFormState = MutableStateFlow(LoginFormState())
    val loginFormState: StateFlow<LoginFormState> = _loginFormState.asStateFlow()

    private val _registerFormState = MutableStateFlow(RegisterFormState())
    val registerFormState: StateFlow<RegisterFormState> = _registerFormState.asStateFlow()

    fun onLoginEmailChanged(value: String) {
        _loginFormState.update { it.copy(email = value, emailError = null) }
        clearAuthError()
    }

    fun onLoginPasswordChanged(value: String) {
        _loginFormState.update { it.copy(password = value, passwordError = null) }
        clearAuthError()
    }

    fun submitLogin() {
        val current = _loginFormState.value
        val email = current.email.trim()
        val password = current.password
        val emailError = validateEmail(email)
        val passwordError = if (password.isBlank()) "Ingresa tu contraseña" else null
        if (emailError != null || passwordError != null) {
            _loginFormState.value = current.copy(
                email = email,
                emailError = emailError,
                passwordError = passwordError
            )
            return
        }
        performLogin(email, password)
    }

    private fun performLogin(email: String, clave: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            loginUserUseCase(email, clave)
                .collect { result ->
                    if (result.isSuccess) {
                        _authUiState.value = AuthUiState.Success(result.getOrNull()!!)
                    } else {
                        _authUiState.value = AuthUiState.Error(
                            result.exceptionOrNull()?.message ?: "Correo o contraseña incorrectos"
                        )
                    }
                }
        }
    }

    fun onRegisterNombreChanged(value: String) {
        _registerFormState.update { it.copy(nombre = value, nombreError = null) }
        clearAuthError()
    }

    fun onRegisterApellidoChanged(value: String) {
        _registerFormState.update { it.copy(apellido = value, apellidoError = null) }
        clearAuthError()
    }

    fun onRegisterEmailChanged(value: String) {
        _registerFormState.update { it.copy(email = value, emailError = null) }
        clearAuthError()
    }

    fun onRegisterPasswordChanged(value: String) {
        _registerFormState.update { it.copy(password = value, passwordError = null) }
        clearAuthError()
    }

    fun onRegisterTelefonoChanged(value: String) {
        _registerFormState.update { it.copy(telefono = value, telefonoError = null) }
        clearAuthError()
    }

    fun submitRegister(rol: String) {
        val current = _registerFormState.value
        val nombre = current.nombre.trim()
        val apellido = current.apellido.trim()
        val email = current.email.trim()
        val password = current.password
        val telefono = current.telefono.trim()
        val telefonoDigits = telefono.filter(Char::isDigit)

        val nombreError = if (nombre.isEmpty()) "Ingresa tu nombre" else null
        val apellidoError = if (apellido.isEmpty()) "Ingresa tu apellido" else null
        val emailError = validateEmail(email)
        val passwordError = if (password.length < 6) "La contraseña debe tener al menos 6 caracteres" else null
        val telefonoError = when {
            telefono.isEmpty() -> "Ingresa tu teléfono"
            telefonoDigits.length < 10 -> "Ingresa un teléfono válido"
            else -> null
        }

        if (listOf(nombreError, apellidoError, emailError, passwordError, telefonoError).any { it != null }) {
            _registerFormState.value = current.copy(
                nombre = nombre,
                apellido = apellido,
                email = email,
                telefono = telefono,
                nombreError = nombreError,
                apellidoError = apellidoError,
                emailError = emailError,
                passwordError = passwordError,
                telefonoError = telefonoError
            )
            return
        }

        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            registerUserUseCase(nombre, apellido, email, password, telefonoDigits, rol)
                .collect { result ->
                    if (result.isSuccess) {
                        _authUiState.value = AuthUiState.Success(result.getOrNull()!!)
                    } else {
                        _authUiState.value = AuthUiState.Error(
                            result.exceptionOrNull()?.message ?: "Error desconocido"
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUserUseCase()
        }
    }

    fun resetAuthState() {
        _authUiState.value = AuthUiState.Empty
        _loginFormState.value = LoginFormState()
        _registerFormState.value = RegisterFormState()
    }

    private fun validateEmail(email: String): String? {
        if (email.isEmpty()) return "Ingresa tu correo"
        return if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) null else "Ingresa un correo válido"
    }

    private fun clearAuthError() {
        if (_authUiState.value is AuthUiState.Error) {
            _authUiState.value = AuthUiState.Empty
        }
    }
}
