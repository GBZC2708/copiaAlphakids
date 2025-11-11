@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.alphakids.ui.screens.tutor.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun StudentTabsRoute(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
    onAssignmentsClick: (String) -> Unit,
    onDictionaryClick: (String) -> Unit,
    onStorePetsClick: (String) -> Unit,
    onStoreAccessoriesClick: (String) -> Unit,
    onStoreConsumablesClick: (String) -> Unit,
    onPetsClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentTabsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StudentTabsScreen(
        modifier = modifier,
        uiState = uiState,
        onBackClick = onBackClick,
        onLogoutClick = onLogoutClick,
        onSettingsClick = onSettingsClick,
        onAssignmentsClick = onAssignmentsClick,
        onDictionaryClick = onDictionaryClick,
        onStorePetsClick = onStorePetsClick,
        onStoreAccessoriesClick = onStoreAccessoriesClick,
        onStoreConsumablesClick = onStoreConsumablesClick,
        onPetsClick = onPetsClick
    )
}

@Composable
private fun StudentTabsScreen(
    modifier: Modifier = Modifier,
    uiState: StudentTabsUiState,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
    onAssignmentsClick: (String) -> Unit,
    onDictionaryClick: (String) -> Unit,
    onStorePetsClick: (String) -> Unit,
    onStoreAccessoriesClick: (String) -> Unit,
    onStoreConsumablesClick: (String) -> Unit,
    onPetsClick: (String) -> Unit
) {
    when (uiState) {
        StudentTabsUiState.Loading -> StudentTabsLoading(modifier)
        is StudentTabsUiState.Error -> StudentTabsMessage(modifier, uiState.message)
        is StudentTabsUiState.Empty -> StudentTabsMessage(modifier, uiState.message)
        is StudentTabsUiState.Success -> StudentTabsContent(
            modifier = modifier,
            header = uiState.header,
            onBackClick = onBackClick,
            onLogoutClick = onLogoutClick,
            onSettingsClick = onSettingsClick,
            onAssignmentsClick = onAssignmentsClick,
            onDictionaryClick = onDictionaryClick,
            onStorePetsClick = onStorePetsClick,
            onStoreAccessoriesClick = onStoreAccessoriesClick,
            onStoreConsumablesClick = onStoreConsumablesClick,
            onPetsClick = onPetsClick
        )
    }
}

@Composable
private fun StudentTabsContent(
    modifier: Modifier,
    header: StudentHeaderState,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
    onAssignmentsClick: (String) -> Unit,
    onDictionaryClick: (String) -> Unit,
    onStorePetsClick: (String) -> Unit,
    onStoreAccessoriesClick: (String) -> Unit,
    onStoreConsumablesClick: (String) -> Unit,
    onPetsClick: (String) -> Unit
) {
    val tabNavController = rememberNavController()
    val tabs = remember { StudentTab.values().toList() }
    var selectedTabRoute by rememberSaveable { mutableStateOf(StudentTab.Play.route) }

    DisposableEffect(tabNavController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val newTab = StudentTab.fromRoute(destination.route)
            if (newTab != null) {
                selectedTabRoute = newTab.route
            }
        }
        tabNavController.addOnDestinationChangedListener(listener)
        onDispose { tabNavController.removeOnDestinationChangedListener(listener) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            StudentTabsTopBar(
                title = "Explora AlphaKids",
                subtitle = "${header.fullName} — Monedas: ${header.coins}",
                onBackClick = onBackClick,
                onLogoutClick = onLogoutClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onSettingsClick(header.id) }) {
                Icon(imageVector = Icons.Rounded.Settings, contentDescription = "Configuración")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StudentTabRow(
                tabs = tabs,
                selectedRoute = selectedTabRoute,
                onTabSelected = { tab ->
                    if (selectedTabRoute != tab.route) {
                        selectedTabRoute = tab.route
                        tabNavController.navigate(tab.route) {
                            val startRoute = tabNavController.graph.startDestinationRoute
                                ?: StudentTab.Play.route
                            popUpTo(startRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
            Box(modifier = Modifier.fillMaxSize()) {
                StudentTabsNavHost(
                    navController = tabNavController,
                    header = header,
                    onAssignmentsClick = onAssignmentsClick,
                    onDictionaryClick = onDictionaryClick,
                    onStorePetsClick = onStorePetsClick,
                    onStoreAccessoriesClick = onStoreAccessoriesClick,
                    onStoreConsumablesClick = onStoreConsumablesClick,
                    onPetsClick = onPetsClick
                )
            }
        }
    }
}

@Composable
private fun StudentTabsNavHost(
    navController: NavHostController,
    header: StudentHeaderState,
    onAssignmentsClick: (String) -> Unit,
    onDictionaryClick: (String) -> Unit,
    onStorePetsClick: (String) -> Unit,
    onStoreAccessoriesClick: (String) -> Unit,
    onStoreConsumablesClick: (String) -> Unit,
    onPetsClick: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = StudentTab.Play.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(StudentTab.Play.route) {
            StudentPlayTab(
                studentName = header.fullName,
                onAssignmentsClick = { onAssignmentsClick(header.id) },
                onDictionaryClick = { onDictionaryClick(header.id) }
            )
        }
        composable(StudentTab.Dictionary.route) {
            StudentDictionaryTab(
                studentName = header.fullName,
                onDictionaryClick = { onDictionaryClick(header.id) }
            )
        }
        composable(StudentTab.Achievements.route) {
            StudentAchievementsTab(studentName = header.fullName)
        }
        composable(StudentTab.Store.route) {
            StudentStoreTab(
                studentName = header.fullName,
                onStorePetsClick = { onStorePetsClick(header.id) },
                onStoreAccessoriesClick = { onStoreAccessoriesClick(header.id) },
                onStoreConsumablesClick = { onStoreConsumablesClick(header.id) }
            )
        }
        composable(StudentTab.Pets.route) {
            StudentPetsTab(
                studentName = header.fullName,
                onPetsClick = { onPetsClick(header.id) }
            )
        }
    }
}

@Composable
private fun StudentTabsTopBar(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar"
                )
            }
        },
        actions = {
            IconButton(onClick = onLogoutClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Cerrar sesión"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun StudentTabRow(
    tabs: List<StudentTab>,
    selectedRoute: String,
    onTabSelected: (StudentTab) -> Unit
) {
    val selectedIndex = tabs.indexOfFirst { it.route == selectedRoute }.let { index ->
        if (index >= 0) index else 0
    }
    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEach { tab ->
            Tab(
                selected = tab.route == selectedRoute,
                onClick = { onTabSelected(tab) },
                text = { Text(text = tab.label) }
            )
        }
    }
}

@Composable
private fun StudentPlayTab(
    studentName: String,
    onAssignmentsClick: () -> Unit,
    onDictionaryClick: () -> Unit
) {
    StudentTabContainer {
        Text(
            text = "Jugar con palabras",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$studentName puede practicar sus palabras asignadas o repasar el diccionario.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onAssignmentsClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ir a palabras asignadas")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onDictionaryClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Abrir diccionario")
        }
    }
}

@Composable
private fun StudentDictionaryTab(
    studentName: String,
    onDictionaryClick: () -> Unit
) {
    StudentTabContainer {
        Text(
            text = "Diccionario",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = "Accede al diccionario completo de $studentName.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onDictionaryClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ver diccionario")
        }
    }
}

@Composable
private fun StudentAchievementsTab(studentName: String) {
    StudentTabContainer {
        Text(
            text = "Mis logros",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = "$studentName puede revisar sus logros desbloqueados.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StudentStoreTab(
    studentName: String,
    onStorePetsClick: () -> Unit,
    onStoreAccessoriesClick: () -> Unit,
    onStoreConsumablesClick: () -> Unit
) {
    StudentTabContainer {
        Text(
            text = "Tienda",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = "Compra recompensas para $studentName.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick = onStorePetsClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ver mascotas")
        }
        Spacer(modifier = Modifier.padding(vertical = 4.dp))
        Button(onClick = onStoreAccessoriesClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ver accesorios")
        }
        Spacer(modifier = Modifier.padding(vertical = 4.dp))
        Button(onClick = onStoreConsumablesClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ver consumibles")
        }
    }
}

@Composable
private fun StudentPetsTab(
    studentName: String,
    onPetsClick: () -> Unit
) {
    StudentTabContainer {
        Text(
            text = "Mascotas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = "Administra las mascotas de $studentName.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick = onPetsClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ir a mis mascotas")
        }
    }
}

@Composable
private fun StudentTabsLoading(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun StudentTabsMessage(modifier: Modifier, message: String) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StudentTabContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

private enum class StudentTab(val route: String, val label: String) {
    Play("student_tab_play", "Jugar"),
    Dictionary("student_tab_dictionary", "Diccionario"),
    Achievements("student_tab_achievements", "Mis logros"),
    Store("student_tab_store", "Tienda"),
    Pets("student_tab_pets", "Mascotas");

    companion object {
        fun fromRoute(route: String?): StudentTab? {
            return values().firstOrNull { it.route == route }
        }
    }
}
