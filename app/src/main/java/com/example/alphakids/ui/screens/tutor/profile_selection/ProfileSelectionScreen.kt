package com.example.alphakids.ui.screens.tutor.profile_selection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alphakids.ui.auth.AuthViewModel
import com.example.alphakids.ui.student.StudentListUiState
import com.example.alphakids.ui.student.StudentViewModel
import com.example.alphakids.ui.components.AppHeader
import com.example.alphakids.ui.components.CustomFAB
import com.example.alphakids.ui.screens.tutor.profile_selection.components.StudentProfileCard
import com.example.alphakids.ui.theme.AlphakidsTheme
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun ProfileSelectionScreen(
    onProfileClick: (profileId: String) -> Unit,
    onAddProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    studentViewModel: StudentViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val tutorName = currentUser?.nombre
    val studentListState by studentViewModel.studentListUiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = "Perfiles",
                actionIcon = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            CustomFAB(
                icon = Icons.Rounded.Settings,
                contentDescription = "Configuración",
                onClick = onSettingsClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                tutorName?.let { name ->
                    Text(
                        text = "¡Hola, $name!",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
                Text(
                    text = "¿Quién jugará hoy?",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Selecciona un perfil para comenzar",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            when (val state = studentListState) {
                StudentListUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                StudentListUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Aún no registras estudiantes",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StudentProfileCard(
                            title = "Agregar perfil",
                            description = "Crea el perfil de tu hijo",
                            icon = Icons.Rounded.Add,
                            onClick = onAddProfileClick
                        )
                    }
                }

                is StudentListUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.message,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        StudentProfileCard(
                            title = "Agregar perfil",
                            description = "Crea el perfil de tu hijo",
                            icon = Icons.Rounded.Add,
                            onClick = onAddProfileClick
                        )
                    }
                }

                is StudentListUiState.Success -> {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.students, key = { it.id }) { student ->
                            val gradeText = if (student.grado.isBlank()) "Sin grado" else student.grado
                            StudentProfileCard(
                                title = student.fullName,
                                description = gradeText,
                                supportingText = "Monedas: ${student.coins}",
                                icon = Icons.Rounded.Face,
                                onClick = { onProfileClick(student.id) }
                            )
                        }
                        item {
                            StudentProfileCard(
                                title = "Agregar perfil",
                                description = "Crea el perfil de tu hijo",
                                icon = Icons.Rounded.Add,
                                onClick = onAddProfileClick
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileSelectionScreenPreview() {
    AlphakidsTheme {
        ProfileSelectionScreen(
            onProfileClick = {},
            onAddProfileClick = {},
            onSettingsClick = {},
            onLogoutClick = {}
        )
    }
}
