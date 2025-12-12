package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.Campaign
import com.example.tesisapp.domain.use_case.GetCampaignsUseCase
import com.example.tesisapp.domain.use_case.SyncCampaignsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummaryUiState(
    val isLoading: Boolean = true,
    val campaigns: List<Campaign> = emptyList()
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val getCampaignsUseCase: GetCampaignsUseCase,
    private val syncCampaignsUseCase: SyncCampaignsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCampaigns()
        refreshData()
    }

    private fun loadCampaigns() {
        viewModelScope.launch {
            getCampaignsUseCase().collectLatest { campaigns ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        campaigns = campaigns
                    )
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Sincronizamos con Odoo para tener los últimos % de avance
            syncCampaignsUseCase()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}