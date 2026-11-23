package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.data.repository.ClientesRepository
import com.example.tesisapp.data.repository.MisComisiones
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummaryUiState(
    val isLoading: Boolean = true,
    val datos: MisComisiones? = null,
    val actualizado: Long = 0L,
    val error: String? = null
)

/** "Mis objetivos" (mockup A5): avance y proyección por campaña, comisión estimada y liquidaciones (RF-32). */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repo: ClientesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.comisiones.collect { (datos, t) -> _uiState.update { it.copy(datos = datos, actualizado = t) } }
        }
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val error = repo.actualizarComisiones()
            _uiState.update { it.copy(isLoading = false, error = error) }
        }
    }
}
