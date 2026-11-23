package com.example.tesisapp.ui.screens.main

import android.location.Location as AndroidLocation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.data.sync.OutboxRepository
import com.example.tesisapp.domain.model.Route
import com.example.tesisapp.domain.model.RouteStop
import com.example.tesisapp.domain.model.TaskType
import com.example.tesisapp.domain.repository.RouteRepository
import com.example.tesisapp.domain.repository.SalesRepository
import com.example.tesisapp.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Llegada pendiente de confirmar porque el GPS está fuera del radio. */
data class FueraDeRango(val stop: RouteStop, val distanciaM: Double, val radioM: Double, val lat: Double, val lon: Double)

data class TareasVisita(val hechas: Int = 0, val total: Int = 0, val obligatoriasFaltan: Int = 0)

data class RoutesUiState(
    val isLoading: Boolean = true,
    val routeInfo: Route? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val miUbicacion: Pair<Double, Double>? = null,
    val fueraDeRango: FueraDeRango? = null,
    val showFinishSheet: Boolean = false,
    val tareas: TareasVisita = TareasVisita()
) {
    val stops: List<RouteStop> get() = routeInfo?.stops.orEmpty()
    val activeStop: RouteStop? get() = stops.firstOrNull { it.visitState == "arrived" }
    val nextStop: RouteStop? get() = stops.filter { it.visitState == "pending" }.minByOrNull { it.sequence }
    val hechas: Int get() = stops.count { it.visitState == "done" }
    val radioM: Double get() = routeInfo?.checkinRadiusM ?: 100.0

    fun distanciaA(stop: RouteStop): Double? = miUbicacion?.let { (lat, lon) -> distancia(lat, lon, stop) }
}

fun distancia(lat: Double, lon: Double, stop: RouteStop): Double {
    val r = FloatArray(1)
    AndroidLocation.distanceBetween(lat, lon, stop.latitude, stop.longitude, r)
    return r[0].toDouble()
}

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val taskRepository: TaskRepository,
    private val salesRepository: SalesRepository,
    private val outbox: OutboxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState = _uiState.asStateFlow()
    private var initialSyncStarted = false
    private var tareasJob: Job? = null
    private var visitaObservada: Int? = null

    init {
        viewModelScope.launch {
            routeRepository.getTodayRoute().collect { route ->
                _uiState.update { it.copy(isLoading = false, routeInfo = route) }
                observarTareas(_uiState.value.activeStop)
            }
        }
    }

    /** Resumen de tareas de la visita en curso para la hoja "Terminar visita". */
    private fun observarTareas(stop: RouteStop?) {
        if (stop?.id == visitaObservada) return
        visitaObservada = stop?.id
        tareasJob?.cancel()
        if (stop == null) { _uiState.update { it.copy(tareas = TareasVisita()) }; return }
        tareasJob = viewModelScope.launch {
            val defs = taskRepository.definitionsFor(stop.clientType)
            taskRepository.observeAnswers(stop.id).collect { (answers, _) ->
                val respondidas = answers.filter { a ->
                    when (val v = a.value) { null -> false; is String -> v.isNotBlank(); else -> true }
                }.map { it.definitionId }.toSet()
                _uiState.update {
                    it.copy(tareas = TareasVisita(
                        hechas = defs.count { d -> d.id in respondidas }, total = defs.size,
                        obligatoriasFaltan = defs.count { d -> d.isRequired && d.id !in respondidas }))
                }
            }
        }
    }

    fun ensureInitialSync() {
        if (initialSyncStarted) return
        initialSyncStarted = true
        syncRoute(showMessage = false)
    }

    /** Sincroniza la ruta y descarga lo necesario para trabajar sin conexión: tareas, catálogo y pedido sugerido. */
    private fun syncRoute(showMessage: Boolean) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            outbox.sincronizarAhora()
            val result = routeRepository.syncRoute()
            if (result.isSuccess) {
                launch { taskRepository.refreshDefinitions(); visitaObservada = null; observarTareas(_uiState.value.activeStop) }
                launch { salesRepository.syncProducts() }
                launch { salesRepository.syncCampaigns() }
                launch { salesRepository.syncSuggested(_uiState.value.stops.map { it.partnerId }.filter { it > 0 }) }
            }
            _uiState.update {
                it.copy(isSyncing = false, syncMessage = when {
                    result.isSuccess -> if (showMessage) "Ruta actualizada" else null
                    else -> result.exceptionOrNull()?.message ?: "Error al sincronizar"
                })
            }
        }
    }

    fun onSyncPressed() = syncRoute(showMessage = true)

    fun clearSyncMessage() = _uiState.update { it.copy(syncMessage = null) }

    fun onLocationUnavailable() = _uiState.update { it.copy(syncMessage = "Activa el GPS para validar la llegada.") }

    fun onUbicacion(lat: Double, lon: Double) = _uiState.update { it.copy(miUbicacion = lat to lon) }

    /**
     * "Validar llegada": elige la parada pendiente más cercana si está dentro del radio; si no, la siguiente de la ruta.
     * La distancia se calcula en el teléfono (funciona sin conexión) con el mismo radio que usa Odoo.
     */
    fun onValidarLlegada(lat: Double, lon: Double) {
        val s = _uiState.value
        onUbicacion(lat, lon)
        val pendientes = s.stops.filter { it.visitState == "pending" }
        if (pendientes.isEmpty()) return
        val cercana = pendientes.minByOrNull { distancia(lat, lon, it) }!!
        val stop = if (distancia(lat, lon, cercana) <= s.radioM) cercana else (s.nextStop ?: cercana)
        val d = distancia(lat, lon, stop)
        if (d > s.radioM) {
            _uiState.update { it.copy(fueraDeRango = FueraDeRango(stop, d, s.radioM, lat, lon)) }
        } else {
            viewModelScope.launch { routeRepository.registrarLlegada(stop, lat, lon, false, null) }
        }
    }

    fun onRegistrarFueraDeRango(motivo: String) {
        val f = _uiState.value.fueraDeRango ?: return
        _uiState.update { it.copy(fueraDeRango = null) }
        viewModelScope.launch { routeRepository.registrarLlegada(f.stop, f.lat, f.lon, true, motivo) }
    }

    fun onCerrarFueraDeRango() = _uiState.update { it.copy(fueraDeRango = null) }

    fun onFinalizarClick() = _uiState.update { it.copy(showFinishSheet = true) }

    fun onDismissFinish() = _uiState.update { it.copy(showFinishSheet = false) }

    fun onConfirmarFin(resultado: String, nota: String) {
        val stop = _uiState.value.activeStop ?: return
        _uiState.update { it.copy(showFinishSheet = false) }
        viewModelScope.launch {
            // Si quedaron respuestas de tareas sin enviar, se envían antes del cierre (la cola respeta el orden)
            val (answers, enviadas) = taskRepository.observeAnswers(stop.id).first()
            if (answers.isNotEmpty() && !enviadas) {
                val defs = taskRepository.definitionsFor(stop.clientType)
                taskRepository.submit(stop.id, stop.partnerName, answers, defs)
            }
            val ubic = _uiState.value.miUbicacion
            routeRepository.terminarVisita(stop, resultado, nota, ubic?.first, ubic?.second)
            _uiState.update { it.copy(syncMessage = "Visita guardada. Se enviará a Odoo en cuanto haya conexión.") }
        }
    }
}

/** Para el resumen de tareas: ¿la definición necesita foto? (se usa en la UI de tareas). */
fun TaskType.esFoto() = this == TaskType.PHOTO
