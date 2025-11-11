package com.example.alphakids.ui.screens.teacher.words

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphakids.domain.models.Word
import com.example.alphakids.ui.components.*
import com.example.alphakids.ui.theme.AlphakidsTheme
import com.example.alphakids.ui.theme.dmSansFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WordDetailScreen(
    word: Word?,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onEditWordClick: () -> Unit,
    onDeleteWordClick: () -> Unit,
    onStudentClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentRoute: String = "words"
) {
    val teacherBottomNavItems = listOf(
        BottomNavItem("home", "Inicio", Icons.Rounded.Home),
        BottomNavItem("students", "Estudiantes", Icons.Rounded.Groups),
        BottomNavItem("words", "Palabras", Icons.Rounded.Spellcheck)
    )

    val dateFormatter = remember {
        SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = word?.texto ?: "Detalle de palabra",
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
        bottomBar = {
            MainBottomBar(
                items = teacherBottomNavItems,
                currentRoute = currentRoute,
                onNavigate = onBottomNavClick
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
        if (word == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Palabra no encontrada o cargando...")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton(
                    modifier = Modifier.weight(1f),
                    text = "Editar palabra",
                    icon = Icons.Rounded.Edit,
                    onClick = onEditWordClick
                )
                ErrorButton(
                    modifier = Modifier.weight(1f),
                    text = "Eliminar palabra",
                    icon = Icons.Rounded.Delete,
                    onClick = onDeleteWordClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            WordListItem(
                title = word.texto,
                subtitle = word.categoria,
                icon = Icons.Rounded.Checkroom,
                chipText = word.nivelDificultad,
                isSelected = true,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    title = "Fecha Creación",
                    data = word.fechaCreacionMillis?.let {
                        dateFormatter.format(Date(it))
                    } ?: "N/A",
                    icon = Icons.Rounded.CalendarToday
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    title = "Info",
                    data = "Data",
                    icon = Icons.Rounded.Star
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Estudiantes asignados",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Funcionalidad no implementada.", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
