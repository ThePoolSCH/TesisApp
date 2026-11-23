package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.*
import com.example.tesisapp.domain.repository.RouteRepository
import com.example.tesisapp.domain.repository.SalesRepository
import com.example.tesisapp.domain.use_case.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProductTab { CATALOG, ORDER }
enum class OrderStatus { IDLE, SUCCESS, ERROR }

// Modelo de UI: producto + su meta de campaña (si tiene) + si está en el pedido sugerido del cliente
data class ProductUiModel(
    val product: Product,
    val targetInfo: CampaignTarget? = null,
    val suggestion: SuggestedItem? = null
)

const val FILTRO_TODOS = "Todos"
const val FILTRO_SUGERIDOS = "Sugeridos"
const val FILTRO_METAS = "Solo metas"

data class ProductsUiState(
    val isLoading: Boolean = true,
    val orderStatus: OrderStatus = OrderStatus.IDLE,
    val errorMessage: String? = null,
    val stop: RouteStop? = null,
    val rawProducts: List<Product> = emptyList(),
    val rawTargets: List<CampaignTarget> = emptyList(),
    val suggested: SuggestedOrder? = null,
    val displayedProducts: List<ProductUiModel> = emptyList(),
    val searchQuery: String = "",
    val filtro: String = FILTRO_TODOS,
    val selectedTab: ProductTab = ProductTab.CATALOG,
    val cart: Map<Int, Int> = emptyMap()
) {
    val cartItemCount: Int get() = cart.values.sum()
    val cartTotal: Double get() = cart.entries.sumOf { (id, q) -> (rawProducts.find { it.id == id }?.price ?: 0.0) * q }
    val cartList: List<Pair<ProductUiModel, Int>> get() = cart.mapNotNull { (id, q) ->
        rawProducts.find { it.id == id }?.let { p -> modelo(p) to q }
    }
    val categorias: List<String> get() = rawProducts.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    val filtros: List<String> get() = listOf(FILTRO_TODOS, FILTRO_SUGERIDOS, FILTRO_METAS) + categorias

    fun modelo(p: Product) = ProductUiModel(p, rawTargets.find { it.productId == p.id },
        suggested?.items?.find { it.productId == p.id })
}

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val syncCatalogUseCase: SyncCatalogUseCase,
    private val getCatalogUseCase: GetCatalogUseCase,
    private val syncCampaignsUseCase: SyncCampaignsUseCase,
    private val getCampaignsUseCase: GetCampaignsUseCase,
    private val submitOrderUseCase: SubmitOrderUseCase,
    private val salesRepository: SalesRepository,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch { syncCatalogUseCase() }
            launch { syncCampaignsUseCase() }
            combine(getCatalogUseCase(), getCampaignsUseCase()) { products, campaigns ->
                products to campaigns.flatMap { it.targets }
            }.collect { (products, targets) ->
                _uiState.update { it.copy(isLoading = false, rawProducts = products, rawTargets = targets) }
                applyFilters()
            }
        }
    }

    /** Visita en curso: el pedido queda ligado a ella y se carga el pedido sugerido del cliente (sin conexión). */
    fun setStop(stop: RouteStop) {
        if (_uiState.value.stop?.id == stop.id) return
        _uiState.update { it.copy(stop = stop, cart = emptyMap(), suggested = null, filtro = FILTRO_TODOS) }
        viewModelScope.launch {
            var sug = salesRepository.getSuggested(stop.partnerId)
            if (sug == null && stop.partnerId > 0) {
                salesRepository.syncSuggested(listOf(stop.partnerId))
                sug = salesRepository.getSuggested(stop.partnerId)
            }
            _uiState.update { it.copy(suggested = sug) }
            applyFilters()
        }
    }

    fun onSearchQueryChanged(query: String) { _uiState.update { it.copy(searchQuery = query) }; applyFilters() }

    fun onFiltro(f: String) { _uiState.update { it.copy(filtro = f) }; applyFilters() }

    fun onTabSelected(tab: ProductTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun onQuantityChanged(productId: Int, change: Int) {
        val cart = _uiState.value.cart.toMutableMap()
        val q = (cart[productId] ?: 0) + change
        if (q > 0) cart[productId] = q else cart.remove(productId)
        _uiState.update { it.copy(cart = cart) }
    }

    /** "Agregar todo": pone en el pedido las cantidades sugeridas (sin bajar las que ya puso el vendedor). */
    fun onAgregarSugerido() {
        val sug = _uiState.value.suggested ?: return
        val disponibles = _uiState.value.rawProducts.map { it.id }.toSet()
        val cart = _uiState.value.cart.toMutableMap()
        sug.items.filter { it.productId in disponibles }.forEach { cart[it.productId] = maxOf(cart[it.productId] ?: 0, it.qty) }
        _uiState.update { it.copy(cart = cart, selectedTab = ProductTab.ORDER) }
    }

    fun onProcessOrder() {
        val s = _uiState.value
        val stop = s.stop
        if (s.cart.isEmpty() || stop == null) {
            _uiState.update { it.copy(orderStatus = OrderStatus.ERROR, errorMessage = "No hay una visita en curso.") }
            return
        }
        viewModelScope.launch {
            val total = s.cartTotal
            submitOrderUseCase(stop.id, stop.partnerName, s.cart.map { (id, q) -> OrderItem(id, q.toDouble()) }, total)
            routeRepository.sumarVentaLocal(stop.id, total)
            _uiState.update { it.copy(orderStatus = OrderStatus.SUCCESS, cart = emptyMap()) }
        }
    }

    fun resetOrderStatus() = _uiState.update { it.copy(orderStatus = OrderStatus.IDLE, errorMessage = null) }

    private fun applyFilters() {
        val s = _uiState.value
        val lista = s.rawProducts.map { s.modelo(it) }.filter { m ->
            val texto = s.searchQuery.isBlank() || m.product.name.contains(s.searchQuery, true) ||
                m.product.code.contains(s.searchQuery, true)
            val filtro = when (s.filtro) {
                FILTRO_TODOS -> true
                FILTRO_SUGERIDOS -> m.suggestion != null
                FILTRO_METAS -> m.targetInfo != null
                else -> m.product.category == s.filtro
            }
            texto && filtro
        }.sortedWith(compareBy({ it.suggestion == null }, { it.targetInfo == null }, { it.product.name }))
        _uiState.update { it.copy(displayedProducts = lista) }
    }
}
