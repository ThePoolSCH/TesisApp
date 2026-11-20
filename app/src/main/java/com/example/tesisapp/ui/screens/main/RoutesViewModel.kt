package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.Location
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.tesisapp.domain.repository.RouteRepository    // El NUEVO repo
import com.example.tesisapp.domain.model.Route    // El modelo de Odoo
import android.location.Location as AndroidLocation // Alias para no confundir con tu modelo


data class RoutesUiState(
    val isLoading: Boolean = true,
    val locations: List<Location> = emptyList(),
    val activeVisitLocation: Location? = null,
    val showFinishDialog: Boolean = false,
    val routeInfo: Route? = null, // Para mostrar nombre, fecha y geometría en el mapa
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val routeRepository: RouteRepository        // Inyectamos el nuevo repo de Odoo
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState = _uiState.asStateFlow()
    private var initialSyncStarted = false

    init {
        viewModelScope.launch {
            routeRepository.getTodayRoute().collect { odooRoute ->
                if (odooRoute != null) {
                    val mappedLocations = odooRoute.stops.map { stop ->
                        Location(
                            id = stop.id,
                            name = stop.partnerName,
                            address = stop.address,
                            status = when (stop.visitState) {
                                "arrived" -> "En visita"  // Odoo 'arrived' = App 'En visita'
                                "done" -> "Visitada"      // Odoo 'done' = App 'Visitada'
                                "skipped" -> "No visitada"
                                else -> "Pendiente"
                            },
                               // Asumiendo que Location tiene lat/lon, si no, agrégalos a tu data class
                            latitude = stop.latitude,
                            longitude = stop.longitude
                        )
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            routeInfo = odooRoute, // Guardamos la info de la ruta (geometría, fecha)
                            locations = mappedLocations,
                            activeVisitLocation = mappedLocations.firstOrNull { it.status == "En visita" }
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            routeInfo = null,
                            locations = emptyList(),
                            activeVisitLocation = null
                        )
                    }
                }
            }
        }
    }

    fun ensureInitialSync() {
        if (initialSyncStarted) return
        initialSyncStarted = true
        syncRoute(showMessage = false)
    }

    private fun syncRoute(showMessage: Boolean) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = routeRepository.syncRoute()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = when {
                        !showMessage -> null
                        result.isSuccess -> "Ruta actualizada"
                        else -> result.exceptionOrNull()?.message ?: "Error al sincronizar"
                    }
                )
            }
        }
    }

    fun onSyncPressed() {
        syncRoute(showMessage = true)
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }

    fun onLocationUnavailable() {
        _uiState.update { it.copy(syncMessage = "Activa el GPS para iniciar la visita.") }
    }

    // --- TUS FUNCIONES EXISTENTES (Ligeramente adaptadas) ---

    fun onMainButtonClick(userLat: Double, userLon: Double) {
        val currentState = _uiState.value

        // A) INICIAR VISITA
        if (currentState.activeVisitLocation == null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }

                // 1. Filtrar pendientes
                val pendingStops = currentState.locations.filter { it.status == "Pendiente" }

                if (pendingStops.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val nearestStop = pendingStops.minByOrNull { stop ->
                    val stopLoc = AndroidLocation("stop")
                    stopLoc.latitude = stop.latitude ?: 0.0
                    stopLoc.longitude = stop.longitude ?: 0.0
                    val userLoc = AndroidLocation("user")
                    userLoc.latitude = userLat
                    userLoc.longitude = userLon
                    userLoc.distanceTo(stopLoc)
                }

                if (nearestStop != null) {
                    val result = routeRepository.startVisit(nearestStop.id, userLat, userLon)

                    if (result.isSuccess) {
                        updateLocationStatusLocal(nearestStop.id, "En visita")
                    } else {
                        _uiState.update {
                            it.copy(syncMessage = result.exceptionOrNull()?.message ?: "No se pudo iniciar la visita")
                        }
                    }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        else {
            _uiState.update { it.copy(showFinishDialog = true) }
        }

    }

    fun onConfirmFinishVisit() {
        viewModelScope.launch {
            val activeLocation = _uiState.value.activeVisitLocation

            if (activeLocation != null) {
                updateLocationStatusLocal(activeLocation.id, "Visitada")
                val result = routeRepository.finishVisit(activeLocation.id)
                if (result.isFailure) {
                    _uiState.update {
                        it.copy(syncMessage = result.exceptionOrNull()?.message ?: "No se pudo finalizar la visita")
                    }
                }
            }
            onDismissFinishDialog()
        }
    }
    fun onDismissFinishDialog() {
        _uiState.update { it.copy(showFinishDialog = false) }
    }

    fun onReportIncident() {
        println("DEBUG: Incidente reportado")
        onDismissFinishDialog()
    }

    // Helper para actualizar la lista localmente sin recargar todo
    private fun updateLocationStatusLocal(id: Int, newStatus: String) {
        val updatedList = _uiState.value.locations.map { loc ->
            if (loc.id == id) loc.copy(status = newStatus) else loc
        }
        _uiState.update {
            it.copy(
                locations = updatedList,
                activeVisitLocation = updatedList.firstOrNull { l -> l.status == "En visita" }
            )
        }
    }
}