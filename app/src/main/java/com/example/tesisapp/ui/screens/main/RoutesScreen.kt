package com.example.tesisapp.ui.screens.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tesisapp.domain.model.RouteStop
import com.example.tesisapp.ui.components.FinishVisitDialog
import com.example.tesisapp.ui.components.LocationCard
import com.example.tesisapp.ui.components.LocationValidationCard
import com.example.tesisapp.ui.components.RouteMap // Importamos nuestro componente de mapa

@Composable
fun RoutesScreen(viewModel: RoutesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- GESTIÓN DE PERMISOS DE UBICACIÓN ---
    var isLocationPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Verificamos si alguno de los permisos de ubicación fue concedido
        isLocationPermissionGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Pedir permiso al iniciar la pantalla
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // --- SNACKBAR SYNC ---
    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    // --- DIÁLOGOS ---
    if (uiState.showFinishDialog) {
        FinishVisitDialog(
            locationName = uiState.activeVisitLocation?.name ?: "Ubicación desconocida",
            onDismiss = viewModel::onDismissFinishDialog,
            onConfirm = viewModel::onConfirmFinishVisit,
            onReportIncident = viewModel::onReportIncident
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.onSyncPressed() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargando...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sincronizar")
                }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- SECCIÓN 0: MAPA Y CABECERA ---
            item {
                if (uiState.routeInfo != null) {
                    RouteMapHeader(
                        routeName = uiState.routeInfo!!.name,
                        routeDate = uiState.routeInfo!!.date,
                        geometry = uiState.routeInfo!!.geometry,
                        stops = uiState.routeInfo!!.stops,
                        isLocationEnabled = isLocationPermissionGranted
                    )
                } else {
                    // Estado vacío (sin sincronizar)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No hay ruta cargada.", style = MaterialTheme.typography.bodyLarge)
                            Text("Presiona 'Sincronizar' para obtener tus visitas.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // --- SECCIÓN 1: VALIDAR UBICACIÓN ---
            item {
                LocationValidationCard(
                    onButtonClick = viewModel::onMainButtonClick,
                    activeVisitLocation = uiState.activeVisitLocation,
                    hasPendingLocations = uiState.locations.any { it.status == "Pendiente" }
                )
            }

            // --- SECCIÓN 2: LISTA DE UBICACIONES ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Ubicaciones",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ubicaciones por Visitar",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (uiState.locations.isEmpty()) {
                            Text("No hay clientes en la lista.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.locations.forEachIndexed { index, location ->
                                    LocationCard(location = location, index = index)
                                }
                            }
                        }
                    }
                }
            }

            // Espacio extra al final
            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }
}

// --- COMPOSABLE HEADER DEL MAPA ---
@Composable
fun RouteMapHeader(
    routeName: String,
    routeDate: String,
    geometry: List<List<Double>>,
    stops: List<RouteStop>,
    isLocationEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = routeName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = routeDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contenedor del Mapa
            Card(
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Altura generosa para ver bien
            ) {
                // Aquí llamamos al componente de OSMDroid
                RouteMap(
                    modifier = Modifier.fillMaxSize(),
                    geometry = geometry,
                    stops = stops,
                    isUserLocationEnabled = isLocationEnabled
                )
            }
        }
    }
}