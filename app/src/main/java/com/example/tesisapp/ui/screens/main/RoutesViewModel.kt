package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.Location
import com.example.tesisapp.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
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

    init {
        // 1. Observar la Ruta Local (Base de datos Room alimentada por Odoo)
        viewModelScope.launch {
            routeRepository.getTodayRoute().collect { odooRoute ->
                if (odooRoute != null) {
                    // CONVERTIR datos de Odoo -> Tus datos de UI (Location)
                    val mappedLocations = odooRoute.stops.map { stop ->
                        Location(
                            id = stop.id,
                            name = stop.partnerName,
                            address = stop.address,
                            // Mapeo de estados de Odoo a tus estados de UI
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
                            // Recalcular visita activa basado en los nuevos datos
                            activeVisitLocation = mappedLocations.firstOrNull { it.status == "En visita" }
                        )
                    }
                } else {
                    // Si no hay ruta de Odoo, podrías cargar tus datos mock antiguos aquí
                    // loadMockLocations()
                }
            }
        }
    }

    // --- NUEVA FUNCIÓN: Sincronizar con Odoo ---
    fun onSyncPressed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }

            val result = routeRepository.syncRoute()

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = if (result.isSuccess) "Ruta actualizada" else "Error al sincronizar"
                )
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
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

                // 2. Calcular más cercano
                val nearestStop = pendingStops.minByOrNull { stop ->
                    val stopLoc = AndroidLocation("stop")
                    stopLoc.latitude = stop.latitude ?: 0.0
                    stopLoc.longitude = stop.longitude ?: 0.0
                    val userLoc = AndroidLocation("user")
                    userLoc.latitude = userLat
                    userLoc.longitude = userLon
                    userLoc.distanceTo(stopLoc)
                }

                // 3. Iniciar (Backend + Local)
                if (nearestStop != null) {
                    // Llamamos a Odoo para marcar check-in
                    val result = routeRepository.startVisit(nearestStop.id, userLat, userLon)

                    if (result.isSuccess) {
                        // IMPORTANTE: Cambiamos estado a "En visita".
                        // Esto hace que activeVisitLocation deje de ser null automáticamente
                        // y se habiliten los tabs en MainScreen.
                        updateLocationStatusLocal(nearestStop.id, "En visita")
                    } else {
                        // Manejo de error
                    }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        // B) FINALIZAR VISITA (Abrir diálogo)
        else {
            _uiState.update { it.copy(showFinishDialog = true) }
        }

    }

    fun onConfirmFinishVisit() {
        viewModelScope.launch {
            val activeLocation = _uiState.value.activeVisitLocation

            if (activeLocation != null) {
                // 1. Actualización visual inmediata
                updateLocationStatusLocal(activeLocation.id, "Visitada")

                // 2. Llamada al Backend (Checkout)
                routeRepository.finishVisit(activeLocation.id)
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