package com.example.tesisapp.ui.screens.clientes

import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.data.repository.ClienteResumen
import com.example.tesisapp.data.repository.ClientesRepository
import com.example.tesisapp.data.repository.FichaCliente
import com.example.tesisapp.data.repository.Online
import com.example.tesisapp.ui.components.*
import com.example.tesisapp.ui.theme.TesisColors
import com.example.tesisapp.utils.Formato
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClientesUiState(
    val cargando: Boolean = false,
    val cartera: List<ClienteResumen> = emptyList(),
    val error: String? = null,
    val busqueda: String = "",
    val ubicacion: Pair<Double, Double>? = null,
    val ficha: FichaCliente? = null,
    val fichaError: String? = null,
    val agregando: Boolean = false,
    val mensaje: String? = null,
    val agregadaId: Int? = null
) {
    fun distancia(c: ClienteResumen): Float? = ubicacion?.let { (lat, lon) ->
        if (c.latitude == 0.0) null else FloatArray(1).also { Location.distanceBetween(lat, lon, c.latitude, c.longitude, it) }[0]
    }

    /** "Mi cartera" ordenada por cercanía (mockup A3, estado 3). */
    val visibles: List<ClienteResumen>
        get() = cartera.filter { busqueda.isBlank() || it.name.contains(busqueda, true) || it.street.contains(busqueda, true) }
            .sortedBy { distancia(it) ?: Float.MAX_VALUE }
}

@HiltViewModel
class ClientesViewModel @Inject constructor(private val repo: ClientesRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ClientesUiState())
    val ui = _ui.asStateFlow()

    fun cargarCartera() = viewModelScope.launch {
        _ui.update { it.copy(cargando = true, error = null) }
        when (val r = repo.cartera()) {
            is Online.Ok -> _ui.update { it.copy(cargando = false, cartera = r.value) }
            is Online.Error -> _ui.update { it.copy(cargando = false, error = r.message) }
        }
    }

    fun onBusqueda(t: String) = _ui.update { it.copy(busqueda = t) }
    fun onUbicacion(lat: Double, lon: Double) = _ui.update { it.copy(ubicacion = lat to lon) }

    fun cargarFicha(partnerId: Int) = viewModelScope.launch {
        _ui.update { it.copy(cargando = true, ficha = null, fichaError = null, agregadaId = null) }
        when (val r = repo.ficha(partnerId)) {
            is Online.Ok -> _ui.update { it.copy(cargando = false, ficha = r.value) }
            is Online.Error -> _ui.update { it.copy(cargando = false, fichaError = r.message) }
        }
    }

    fun visitaNoPlanificada(partnerId: Int, motivo: String) = viewModelScope.launch {
        _ui.update { it.copy(agregando = true) }
        when (val r = repo.visitaNoPlanificada(partnerId, motivo)) {
            is Online.Ok -> _ui.update {
                it.copy(agregando = false, agregadaId = r.value,
                    mensaje = listOf("Cliente agregado a tu ruta de hoy.", r.message.orEmpty()).filter { m -> m.isNotBlank() }.joinToString(" "))
            }
            is Online.Error -> _ui.update { it.copy(agregando = false, mensaje = r.message) }
        }
    }

    fun limpiarMensaje() = _ui.update { it.copy(mensaje = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Barra(titulo: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TesisColors.Fondo)
    )
}

@Composable
fun CarteraScreen(onBack: () -> Unit, onAbrirCliente: (Int) -> Unit, vm: ClientesViewModel = hiltViewModel()) {
    val s by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.cargarCartera()
        try {
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { l -> if (l != null) vm.onUbicacion(l.latitude, l.longitude) }
        } catch (_: SecurityException) { }
    }
    Scaffold(topBar = { Barra("Mi cartera", onBack) }, containerColor = TesisColors.Fondo) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                OutlinedTextField(s.busqueda, vm::onBusqueda, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Buscar cliente o calle") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = MaterialTheme.shapes.medium, colors = campoColores())
                Text("${s.cartera.size} clientes · ordenados por cercanía", style = MaterialTheme.typography.bodySmall,
                    color = TesisColors.Texto2, modifier = Modifier.padding(top = 6.dp))
            }
            when {
                s.cargando && s.cartera.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TesisColors.Negro) }
                }
                s.error != null -> item { TesisCard { EmptyState(Icons.Outlined.CloudOff, "No se pudo cargar tu cartera", s.error) } }
            }
            items(s.visibles, key = { it.id }) { c ->
                TesisCard(Modifier.clickable { onAbrirCliente(c.id) }, padding = 12) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOf(c.street, Formato.tipoCliente(c.clientType), c.visitClass.takeIf { it.isNotBlank() }?.let { "clase $it" } ?: "")
                                .filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (c.nextVisit.isNotBlank()) "Próxima visita: ${Formato.fechaCorta(c.nextVisit)}" else "Sin visita programada",
                                style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            s.distancia(c)?.let { d ->
                                Text(if (d >= 1000) "${Formato.numero(d / 1000.0, 1)} km" else "${Formato.numero(d.toDouble())} m",
                                    fontWeight = FontWeight.SemiBold)
                            }
                            if (c.inTodayRoute) StatusChip("En ruta hoy", ChipKind.INFO)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FichaClienteScreen(partnerId: Int, onBack: () -> Unit, onVisitaAgregada: () -> Unit, vm: ClientesViewModel = hiltViewModel()) {
    val s by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pedirMotivo by remember { mutableStateOf(false) }
    LaunchedEffect(partnerId) { vm.cargarFicha(partnerId); vm.cargarCartera() }
    LaunchedEffect(s.mensaje) { s.mensaje?.let { snackbar.showSnackbar(it); vm.limpiarMensaje() } }
    LaunchedEffect(s.agregadaId) { if (s.agregadaId != null) { pedirMotivo = false; onVisitaAgregada() } }

    val enRuta = s.cartera.firstOrNull { it.id == partnerId }?.inTodayRoute == true
    if (pedirMotivo) {
        MotivoSheet("Visita no planificada",
            "Se agrega a tu ruta de hoy. Si tenía una visita programada en los próximos días, se adelanta para no visitarlo dos veces.",
            "Motivo (p. ej., llamó para pedir reposición)", "Agregar a mi ruta", s.agregando,
            onCancelar = { pedirMotivo = false }, onConfirmar = { m -> vm.visitaNoPlanificada(partnerId, m) })
    }

    Scaffold(topBar = { Barra("Cliente", onBack) }, snackbarHost = { SnackbarHost(snackbar) }, containerColor = TesisColors.Fondo) { padding ->
        val f = s.ficha
        if (f == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (s.fichaError != null) EmptyState(Icons.Outlined.CloudOff, "No se pudo abrir la ficha", s.fichaError)
                else CircularProgressIndicator(color = TesisColors.Negro)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TesisCard {
                    Text(f.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(listOf(f.street, Formato.tipoCliente(f.clientType), f.visitClass.takeIf { it.isNotBlank() }?.let { "clase $it" } ?: "",
                        if (f.visitFrequency > 0) "cada ${f.visitFrequency} días" else "").filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
                    if (f.phone.isNotBlank()) Text("Tel. ${f.phone}", style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Kpi("Pedidos (12 m)", Formato.numero(f.ordersCount.toDouble()), Modifier.weight(1f))
                        Kpi("Ticket medio", Formato.pen(f.avgTicket, 0), Modifier.weight(1f))
                        Kpi("Última compra", if (f.lastOrder.isNotBlank()) Formato.fechaCorta(f.lastOrder) else "—", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    if (enRuta) StatusChip("Ya está en tu ruta de hoy", ChipKind.INFO)
                    else PrimaryButton("Visita no planificada", { pedirMotivo = true }, icon = Icons.Default.AddLocationAlt)
                }
            }
            item {
                TesisCard {
                    Text("Compras por mes (sin IGV)", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    BarrasMes(f)
                }
            }
            if (f.usualProducts.isNotEmpty()) item {
                TesisCard {
                    Text("Lo que suele comprar", fontWeight = FontWeight.Bold)
                    f.usualProducts.forEach { p ->
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${p.qty} ×", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(p.reason, style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
                            }
                        }
                    }
                }
            }
            if (f.visits.isNotEmpty()) item {
                TesisCard {
                    Text("Últimas visitas", fontWeight = FontWeight.Bold)
                    f.visits.forEach { v ->
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(Formato.fechaCorta(v.date), Modifier.width(80.dp))
                            Text(v.result.ifBlank { "—" } + if (v.unplanned) " · no planificada" else "", Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall)
                            if (v.total > 0) Text(Formato.pen(v.total, 0), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Kpi(titulo: String, valor: String, modifier: Modifier) {
    Column(modifier.background(TesisColors.Gris100, MaterialTheme.shapes.small).padding(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
        Text(valor, fontWeight = FontWeight.Bold)
    }
}

/** Barras de las compras por mes (una sola serie, sin eje doble; el máximo se etiqueta). */
@Composable
private fun BarrasMes(f: FichaCliente) {
    val maximo = (f.months.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1.0)
    Text("Máximo ${Formato.pen(maximo, 0)}", style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val n = f.months.size.coerceAtLeast(1)
        val ancho = size.width / n
        f.months.forEachIndexed { i, m ->
            val h = (m.amount / maximo * size.height).toFloat()
            drawRoundRect(TesisColors.Negro, topLeft = Offset(i * ancho + 2.dp.toPx(), size.height - h),
                size = Size(ancho - 4.dp.toPx(), h), cornerRadius = CornerRadius(3.dp.toPx()))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(f.months.firstOrNull()?.month.orEmpty(), style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
        Text(f.months.lastOrNull()?.month.orEmpty(), style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
    }
}
