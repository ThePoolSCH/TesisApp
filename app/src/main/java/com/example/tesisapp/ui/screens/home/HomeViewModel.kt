package com.example.tesisapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.User
import com.example.tesisapp.domain.model.Product
import com.example.tesisapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.tesisapp.domain.use_case.GetProductsUseCase

data class HomeUiState(
    val user: User? = null,
    val products: List<Product> = emptyList(), // <-- AÑADIR LISTA DE PRODUCTOS
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    getProductsUseCase: GetProductsUseCase // <-- INYECTAR CASO DE USO
) : ViewModel() {

    // --- MODIFICAR LA CREACIÓN DEL ESTADO ---
    // Usamos 'combine' para fusionar los datos del usuario y los productos en un solo estado.
    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.getSavedUser(),
        getProductsUseCase()
    ) { user, products ->
        HomeUiState(
            user = user,
            products = products,
            isLoading = false // La carga termina cuando ambos flujos emiten.
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }
}