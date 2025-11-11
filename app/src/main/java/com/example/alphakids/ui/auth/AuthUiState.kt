package com.example.alphakids.ui.auth

import com.example.alphakids.domain.models.User

sealed interface AuthUiState {
    object Empty : AuthUiState
    object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
