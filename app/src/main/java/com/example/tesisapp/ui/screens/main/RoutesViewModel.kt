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
    private val locationRepository: LocationRepository, // Mantenemos tu repo antiguo si lo usas para lógica local
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
                                "arrived" -> "Visitada"
                                "skipped" -> "No visitada"
                                else -> "Pendiente" // 'pending'
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

    fun onMainButtonClick() {
        if (_uiState.value.activeVisitLocation == null) {
            // Iniciar visita
            viewModelScope.launch {
                // Aquí deberías actualizar el estado en la BD local también si quieres persistencia real
                val pendingLocation = _uiState.value.locations
                    .firstOrNull { it.status == "Pendiente" } // Tomamos el primero en orden, no random

                if (pendingLocation != null) {
                    // Actualizamos el estado en memoria (y en UI)
                    updateLocationStatusLocal(pendingLocation.id, "En visita")
                }
            }
        } else {
            _uiState.update { it.copy(showFinishDialog = true) }
        }
    }

    fun onDismissFinishDialog() {
        _uiState.update { it.copy(showFinishDialog = false) }
    }

    fun onConfirmFinishVisit() {
        viewModelScope.launch {
            _uiState.value.activeVisitLocation?.let {
                updateLocationStatusLocal(it.id, "Visitada")
                // TODO: Aquí podrías mandar una petición a Odoo para marcar "arrived" en el servidor
            }
            onDismissFinishDialog()
        }
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