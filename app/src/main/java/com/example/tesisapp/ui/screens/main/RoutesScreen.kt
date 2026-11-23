package com.example.tesisapp.ui.screens.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tesisapp.domain.model.RouteStop
import com.example.tesisapp.ui.components.*
import com.example.tesisapp.ui.theme.TesisColors
import com.example.tesisapp.utils.Formato
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun RoutesScreen(
    viewModel: RoutesViewModel,
    onAbrirPedido: () -> Unit,
    onAbrirTareas: () -> Unit,
    onAbrirCliente: (Int) -> Unit,
    onAbrirCartera: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    var permiso by remember { mutableStateOf(false) }

    // Ubicación actual: la última conocida al instante y una lectura fresca de alta precisión
    fun conUbicacion(accion: (Double, Double) -> Unit) {
        try {
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc -> if (loc != null) accion(loc.latitude, loc.longitude) else viewModel.onLocationUnavailable() }
                .addOnFailureListener { viewModel.onLocationUnavailable() }
        } catch (e: SecurityException) {
            viewModel.onLocationUnavailable()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { p ->
        permiso = p[Manifest.permission.ACCESS_FINE_LOCATION] == true || p[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (permiso) conUbicacion(viewModel::onUbicacion)
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSyncMessage() }
    }

    uiState.fueraDeRango?.let { f ->
        FueraDeRangoSheet(f.stop.partnerName, f.distanciaM, f.radioM, viewModel::onCerrarFueraDeRango, viewModel::onRegistrarFueraDeRango)
    }
    val activa = uiState.activeStop
    if (uiState.showFinishSheet && activa != null) {
        TerminarVisitaSheet(
            nombre = activa.partnerName,
            minutos = if (activa.checkinAt > 0) (System.currentTimeMillis() - activa.checkinAt) / 60000 else 0,
            vendido = activa.totalSold, tareasHechas = uiState.tareas.hechas, tareasTotal = uiState.tareas.total,
            obligatoriasFaltan = uiState.tareas.obligatoriasFaltan,
            onCancelar = viewModel::onDismissFinish, onFinalizar = viewModel::onConfirmarFin
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = TesisColors.Fondo,
        floatingActionButton = {
            // Durante la visita el botón tapaba "Finalizar visita" (hallazgo de los mockups): se oculta
            if (activa == null) {
                ExtendedFloatingActionButton(onClick = viewModel::onSyncPressed, containerColor = TesisColors.Negro,
                    contentColor = TesisColors.Blanco) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = TesisColors.Blanco, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp)); Text("Cargando...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("Sincronizar")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { RutaCard(uiState, permiso, onAbrirCartera) }

            item {
                when {
                    uiState.routeInfo == null -> {}
                    activa != null -> VisitaEnCursoCard(activa, uiState, onAbrirPedido, onAbrirTareas,
                        { onAbrirCliente(activa.partnerId) }, viewModel::onFinalizarClick)
                    uiState.nextStop != null -> ValidarLlegadaCard(uiState.nextStop!!, uiState) {
                        conUbicacion(viewModel::onValidarLlegada)
                    }
                    else -> TesisCard { EmptyState(Icons.Default.TaskAlt, "Ruta del día terminada", "Visitaste todas las paradas.") }
                }
            }

            if (uiState.stops.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Storefront, null, Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ubicaciones por Visitar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                itemsIndexed(uiState.stops, key = { _, s -> s.id }) { i, s ->
                    StopCard(s, i, isNext = activa == null && s.id == uiState.nextStop?.id)
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun RutaCard(s: RoutesUiState, permiso: Boolean, onAbrirCartera: () -> Unit) {
    TesisCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Ruta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                val r = s.routeInfo
                Text(if (r != null) "${Formato.fechaCorta(r.date)} · ${r.stops.size} paradas" else "Sin ruta descargada",
                    color = TesisColors.Texto2)
            }
            SecondaryButton("Cartera", onAbrirCartera, icon = Icons.Default.People, height = 40)
        }
        val r = s.routeInfo
        if (r == null) {
            Spacer(Modifier.height(12.dp))
            Text(if (s.isLoading) "Cargando..." else "Presiona \"Sincronizar\" para obtener tus visitas.",
                color = TesisColors.Texto2, style = MaterialTheme.typography.bodySmall)
            return@TesisCard
        }
        Spacer(Modifier.height(12.dp))
        ProgresoRuta(s.hechas, r.stops.size)
        Spacer(Modifier.height(12.dp))
        Card(shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().height(280.dp)) {
            RouteMap(modifier = Modifier.fillMaxSize(), geometry = r.geometry, stops = r.stops, isUserLocationEnabled = permiso)
        }
    }
}

@Composable
private fun ValidarLlegadaCard(next: RouteStop, s: RoutesUiState, onValidar: () -> Unit) {
    TesisCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null)
                Spacer(Modifier.width(4.dp))
                Text("Validación de Ubicación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text("Próxima parada: ${next.partnerName}", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            val d = s.distanciaA(next)
            val partes = listOfNotNull(
                d?.let { if (it >= 1000) "Estás a ${Formato.numero(it / 1000, 1)} km" else "Estás a ${Formato.numero(it)} m" },
                next.plannedArrival.takeIf { it.isNotBlank() }?.let { "llegada estimada $it" }
            )
            if (partes.isNotEmpty()) Text(partes.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Validar llegada", onValidar, icon = Icons.Default.CheckCircle)
            Text("Sin conexión la llegada se valida con el GPS y se envía después.",
                style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun VisitaEnCursoCard(stop: RouteStop, s: RoutesUiState, onPedido: () -> Unit, onTareas: () -> Unit,
                              onHistorial: () -> Unit, onFinalizar: () -> Unit) {
    TesisCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Cronometro(stop.checkinAt) }
        Spacer(Modifier.height(10.dp))
        Text("Estás visitando a:", color = TesisColors.Texto2)
        Text(stop.partnerName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        val detalle = listOf(stop.street, Formato.tipoCliente(stop.clientType),
            stop.visitClass.takeIf { it.isNotBlank() }?.let { "clase $it" } ?: "",
            if (stop.outOfRange) "llegada fuera de rango" else "").filter { it.isNotBlank() }.joinToString(" · ")
        Text(detalle, style = MaterialTheme.typography.bodySmall, color = if (stop.outOfRange) TesisColors.Aviso else TesisColors.Texto2)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Pedido", onPedido, Modifier.weight(1f), Icons.Default.ShoppingCart, height = 44)
            val t = s.tareas
            SecondaryButton(if (t.total > 0) "Tareas ${t.hechas}/${t.total}" else "Tareas", onTareas, Modifier.weight(1f),
                Icons.Default.CheckBox, height = 44)
            SecondaryButton("Historial", onHistorial, Modifier.weight(1f), Icons.Default.History, height = 44,
                enabled = stop.partnerId > 0)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Finalizar visita", onFinalizar, icon = Icons.Default.Stop)
    }
}
