package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.CampaignTarget
import com.example.tesisapp.domain.model.OrderItem
import com.example.tesisapp.domain.model.Product
import com.example.tesisapp.domain.use_case.*
import com.example.tesisapp.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProductTab { CATALOG, ORDER }
enum class OrderStatus { IDLE, PROCESSING, SUCCESS, ERROR }

// Modelo de UI que combina el Producto base con su información de Meta (si existe)
data class ProductUiModel(
    val product: Product,
    val targetInfo: CampaignTarget? = null // Si es null, no es parte de una campaña activa
)

data class ProductsUiState(
    val isLoading: Boolean = true,
    val orderStatus: OrderStatus = OrderStatus.IDLE,
    val errorMessage: String? = null,

    // Datos crudos
    val rawProducts: List<Product> = emptyList(),
    val rawTargets: List<CampaignTarget> = emptyList(),

    // Datos procesados para la vista
    val displayedProducts: List<ProductUiModel> = emptyList(),

    // Filtros
    val searchQuery: String = "",
    val showOnlyTargets: Boolean = false, // Reemplaza a las categorías hardcodeadas

    val selectedTab: ProductTab = ProductTab.CATALOG,
    val cart: Map<Int, Int> = emptyMap() // Map<ProductID, Quantity> (Usamos Int ID, no String Code)
) {
    val cartItemCount: Int get() = cart.values.sum()

    val cartTotal: Double get() = cart.entries.sumOf { (productId, quantity) ->
        (rawProducts.find { it.id == productId }?.price ?: 0.0) * quantity
    }

    // Lista para mostrar en el resumen del pedido
    val cartList: List<Pair<ProductUiModel, Int>> get() = cart.mapNotNull { (productId, quantity) ->
        val product = rawProducts.find { it.id == productId }
        val target = rawTargets.find { it.productId == productId }
        if (product != null) {
            ProductUiModel(product, target) to quantity
        } else null
    }
}

@HiltViewModel
class ProductsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val syncCatalogUseCase: SyncCatalogUseCase,
    private val getCatalogUseCase: GetCatalogUseCase,
    private val syncCampaignsUseCase: SyncCampaignsUseCase,
    private val getCampaignsUseCase: GetCampaignsUseCase,
    private val submitOrderUseCase: SubmitOrderUseCase
) : ViewModel() {

    private var currentRouteLineId: Int? = null

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Cargamos catálogo y campañas apenas se crea el ViewModel
        // (No dependen del cliente específico, sino del vendedor)
        loadGeneralData()
    }

    // --- NUEVA FUNCIÓN: Recibe el ID desde la UI ---
    fun setRouteLineId(id: Int) {
        if (currentRouteLineId != id) {
            currentRouteLineId = id
            // Opcional: Si quisieras cargar precios específicos por cliente, lo harías aquí.
            // Por ahora solo guardamos el ID para usarlo al enviar el pedido.
        }
    }

    private fun loadGeneralData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Sincronizar
            launch { syncCatalogUseCase() }
            launch { syncCampaignsUseCase() }

            // 2. Observar datos locales
            combine(
                getCatalogUseCase(),
                getCampaignsUseCase()
            ) { products, campaigns ->
                val allTargets = campaigns.flatMap { it.targets }
                Pair(products, allTargets)
            }.collect { (products, targets) ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        rawProducts = products,
                        rawTargets = targets
                    )
                }
                applyFilters()
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Disparar sincronización en segundo plano (API -> Local)
            launch { syncCatalogUseCase() }
            launch { syncCampaignsUseCase() }

            // 2. Observar base de datos local (Local -> UI)
            // Combinamos Productos y Campañas para saber qué productos tienen meta
            combine(
                getCatalogUseCase(),
                getCampaignsUseCase()
            ) { products, campaigns ->
                // Aplanamos todos los targets de todas las campañas activas
                val allTargets = campaigns.flatMap { it.targets }
                Pair(products, allTargets)
            }.collect { (products, targets) ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        rawProducts = products,
                        rawTargets = targets
                    )
                }
                applyFilters()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onToggleTargetsFilter(showOnlyTargets: Boolean) {
        _uiState.update { it.copy(showOnlyTargets = showOnlyTargets) }
        applyFilters()
    }

    fun onTabSelected(tab: ProductTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onQuantityChanged(productId: Int, change: Int) {
        val currentCart = _uiState.value.cart.toMutableMap()
        val currentQuantity = currentCart[productId] ?: 0
        val newQuantity = currentQuantity + change

        if (newQuantity > 0) {
            currentCart[productId] = newQuantity
        } else {
            currentCart.remove(productId)
        }
        _uiState.update { it.copy(cart = currentCart) }
    }

    fun onProcessOrder() {
        val visitId = currentRouteLineId
        if (_uiState.value.cart.isEmpty() || visitId == null) {
            _uiState.update { it.copy(errorMessage = "Error: No hay visita activa identificada.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(orderStatus = OrderStatus.PROCESSING) }

            val items = _uiState.value.cart.map { (id, qty) ->
                OrderItem(productId = id, quantity = qty.toDouble())
            }

            // Usamos el ID que nos pasaron
            val result = submitOrderUseCase(visitId, items)

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(orderStatus = OrderStatus.SUCCESS, cart = emptyMap())
                    }
                    syncCampaignsUseCase()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(orderStatus = OrderStatus.ERROR, errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun resetOrderStatus() {
        _uiState.update { it.copy(orderStatus = OrderStatus.IDLE, errorMessage = null) }
    }

    private fun applyFilters() {
        val state = _uiState.value

        val filtered = state.rawProducts.mapNotNull { product ->
            // Buscamos si este producto tiene una meta activa
            val target = state.rawTargets.find { it.productId == product.id }

            // Filtro 1: Texto
            val matchesSearch = state.searchQuery.isBlank() ||
                    product.name.contains(state.searchQuery, ignoreCase = true) ||
                    product.code.contains(state.searchQuery, ignoreCase = true)

            // Filtro 2: Solo metas
            val matchesTarget = if (state.showOnlyTargets) target != null else true

            if (matchesSearch && matchesTarget) {
                ProductUiModel(product, target)
            } else {
                null
            }
        }

        _uiState.update { it.copy(displayedProducts = filtered) }
    }
}