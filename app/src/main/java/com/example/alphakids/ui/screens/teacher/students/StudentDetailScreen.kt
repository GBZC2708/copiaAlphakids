package com.example.alphakids.ui.screens.teacher.students

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphakids.ui.components.AppHeader
import com.example.alphakids.ui.components.BottomNavItem
import com.example.alphakids.ui.components.CustomFAB
import com.example.alphakids.ui.components.InfoCard
import com.example.alphakids.ui.components.InfoChip
import com.example.alphakids.ui.components.MainBottomBar
import com.example.alphakids.ui.components.PrimaryButton
import com.example.alphakids.ui.components.StudentListItem
import com.example.alphakids.ui.components.WordListItem
import com.example.alphakids.ui.theme.AlphakidsTheme
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun StudentDetailScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAssignWordClick: () -> Unit,
    onWordClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentRoute: String = "students"
) {
    val teacherBottomNavItems = listOf(
        BottomNavItem("home", "Inicio", Icons.Rounded.Home),
        BottomNavItem("students", "Estudiantes", Icons.Rounded.Groups),
        BottomNavItem("words", "Palabras", Icons.Rounded.Spellcheck)
    )

    var selectedWordId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = "Mi estudiante",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = "Asignar palabra",
                icon = Icons.Rounded.ListAlt,
                onClick = onAssignWordClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Corregido: 'onClick' cambiado a 'onClickNavigation'
            StudentListItem(
                fullname = "Sofia Arenas",
                age = "3 años",
                numWords = "18 palabras",
                icon = Icons.Rounded.Face,
                chipText = "90%",
                isSelected = false,
                onClickNavigation = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    title = "Info",
                    data = "Data",
                    icon = Icons.Rounded.Star
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
                text = "Palabras aprendidas",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(text = "Chip 1", isSelected = false)
                InfoChip(text = "Chip 2", isSelected = false)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(5) { index ->
                    WordListItem(
                        title = "WORD ${index + 1}",
                        subtitle = "Categoría",
                        icon = Icons.Rounded.Checkroom,
                        chipText = if (index == 0) "3 intentos" else "Chip",
                        isSelected = (selectedWordId == "id_$index"),
                        onClick = { selectedWordId = "id_$index" }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentDetailScreenPreview() {
    AlphakidsTheme {
        StudentDetailScreen(
            onBackClick = {},
            onLogoutClick = {},
            onAssignWordClick = {},
            onWordClick = {},
            onSettingsClick = {},
            onBottomNavClick = {}
        )
    }
}
