@file:OptIn(
    ExperimentalMaterial3Api::class
)

package com.example.alphakids.ui.screens.tutor.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alphakids.ui.components.BottomNavItem
import com.example.alphakids.ui.components.CustomFAB
import com.example.alphakids.ui.components.InfoChip
import com.example.alphakids.ui.components.MainBottomBar
import com.example.alphakids.ui.components.SearchBar
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun StudentDictionaryRoute(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    onWordScanRequest: (String, String, String) -> Unit,
    currentRoute: String,
    viewModel: StudentDictionaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StudentDictionaryEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    StudentDictionaryScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onLogoutClick = onLogoutClick,
        onSettingsClick = onSettingsClick,
        onBottomNavClick = onBottomNavClick,
        currentRoute = currentRoute,
        onRetry = viewModel::retry,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onDifficultySelected = viewModel::onDifficultySelected,
        onClearFilters = viewModel::clearFilters,
        onWordClick = { item, header ->
            viewModel.onWordSelected(item.id)
            onWordScanRequest(header.studentId, item.id, item.targetWord)
        }
    )
}

@Composable
fun StudentDictionaryScreen(
    uiState: StudentDictionaryUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentRoute: String,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onDifficultySelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onWordClick: (StudentDictionaryWordItem, StudentDictionaryHeader) -> Unit
) {
    val header = when (uiState) {
        is StudentDictionaryUiState.Success -> uiState.header
        is StudentDictionaryUiState.Empty -> uiState.header
        else -> null
    }

    Scaffold(
        topBar = {
            StudentDictionaryTopBar(
                header = header,
                onBackClick = onBackClick,
                onLogoutClick = onLogoutClick
            )
        },
        bottomBar = {
            MainBottomBar(
                items = listOf(
                    BottomNavItem("home", "Inicio", Icons.Rounded.Home),
                    BottomNavItem("store", "Tienda", Icons.Rounded.Store),
                    BottomNavItem("pets", "Mascotas", Icons.Rounded.Pets)
                ),
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when (uiState) {
            StudentDictionaryUiState.Loading -> LoadingContent(padding)
            is StudentDictionaryUiState.Error -> ErrorContent(padding, uiState.message, onRetry)
            is StudentDictionaryUiState.Empty -> EmptyContent(padding, uiState.message)
            is StudentDictionaryUiState.Success -> DictionaryContent(
                padding = padding,
                state = uiState,
                onSearchQueryChange = onSearchQueryChange,
                onCategorySelected = onCategorySelected,
                onDifficultySelected = onDifficultySelected,
                onClearFilters = onClearFilters,
                onWordClick = { item -> onWordClick(item, uiState.header) }
            )
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    padding: PaddingValues,
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                fontFamily = dmSansFamily
            )
            Button(onClick = onRetry) {
                Text(text = "Reintentar")
            }
        }
    }
}

@Composable
private fun EmptyContent(
    padding: PaddingValues,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = dmSansFamily
        )
    }
}

@Composable
private fun DictionaryContent(
    padding: PaddingValues,
    state: StudentDictionaryUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onDifficultySelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onWordClick: (StudentDictionaryWordItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SearchBar(
            value = state.filters.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholderText = "Buscar en mi diccionario"
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (state.filters.categories.isNotEmpty()) {
            Text(
                text = "Categorías",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = dmSansFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilterRow(
                options = state.filters.categories,
                selected = state.filters.selectedCategory,
                onSelected = onCategorySelected,
                allLabel = "Todas"
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (state.filters.difficulties.isNotEmpty()) {
            Text(
                text = "Dificultad",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = dmSansFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilterRow(
                options = state.filters.difficulties,
                selected = state.filters.selectedDifficulty,
                onSelected = onDifficultySelected,
                allLabel = "Todas"
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (state.emptyMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = dmSansFamily
                )
                Button(onClick = onClearFilters) {
                    Text(text = "Restablecer filtros")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(items = state.items, key = { it.id }) { item ->
                DictionaryWordCard(
                    item = item,
                    onClick = { onWordClick(item) }
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    options: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
    allLabel: String = "Todas"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selected.isNullOrBlank(),
            onClick = { onSelected(null) },
            label = { Text(text = allLabel, fontFamily = dmSansFamily) },
            colors = FilterChipDefaults.filterChipColors()
        )
        options.forEach { option ->
            FilterChip(
                selected = selected?.equals(option, ignoreCase = true) == true,
                onClick = { onSelected(option) },
                label = { Text(text = option, fontFamily = dmSansFamily) },
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DictionaryWordCard(
    item: StudentDictionaryWordItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (item.isSelected) {
            CardDefaults.outlinedCardBorder()
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Book,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = item.maskedWord,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val subtitle = listOfNotNull(item.category, item.difficulty)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = dmSansFamily
                            )
                        }
                    }
                }
                if (item.rewardCoins > 0) {
                    InfoChip(text = "+${item.rewardCoins} monedas", isSelected = true)
                }
            }
            if (!item.usage.isNullOrBlank()) {
                Text(
                    text = item.usage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = dmSansFamily
                )
            }
        }
    }
}

@Composable
private fun StudentDictionaryTopBar(
    header: StudentDictionaryHeader?,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Mi diccionario",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold
                )
                if (header != null) {
                    Text(
                        text = "${header.fullName} — Monedas: ${header.coins}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = dmSansFamily
                    )
                }
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
