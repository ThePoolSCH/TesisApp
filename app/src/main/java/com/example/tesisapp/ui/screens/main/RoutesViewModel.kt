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

data class RoutesUiState(
    val isLoading: Boolean = true,
    val locations: List<Location> = emptyList(),
    // Rastrea la ubicación que está actualmente "En visita"
    val activeVisitLocation: Location? = null,
    // Controla la visibilidad del diálogo de finalización
    val showFinishDialog: Boolean = false
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.seedLocationsIfEmpty()

            locationRepository.getLocations().map { locations ->
                locations.sortedWith(compareBy {
                    when (it.status) {
                        "En visita" -> 0
                        "Pendiente" -> 1
                        else -> 2
                    }
                })
            }.collect { sortedLocations ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        locations = sortedLocations,
                        // Busca si hay una visita activa y la guarda en el estado
                        activeVisitLocation = sortedLocations.firstOrNull { loc -> loc.status == "En visita" }
                    )
                }
            }
        }
    }

    /**
     * Maneja el clic en el botón principal.
     * Si no hay visita activa, inicia una.
     * Si hay una visita activa, muestra el diálogo para terminarla.
     */
    fun onMainButtonClick() {
        if (_uiState.value.activeVisitLocation == null) {
            // Iniciar visita
            viewModelScope.launch {
                val pendingLocation = _uiState.value.locations
                    .filter { it.status == "Pendiente" }
                    .randomOrNull()

                pendingLocation?.let {
                    locationRepository.updateLocationStatus(it.id, "En visita")
                }
            }
        } else {
            // Mostrar diálogo para terminar visita
            _uiState.update { it.copy(showFinishDialog = true) }
        }
    }

    fun onDismissFinishDialog() {
        _uiState.update { it.copy(showFinishDialog = false) }
    }

    /**
     * Confirma la finalización de la visita, cambiando el estado a "Visitada".
     */
    fun onConfirmFinishVisit() {
        viewModelScope.launch {
            _uiState.value.activeVisitLocation?.let {
                locationRepository.updateLocationStatus(it.id, "Visitada")
            }
            // Ocultamos el diálogo después de confirmar
            onDismissFinishDialog()
        }
    }

    /**
     * Placeholder para la lógica de reportar incidente.
     */
    fun onReportIncident() {
        // Aquí iría la lógica para navegar a una pantalla de reporte, etc.
        println("DEBUG: Incidente reportado para la ubicación ${_uiState.value.activeVisitLocation?.name}")
        onDismissFinishDialog()
    }
}