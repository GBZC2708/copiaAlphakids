package com.example.alphakids.ui.screens.teacher.words

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphakids.domain.models.Word
import com.example.alphakids.ui.components.*
import com.example.alphakids.ui.screens.teacher.words.components.ImageUploadBox
import com.example.alphakids.ui.theme.dmSansFamily
import com.example.alphakids.ui.word.WordUiState
import com.example.alphakids.ui.word.WordViewModel

@Composable
fun WordEditScreen(
    viewModel: WordViewModel,
    wordUiState: WordUiState,
    word: Word?,
    isEditing: Boolean,
    onCloseClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val context = LocalContext.current

    var texto by remember(word) { mutableStateOf(word?.texto ?: "") }
    var categoria by remember(word) { mutableStateOf(word?.categoria ?: "") }
    var dificultad by remember(word) { mutableStateOf(word?.nivelDificultad ?: "") }

    LaunchedEffect(wordUiState) {
        when (wordUiState) {
            is WordUiState.Success -> {
                Toast.makeText(context, wordUiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetUiState()
                onCloseClick()
            }
            is WordUiState.Error -> {
                Toast.makeText(context, wordUiState.message, Toast.LENGTH_SHORT).show()
                viewModel.resetUiState()
            }
            WordUiState.Loading -> {

            }
            WordUiState.Idle -> {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = if (isEditing) "Editar palabra" else "Crear palabra",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            LabeledTextField(
                label = "Palabra",
                value = texto,
                onValueChange = { texto = it },
                placeholderText = "Escribe la palabra"
            )

            Text(
                text = "Imagen",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            ImageUploadBox { }

            LabeledDropdownField(
                label = "Categoría",
                selectedOption = categoria,
                placeholderText = "Selecciona categoría",
                onClick = { }
            )

            LabeledDropdownField(
                label = "Dificultad",
                selectedOption = dificultad,
                placeholderText = "Selecciona dificultad",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
            ) {
                SecondaryTonalButton(text = "Cancelar", onClick = onCancelClick)
                PrimaryButton(
                    text = if (isEditing) "Guardar" else "Crear palabra",
                    icon = if (!isEditing) Icons.Rounded.Add else null,
                    enabled = wordUiState != WordUiState.Loading,
                    onClick = {
                        if (isEditing && word != null) {
                            val updatedWord = word.copy(
                                texto = texto,
                                categoria = categoria,
                                nivelDificultad = dificultad
                            )
                            viewModel.updateWord(updatedWord)
                        } else {
                            val reward = when (dificultad.lowercase()) {
                                "fácil" -> 10
                                "medio" -> 15
                                "difícil" -> 20
                                else -> 12
                            }
                            viewModel.createWord(
                                texto = texto,
                                categoria = categoria,
                                nivelDificultad = dificultad,
                                imagenUrl = "url_imagen_mock",
                                audioUrl = "url_audio_mock",
                                rewardCoins = reward
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vista previa",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WordPreview(
                        word = texto,
                        icon = Icons.Rounded.Checkroom,
                        difficulty = dificultad.ifEmpty { "Fácil" }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WordPreview(
    word: String,
    icon: ImageVector,
    difficulty: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "¿Qué es esto?",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(30.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(word.length.coerceAtLeast(4)) { index ->
                LetterBox(
                    letter = null,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        PrimaryIconButton(
            icon = Icons.Rounded.CameraAlt,
            contentDescription = null,
            onClick = {},
            enabled = false
        )

        Spacer(modifier = Modifier.height(10.dp))

        InfoChip(
            text = difficulty,
            isSelected = true
        )
    }
}
