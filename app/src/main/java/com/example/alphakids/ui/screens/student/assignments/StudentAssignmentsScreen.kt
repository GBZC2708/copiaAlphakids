@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.alphakids.ui.screens.student.assignments

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.alphakids.ui.theme.dmSansFamily
import kotlinx.coroutines.flow.collectLatest

@Composable
fun StudentAssignmentsRoute(
    onBackClick: () -> Unit,
    onOpenCamera: (assignmentId: String, word: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentAssignmentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is StudentAssignmentsEvent.Message ->
                    snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    StudentAssignmentsScreen(
        modifier = modifier,
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onRetry = viewModel::retry,
        onScanClick = { item ->
            if (item.attemptsLeft > 0) {
                onOpenCamera(item.id, item.word)
            }
        }
    )
}

@Composable
private fun StudentAssignmentsScreen(
    modifier: Modifier,
    uiState: StudentAssignmentsUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onScanClick: (StudentAssignmentItem) -> Unit
) {
    val header = when (uiState) {
        is StudentAssignmentsUiState.Success -> uiState.header
        is StudentAssignmentsUiState.Empty -> uiState.header
        is StudentAssignmentsUiState.Error -> uiState.header
        StudentAssignmentsUiState.Loading -> null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Palabras asignadas",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        when (uiState) {
            StudentAssignmentsUiState.Loading -> LoadingContent(paddingValues)
            is StudentAssignmentsUiState.Error -> ErrorContent(
                paddingValues = paddingValues,
                message = uiState.message,
                onRetry = onRetry
            )
            is StudentAssignmentsUiState.Empty -> EmptyContent(
                paddingValues = paddingValues,
                message = uiState.message
            )
            is StudentAssignmentsUiState.Success -> AssignmentList(
                paddingValues = paddingValues,
                assignments = uiState.assignments,
                onScanClick = onScanClick
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
                color = MaterialTheme.colorScheme.error,
                fontFamily = dmSansFamily,
                textAlign = TextAlign.Center
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
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = dmSansFamily,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Cuando completes una palabra, aparecerá aquí.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = dmSansFamily,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AssignmentList(
    paddingValues: PaddingValues,
    assignments: List<StudentAssignmentItem>,
    onScanClick: (StudentAssignmentItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(assignments, key = { it.id }) { assignment ->
            AssignmentCard(
                item = assignment,
                onScanClick = onScanClick
            )
        }
    }
}

@Composable
private fun AssignmentCard(
    item: StudentAssignmentItem,
    onScanClick: (StudentAssignmentItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RowHeader(item)
            Text(
                text = "Recompensa: +${item.rewardCoins} monedas",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = dmSansFamily,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Intentos restantes: ${item.attemptsLeft}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = dmSansFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onScanClick(item) },
                enabled = item.attemptsLeft > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (item.attemptsLeft > 0) "Escanear" else "Sin intentos")
            }
        }
    }
}

@Composable
private fun RowHeader(item: StudentAssignmentItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.word,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.maskedWord,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Longitud: ${item.word.count { !it.isWhitespace() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = dmSansFamily
            )
        }
    }
}
