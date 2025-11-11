package com.example.alphakids.ui.screens.tutor.studentprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alphakids.ui.components.AppHeader
import com.example.alphakids.ui.components.IconContainer
import com.example.alphakids.ui.components.LabeledDropdownField
import com.example.alphakids.ui.components.LabeledTextField
import com.example.alphakids.ui.components.PrimaryButton
import com.example.alphakids.ui.student.StudentDetailUiState
import com.example.alphakids.ui.student.StudentUiState
import com.example.alphakids.ui.student.StudentViewModel
import com.example.alphakids.ui.student.TeacherListUiState
import com.example.alphakids.ui.theme.AlphakidsTheme
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun EditStudentProfileScreen(
    studentId: String,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
    viewModel: StudentViewModel = hiltViewModel()
) {
    val detailState by viewModel.studentDetailUiState.collectAsState()
    val updateState by viewModel.updateUiState.collectAsState()
    val teacherState by viewModel.teacherListUiState.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var edadString by remember { mutableStateOf("") }
    var grado by remember { mutableStateOf("") }
    var seccion by remember { mutableStateOf("") }
    var selectedDocenteId by remember { mutableStateOf<String?>(null) }

    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(studentId) {
        viewModel.setEditingStudentId(studentId)
        initialized = false
    }

    LaunchedEffect(detailState) {
        if (!initialized) {
            val estudiante = (detailState as? StudentDetailUiState.Success)?.estudiante
            if (estudiante != null) {
                nombre = estudiante.nombre
                apellido = estudiante.apellido
                edadString = estudiante.edad.takeIf { it > 0 }?.toString() ?: ""
                grado = estudiante.grado
                seccion = estudiante.seccion
                selectedDocenteId = estudiante.idDocente
                initialized = true
            }
        }
    }

    var nombreError by remember { mutableStateOf<String?>(null) }
    var apellidoError by remember { mutableStateOf<String?>(null) }
    var edadError by remember { mutableStateOf<String?>(null) }
    var docenteError by remember { mutableStateOf<String?>(null) }
    var docenteMenuExpanded by remember { mutableStateOf(false) }

    val teacherOptions = when (val state = teacherState) {
        is TeacherListUiState.Success -> state.teachers
        else -> emptyList()
    }

    val selectedDocenteName =
        teacherOptions.firstOrNull { it.id == selectedDocenteId }?.fullName ?: ""

    val isLoading =
        updateState is StudentUiState.Loading ||
                detailState is StudentDetailUiState.Loading ||
                detailState is StudentDetailUiState.Idle

    val updateError = (updateState as? StudentUiState.Error)?.message
    val detailError = (detailState as? StudentDetailUiState.Error)?.message

    // Dispara navegación al guardar exitoso
    LaunchedEffect(updateState) {
        if (updateState is StudentUiState.Success) {
            onSaveClick()
            viewModel.resetUpdateState()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = "Editar perfil",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actionIcon = {
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (detailError != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = detailError,
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    IconContainer(
                        icon = Icons.Rounded.Star,
                        contentDescription = "Icono de Perfil de Estudiante"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Editar perfil",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Actualiza los datos del estudiante",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    LabeledTextField(
                        label = "Nombre",
                        value = nombre,
                        onValueChange = {
                            nombre = it
                            if (nombreError != null) nombreError = null
                        },
                        placeholderText = "Nombre del estudiante"
                    )
                    nombreError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledTextField(
                        label = "Apellido",
                        value = apellido,
                        onValueChange = {
                            apellido = it
                            if (apellidoError != null) apellidoError = null
                        },
                        placeholderText = "Apellido del estudiante"
                    )
                    apellidoError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mantengo el filtrado numérico; se puede abrir teclado numérico desde el propio LabeledTextField si expone ese parámetro.
                    LabeledTextField(
                        label = "Edad",
                        value = edadString,
                        onValueChange = {
                            edadString = it.filter { ch -> ch.isDigit() }
                            if (edadError != null) edadError = null
                        },
                        placeholderText = "Edad"
                    )
                    edadError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledTextField(
                        label = "Grado",
                        value = grado,
                        onValueChange = { grado = it },
                        placeholderText = "Grado"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledTextField(
                        label = "Sección",
                        value = seccion,
                        onValueChange = { seccion = it },
                        placeholderText = "Sección"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box {
                        LabeledDropdownField(
                            label = "Docente",
                            selectedOption = selectedDocenteName,
                            placeholderText = "Selecciona docente",
                            onClick = {
                                if (teacherOptions.isNotEmpty()) {
                                    docenteMenuExpanded = true
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = docenteMenuExpanded,
                            onDismissRequest = { docenteMenuExpanded = false }
                        ) {
                            teacherOptions.forEach { teacher ->
                                DropdownMenuItem(
                                    text = { Text(teacher.fullName) },
                                    onClick = {
                                        selectedDocenteId = teacher.id
                                        docenteError = null
                                        docenteMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    when (val ts = teacherState) {
                        TeacherListUiState.Loading -> {
                            Text(
                                text = "Cargando docentes...",
                                fontFamily = dmSansFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TeacherListUiState.Empty -> {
                            Text(
                                text = "No hay docentes disponibles",
                                fontFamily = dmSansFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is TeacherListUiState.Error -> {
                            Text(
                                text = ts.message,
                                fontFamily = dmSansFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> Unit
                    }

                    docenteError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    updateError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp
                        )
                    }

                    PrimaryButton(
                        text = "Guardar",
                        onClick = {
                            val edadInt = edadString.toIntOrNull()
                            nombreError = if (nombre.isBlank()) "Ingresa el nombre" else null
                            apellidoError = if (apellido.isBlank()) "Ingresa el apellido" else null
                            edadError = if (edadInt == null || edadInt <= 0) "Ingresa una edad válida" else null
                            docenteError = if (selectedDocenteId.isNullOrEmpty()) "Selecciona un docente" else null

                            if (listOf(nombreError, apellidoError, edadError, docenteError).any { it != null }) {
                                return@PrimaryButton
                            }

                            viewModel.updateStudent(
                                studentId = studentId,
                                nombre = nombre,
                                apellido = apellido,
                                edad = edadInt ?: 0,
                                grado = grado,
                                seccion = seccion,
                                docenteId = selectedDocenteId.orEmpty()
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && detailState is StudentDetailUiState.Success
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditStudentProfileScreenPreview() {
    AlphakidsTheme {
        EditStudentProfileScreen(
            studentId = "",
            onBackClick = {},
            onCloseClick = {},
            onSaveClick = {}
        )
    }
}
