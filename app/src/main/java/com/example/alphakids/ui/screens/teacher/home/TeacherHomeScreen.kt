package com.example.alphakids.ui.screens.teacher.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphakids.ui.components.AppHeader
import com.example.alphakids.ui.components.BottomNavItem
import com.example.alphakids.ui.components.CustomFAB
import com.example.alphakids.ui.components.InfoCard
import com.example.alphakids.ui.components.MainBottomBar
import com.example.alphakids.ui.components.PrimaryButton
import com.example.alphakids.ui.theme.AlphakidsTheme
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun TeacherHomeScreen(
    teacherName: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAssignWordsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    onSettingsClick: () -> Unit, // <-- Añadido
    currentRoute: String = "home"
) {
    val teacherBottomNavItems = listOf(
        BottomNavItem("home", "Inicio", Icons.Rounded.Home),
        BottomNavItem("students", "Estudiantes", Icons.Rounded.Groups),
        BottomNavItem("words", "Palabras", Icons.Rounded.Spellcheck)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = "Inicio",
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
        floatingActionButton = { // <-- Añadido
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = "¡Hola $teacherName!",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Este es tu panel de docente, podrás asignar palabras y monitorear a tus estudiantes.",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrimaryButton(
                text = "Asignar palabras",
                icon = Icons.Rounded.ListAlt,
                onClick = onAssignWordsClick,
                modifier = Modifier.fillMaxWidth()
            )

            InfoCard(
                title = "Info",
                data = "Data",
                icon = Icons.Rounded.Face
            )
            InfoCard(
                title = "info",
                data = "Data",
                icon = Icons.Rounded.Face
            )
            InfoCard(
                title = "Info",
                data = "Data",
                icon = Icons.Rounded.Face
            )
            InfoCard(
                title = "Info",
                data = "Data",
                icon = Icons.Rounded.Settings
            )

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TeacherHomeScreenPreview() {
    AlphakidsTheme {
        TeacherHomeScreen(
            teacherName = "<nombre>",
            onBackClick = {},
            onLogoutClick = {},
            onAssignWordsClick = {},
            onBottomNavClick = {},
            onSettingsClick = {}
        )
    }
}
