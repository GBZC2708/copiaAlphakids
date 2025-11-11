package com.example.alphakids.ui.screens.student.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alphakids.R
import com.example.alphakids.domain.models.AccessorySlot
import com.example.alphakids.domain.models.ConsumableKind
import com.example.alphakids.domain.models.PetKind
import com.example.alphakids.domain.models.StoreItemMeta
import com.example.alphakids.ui.components.BottomNavItem
import com.example.alphakids.ui.components.MainBottomBar
import com.example.alphakids.ui.components.StoreItemCard
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun StudentStoreRoute(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentBottomRoute: String,
    initialSection: StoreSection,
    modifier: Modifier = Modifier,
    viewModel: StudentStoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedSection = rememberSaveable { mutableStateOf(initialSection) }
    val bottomNavItems = remember {
        listOf(
            BottomNavItem("home", "Inicio", Home),
            BottomNavItem("store", "Tienda", Store),
            BottomNavItem("pets", "Mascotas", Pets)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StudentStoreEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LaunchedEffect(initialSection) {
        selectedSection.value = initialSection
    }

    StudentStoreScreen(
        modifier = modifier,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        selectedSectionState = selectedSection,
        onBackClick = onBackClick,
        onLogoutClick = onLogoutClick,
        onSettingsClick = onSettingsClick,
        onBottomNavClick = onBottomNavClick,
        currentBottomRoute = currentBottomRoute,
        bottomNavItems = bottomNavItems,
        onRetry = viewModel::retry,
        onPurchaseConfirmed = viewModel::purchase
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentStoreScreen(
    modifier: Modifier,
    uiState: StudentStoreUiState,
    snackbarHostState: SnackbarHostState,
    selectedSectionState: MutableState<StoreSection>,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentBottomRoute: String,
    bottomNavItems: List<BottomNavItem>,
    onRetry: () -> Unit,
    onPurchaseConfirmed: (String) -> Unit
) {
    val header = when (uiState) {
        is StudentStoreUiState.Success -> uiState.header
        is StudentStoreUiState.Empty -> uiState.header
        is StudentStoreUiState.Error -> uiState.header
        StudentStoreUiState.Loading -> null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tienda",
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
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Configuración"
                        )
                    }
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            MainBottomBar(
                items = bottomNavItems,
                currentRoute = currentBottomRoute,
                onNavigate = onBottomNavClick
            )
        }
    ) { paddingValues ->
        when (uiState) {
            StudentStoreUiState.Loading -> LoadingContent(paddingValues)
            is StudentStoreUiState.Error -> ErrorContent(
                paddingValues = paddingValues,
                message = uiState.message,
                onRetry = onRetry
            )
            is StudentStoreUiState.Empty -> EmptyContent(
                paddingValues = paddingValues,
                message = uiState.message,
                selectedSectionState = selectedSectionState
            )
            is StudentStoreUiState.Success -> SuccessContent(
                paddingValues = paddingValues,
                state = uiState,
                selectedSectionState = selectedSectionState,
                onPurchaseConfirmed = onPurchaseConfirmed
            )
        }
    }
}

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
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
private fun ErrorContent(
    paddingValues: PaddingValues,
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
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
    paddingValues: PaddingValues,
    message: String,
    selectedSectionState: MutableState<StoreSection>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SectionTabs(selectedSectionState = selectedSectionState)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp),
            fontFamily = dmSansFamily
        )
    }
}

@Composable
private fun SuccessContent(
    paddingValues: PaddingValues,
    state: StudentStoreUiState.Success,
    selectedSectionState: MutableState<StoreSection>,
    onPurchaseConfirmed: (String) -> Unit
) {
    val section = selectedSectionState.value
    val sectionsByType = state.sections.associateBy { it.section }
    val items = sectionsByType[section]?.items.orEmpty()
    val confirmItemId = remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SectionTabs(selectedSectionState = selectedSectionState)
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay artículos en esta sección.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = dmSansFamily
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val supportingText = when {
                            item.isProcessing -> "Procesando..."
                            !item.canPurchase -> item.restrictionMessage
                            item.section == StoreSection.CONSUMABLES && item.quantity > 0 ->
                                "En inventario: ${item.quantity}"
                            else -> null
                        }
                        StoreItemCard(
                            title = item.name,
                            price = item.price,
                            itemImage = painterResource(id = itemImageResource(item)),
                            onClickBuy = {
                                if (item.canPurchase && !item.isProcessing) {
                                    confirmItemId.value = item.id
                                }
                            },
                            enabled = item.canPurchase,
                            isProcessing = item.isProcessing,
                            supportingText = supportingText
                        )
                    }
                }
            }
        }

        val itemToConfirm = confirmItemId.value
        if (itemToConfirm != null) {
            val item = items.firstOrNull { it.id == itemToConfirm }
            if (item != null) {
                ConfirmPurchaseDialog(
                    itemName = item.name,
                    price = item.price,
                    onDismiss = { confirmItemId.value = null },
                    onConfirm = {
                        confirmItemId.value = null
                        onPurchaseConfirmed(item.id)
                    }
                )
            } else {
                confirmItemId.value = null
            }
        }
    }
}

@Composable
private fun SectionTabs(selectedSectionState: MutableState<StoreSection>) {
    val sections = StoreSection.values()
    TabRow(
        selectedTabIndex = sections.indexOf(selectedSectionState.value)
    ) {
        sections.forEach { section ->
            Tab(
                selected = section == selectedSectionState.value,
                onClick = { selectedSectionState.value = section },
                text = {
                    Text(
                        text = section.title,
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            )
        }
    }
}

@Composable
private fun ConfirmPurchaseDialog(
    itemName: String,
    price: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Confirmar compra", fontFamily = dmSansFamily) },
        text = {
            Text(
                text = "¿Comprar $itemName por $price monedas?",
                fontFamily = dmSansFamily
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Comprar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}

private fun itemImageResource(item: StudentStoreItemUi): Int {
    return when (item.section) {
        StoreSection.PETS -> when ((item.meta as? StoreItemMeta.Pet)?.petKind) {
            PetKind.GATO -> R.drawable.ic_happy_cat
            PetKind.PERRO -> R.drawable.ic_happy_dog
            else -> R.drawable.ic_happy_dog
        }
        StoreSection.ACCESSORIES -> when ((item.meta as? StoreItemMeta.Accessory)?.slot) {
            AccessorySlot.HEAD -> R.drawable.ic_happy_cat
            AccessorySlot.NECK -> R.drawable.ic_happy_dog
            AccessorySlot.BACK -> R.drawable.ic_fish_cat
            else -> R.drawable.ic_happy_dog
        }
        StoreSection.CONSUMABLES -> when ((item.meta as? StoreItemMeta.Consumable)?.kind) {
            ConsumableKind.CROQUETA -> R.drawable.ic_kibble_dog_cat
            ConsumableKind.HUESO -> R.drawable.ic_bone_dog
            null -> R.drawable.ic_fish_cat
        }
    }
}
