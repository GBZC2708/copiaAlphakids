package com.example.alphakids.ui.student

import com.example.alphakids.data.firebase.models.Estudiante

sealed interface StudentUiState {
    object Idle : StudentUiState
    object Loading : StudentUiState
    data class Success(val studentId: String) : StudentUiState
    data class Error(val message: String) : StudentUiState
}

sealed interface StudentListUiState {
    object Loading : StudentListUiState
    object Empty : StudentListUiState
    data class Error(val message: String) : StudentListUiState
    data class Success(val students: List<StudentSummaryUi>) : StudentListUiState
}

data class StudentSummaryUi(
    val id: String,
    val fullName: String,
    val grado: String,
    val coins: Int
)

sealed interface StudentDetailUiState {
    object Idle : StudentDetailUiState
    object Loading : StudentDetailUiState
    data class Success(val estudiante: Estudiante) : StudentDetailUiState
    data class Error(val message: String) : StudentDetailUiState
}

sealed interface TeacherListUiState {
    object Loading : TeacherListUiState
    object Empty : TeacherListUiState
    data class Error(val message: String) : TeacherListUiState
    data class Success(val teachers: List<TeacherListItem>) : TeacherListUiState
}

data class TeacherListItem(
    val id: String,
    val fullName: String,
    val institucionId: String
)