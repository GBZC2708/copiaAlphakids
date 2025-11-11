package com.example.alphakids.ui.screens.student.pets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.alphakids.domain.models.PetKind
import com.example.alphakids.ui.components.BottomNavItem
import com.example.alphakids.ui.components.MainBottomBar
import com.example.alphakids.ui.components.PetStatusCard
import com.example.alphakids.ui.theme.dmSansFamily

@Composable
fun StudentPetsRoute(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentBottomRoute: String,
    modifier: Modifier = Modifier,
    viewModel: StudentPetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomNavItems = remember {
        listOf(
            BottomNavItem("home", "Inicio", Icons.Rounded.Home),
            BottomNavItem("store", "Tienda", Icons.Rounded.Store),
            BottomNavItem("pets", "Mascotas", Icons.Rounded.Pets)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StudentPetsEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    StudentPetsScreen(
        modifier = modifier,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onLogoutClick = onLogoutClick,
        onSettingsClick = onSettingsClick,
        onBottomNavClick = onBottomNavClick,
        currentBottomRoute = currentBottomRoute,
        bottomNavItems = bottomNavItems,
        onRetry = viewModel::retry,
        onFeed = viewModel::feed,
        onEquip = viewModel::equip
    )
}

@Composable
private fun StudentPetsScreen(
    modifier: Modifier,
    uiState: StudentPetsUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBottomNavClick: (String) -> Unit,
    currentBottomRoute: String,
    bottomNavItems: List<BottomNavItem>,
    onRetry: () -> Unit,
    onFeed: (String) -> Unit,
    onEquip: (String) -> Unit
) {
    val header = when (uiState) {
        is StudentPetsUiState.Success -> uiState.header
        is StudentPetsUiState.Empty -> uiState.header
        is StudentPetsUiState.Error -> uiState.header
        StudentPetsUiState.Loading -> null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mascotas",
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
            StudentPetsUiState.Loading -> LoadingContent(paddingValues)
            is StudentPetsUiState.Error -> ErrorContent(paddingValues, uiState.message, onRetry)
            is StudentPetsUiState.Empty -> EmptyContent(paddingValues, uiState.message)
            is StudentPetsUiState.Success -> SuccessContent(
                paddingValues = paddingValues,
                pets = uiState.pets,
                consumables = uiState.consumables,
                accessories = uiState.accessories,
                onFeed = onFeed,
                onEquip = onEquip
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
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = dmSansFamily
            )
            Button(onClick = onRetry) {
                Text(text = "Reintentar", fontFamily = dmSansFamily)
            }
        }
    }
}

@Composable
private fun EmptyContent(
    paddingValues: PaddingValues,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = dmSansFamily
        )
    }
}

@Composable
private fun SuccessContent(
    paddingValues: PaddingValues,
    pets: List<StudentPetCardUi>,
    consumables: List<ConsumableActionUi>,
    accessories: List<AccessoryOptionUi>,
    onFeed: (String) -> Unit,
    onEquip: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(pets) { pet ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PetStatusCard(
                    petName = petNameFor(pet.petKind),
                    petType = petTypeLabel(pet.petKind),
                    petImage = painterResource(id = petImageFor(pet.petKind)),
                    hungerProgress = pet.hunger / 100f,
                    happinessProgress = pet.happiness / 100f
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Accesorios equipados",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                    pet.equipped.forEach { equipped ->
                        AccessorySlotRow(equipped)
                    }
                }
            }
        }

        item {
            Text(
                text = "Inventario de consumibles",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(consumables, key = { it.itemId }) { item ->
            ConsumableCard(item = item, onFeed = onFeed)
        }

        if (consumables.isEmpty()) {
            item {
                Text(
                    text = "No tienes consumibles. Visita la tienda para conseguir más.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = dmSansFamily
                )
            }
        }

        item {
            Text(
                text = "Accesorios disponibles",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(accessories, key = { it.itemId }) { accessory ->
            AccessoryCardItem(accessory = accessory, onEquip = onEquip)
        }

        if (accessories.isEmpty()) {
            item {
                Text(
                    text = "Sin accesorios disponibles. Compra en la tienda para equipar a tu mascota.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = dmSansFamily
                )
            }
        }
    }
}

@Composable
private fun AccessorySlotRow(equippedAccessoryUi: EquippedAccessoryUi) {
    val slotLabel = when (equippedAccessoryUi.slot) {
        AccessorySlot.HEAD -> "Cabeza"
        AccessorySlot.NECK -> "Cuello"
        AccessorySlot.BACK -> "Espalda"
    }
    ElevatedAssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = if (equippedAccessoryUi.name != null) {
                    "$slotLabel: ${equippedAccessoryUi.name}"
                } else {
                    "$slotLabel: Sin accesorio"
                },
                fontFamily = dmSansFamily
            )
        }
    )
}

@Composable
private fun ConsumableCard(
    item: ConsumableActionUi,
    onFeed: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Cantidad: ${item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = dmSansFamily
            )
            Text(
                text = "Hambre +${item.hungerDelta} · Felicidad +${item.happinessDelta}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = dmSansFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onFeed(item.itemId) },
                enabled = item.quantity > 0 && !item.isProcessing
            ) {
                if (item.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(text = "Dar", fontFamily = dmSansFamily)
            }
        }
    }
}

@Composable
private fun AccessoryCardItem(
    accessory: AccessoryOptionUi,
    onEquip: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = accessory.name,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Slot: ${slotLabel(accessory.slot)}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = dmSansFamily
            )
            if (accessory.isEquipped) {
                Text(
                    text = "Actualmente equipado",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = dmSansFamily,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(
                onClick = { onEquip(accessory.itemId) },
                enabled = !accessory.isEquipped && !accessory.isProcessing
            ) {
                if (accessory.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    text = if (accessory.isEquipped) "Equipado" else "Equipar",
                    fontFamily = dmSansFamily
                )
            }
        }
    }
}

private fun petImageFor(petKind: PetKind): Int = when (petKind) {
    PetKind.GATO -> R.drawable.ic_happy_cat
    PetKind.PERRO -> R.drawable.ic_happy_dog
    PetKind.UNIVERSAL -> R.drawable.ic_happy_dog
}

private fun petTypeLabel(petKind: PetKind): String = when (petKind) {
    PetKind.GATO -> "Tu gato"
    PetKind.PERRO -> "Tu perro"
    PetKind.UNIVERSAL -> "Tu amigo"
}

private fun petNameFor(petKind: PetKind): String = when (petKind) {
    PetKind.GATO -> "Gato"
    PetKind.PERRO -> "Perro"
    PetKind.UNIVERSAL -> "Mascota"
}

private fun slotLabel(slot: AccessorySlot): String = when (slot) {
    AccessorySlot.HEAD -> "Cabeza"
    AccessorySlot.NECK -> "Cuello"
    AccessorySlot.BACK -> "Espalda"
}
