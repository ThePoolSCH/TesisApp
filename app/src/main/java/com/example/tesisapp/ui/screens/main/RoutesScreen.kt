package com.example.tesisapp.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tesisapp.ui.components.FinishVisitDialog
import com.example.tesisapp.ui.components.LocationCard
import com.example.tesisapp.ui.components.LocationValidationCard

@Composable
fun RoutesScreen(viewModel: RoutesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Mostrar el diálogo si el estado así lo indica
    if (uiState.showFinishDialog) {
        FinishVisitDialog(
            locationName = uiState.activeVisitLocation?.name ?: "Ubicación desconocida",
            onDismiss = viewModel::onDismissFinishDialog,
            onConfirm = viewModel::onConfirmFinishVisit,
            onReportIncident = viewModel::onReportIncident
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECCIÓN 1: VALIDAR UBICACIÓN ---
        item {
            LocationValidationCard(
                onButtonClick = viewModel::onMainButtonClick,
                activeVisitLocation = uiState.activeVisitLocation,
                hasPendingLocations = uiState.locations.any { it.status == "Pendiente" }
            )
        }

        // --- SECCIÓN 2: LISTA DE UBICACIONES ---
        // (Esta sección no necesita cambios)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Título de la sección
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Ubicaciones",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ubicaciones por Visitar",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.locations.forEachIndexed { index, location ->
                                LocationCard(location = location, index = index)
                            }
                        }
                    }
                }
            }
        }
    }
}