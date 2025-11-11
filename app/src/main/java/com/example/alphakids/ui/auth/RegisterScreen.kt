package com.example.alphakids.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alphakids.ui.components.AppHeader
import com.example.alphakids.ui.components.IconContainer
import com.example.alphakids.ui.components.LabeledTextField
import com.example.alphakids.ui.components.PrimaryButton
import com.example.alphakids.ui.theme.dmSansFamily
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    isTutorRegister: Boolean = true,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.authUiState.collectAsState()
    val formState by viewModel.registerFormState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(Unit) {
        viewModel.authUiState.collectLatest { state ->
            if (state is AuthUiState.Success) {
                onRegisterSuccess()
                viewModel.resetAuthState()
            }
        }
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = "Registro",
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
                    icon = if (isTutorRegister) Icons.Rounded.Face else Icons.Rounded.School,
                    contentDescription = "Icono de Registro"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Registro",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = if (isTutorRegister) "Crea tu cuenta de tutor" else "Crea tu cuenta de docente",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState is AuthUiState.Empty) {
                    Text(
                        text = "Completa los datos para crear tu cuenta",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                LabeledTextField(
                    label = "Nombre",
                    value = formState.nombre,
                    onValueChange = viewModel::onRegisterNombreChanged,
                    placeholderText = "Escribe tu nombre",
                    error = formState.nombreError
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledTextField(
                    label = "Apellido",
                    value = formState.apellido,
                    onValueChange = viewModel::onRegisterApellidoChanged,
                    placeholderText = "Escribe tu apellido",
                    error = formState.apellidoError
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledTextField(
                    label = "Email",
                    value = formState.email,
                    onValueChange = viewModel::onRegisterEmailChanged,
                    placeholderText = "Escribe tu email",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    error = formState.emailError
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledTextField(
                    label = "Contraseña",
                    value = formState.password,
                    onValueChange = viewModel::onRegisterPasswordChanged,
                    placeholderText = "Escribe tu contraseña",
                    visualTransformation = PasswordVisualTransformation(),
                    error = formState.passwordError
                )

                Spacer(modifier = Modifier.height(16.dp))

                LabeledTextField(
                    label = "Teléfono",
                    value = formState.telefono,
                    onValueChange = viewModel::onRegisterTelefonoChanged,
                    placeholderText = "Escribe tu teléfono",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    error = formState.telefonoError
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (uiState is AuthUiState.Error) {
                    Text(
                        text = (uiState as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                PrimaryButton(
                    text = "Crear cuenta",
                    onClick = {
                        val rol = if (isTutorRegister) "tutor" else "docente"
                        viewModel.submitRegister(rol)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
