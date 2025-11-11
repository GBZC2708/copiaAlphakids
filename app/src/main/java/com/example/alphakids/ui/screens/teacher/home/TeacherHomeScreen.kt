package com.example.alphakids.ui.screens.teacher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alphakids.ui.components.AppHeader
import com.example.alphakids.ui.components.BottomNavItem
import com.example.alphakids.ui.components.CustomFAB
import com.example.alphakids.ui.components.InfoCard
import com.example.alphakids.ui.components.InfoChip
import com.example.alphakids.ui.components.MainBottomBar
import com.example.alphakids.ui.components.SearchBar
import com.example.alphakids.ui.components.StudentListItem
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun TeacherHomeRoute(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAssignWordsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentRoute: String,
    modifier: Modifier = Modifier,
    viewModel: TeacherHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TeacherHomeScreen(
        modifier = modifier,
        uiState = uiState,
        onBackClick = onBackClick,
        onLogoutClick = onLogoutClick,
        onAssignWordsClick = onAssignWordsClick,
        onSettingsClick = onSettingsClick,
        onBottomNavClick = onBottomNavClick,
        currentRoute = currentRoute,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onGradeSelected = viewModel::onGradeSelected,
        onRetry = viewModel::retry
    )
}

@Composable
private fun TeacherHomeScreen(
    modifier: Modifier,
    uiState: TeacherHomeUiState,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAssignWordsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentRoute: String,
    onSearchQueryChange: (String) -> Unit,
    onGradeSelected: (String) -> Unit,
    onRetry: () -> Unit
) {
    val teacherBottomNavItems = listOf(
        BottomNavItem("home", "Inicio", Icons.Rounded.Home),
        BottomNavItem("students", "Estudiantes", Icons.Rounded.Groups),
        BottomNavItem("words", "Palabras", Icons.Rounded.Spellcheck)
    )

    val headerSubtitle = when (uiState) {
        is TeacherHomeUiState.Success -> uiState.header.fullName
        is TeacherHomeUiState.Empty -> uiState.header.fullName
        is TeacherHomeUiState.Error -> uiState.header?.fullName
        TeacherHomeUiState.Loading -> null
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = "Inicio docente",
                subtitle = headerSubtitle,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                actionIcon = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
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
        when (uiState) {
            TeacherHomeUiState.Loading -> LoadingState(paddingValues)
            is TeacherHomeUiState.Error -> ErrorState(
                paddingValues = paddingValues,
                message = uiState.message,
                onRetry = onRetry
            )
            is TeacherHomeUiState.Empty -> EmptyState(
                paddingValues = paddingValues,
                stats = uiState.stats,
                onAssignWordsClick = onAssignWordsClick
            )
            is TeacherHomeUiState.Success -> SuccessState(
                paddingValues = paddingValues,
                uiState = uiState,
                onAssignWordsClick = onAssignWordsClick,
                onSearchQueryChange = onSearchQueryChange,
                onGradeSelected = onGradeSelected
            )
        }
    }
}

@Composable
private fun LoadingState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    paddingValues: PaddingValues,
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun EmptyState(
    paddingValues: PaddingValues,
    stats: TeacherHomeStats,
    onAssignWordsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        StatsRow(stats = stats)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Aún no tienes estudiantes registrados.",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAssignWordsClick) {
                Text("Asignar palabras")
            }
        }
    }
}

@Composable
private fun SuccessState(
    paddingValues: PaddingValues,
    uiState: TeacherHomeUiState.Success,
    onAssignWordsClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onGradeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        StatsRow(stats = uiState.stats)

        Button(
            onClick = onAssignWordsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Asignar palabras")
        }

        SearchBar(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholderText = "Buscar estudiante"
        )

        if (uiState.availableGrades.isNotEmpty()) {
            LazyGradesChips(
                grades = uiState.availableGrades,
                selectedGrade = uiState.selectedGrade,
                onGradeSelected = onGradeSelected
            )
        }

        val students = uiState.students

        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin resultados para los filtros actuales.",
                    fontFamily = dmSansFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(students, key = { it.id }) { student ->
                    StudentListItem(
                        fullname = student.fullName.ifBlank { "Estudiante" },
                        age = buildString {
                            if (student.grade.isNotBlank()) {
                                append("Grado ${student.grade}")
                            }
                            if (student.section.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append("Sección ${student.section}")
                            }
                        }.ifBlank { "Sin grado" },
                        numWords = "Monedas: ${student.coins}",
                        icon = Icons.Rounded.Face,
                        chipText = "Activo",
                        onClickNavigation = {},
                        isSelected = false
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: TeacherHomeStats) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RowStats(
            primaryTitle = "Estudiantes",
            primaryValue = stats.studentsCount.toString(),
            secondaryTitle = "Palabras",
            secondaryValue = stats.wordsCount.toString()
        )
    }
}

@Composable
private fun RowStats(
    primaryTitle: String,
    primaryValue: String,
    secondaryTitle: String,
    secondaryValue: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                modifier = Modifier.weight(1f),
                title = primaryTitle,
                data = primaryValue
            )
            InfoCard(
                modifier = Modifier.weight(1f),
                title = secondaryTitle,
                data = secondaryValue
            )
        }
    }
}

@Composable
private fun LazyGradesChips(
    grades: List<String>,
    selectedGrade: String?,
    onGradeSelected: (String) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grades.forEach { grade ->
            InfoChip(
                text = grade,
                isSelected = grade == selectedGrade,
                onClick = { onGradeSelected(grade) }
            )
        }
    }
}
