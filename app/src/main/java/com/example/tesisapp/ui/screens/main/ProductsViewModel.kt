package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.Product
import com.example.tesisapp.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProductTab { CATALOG, ORDER }

data class ProductsUiState(
    val isLoading: Boolean = true,
    val allProducts: List<Product> = emptyList(),
    val displayedProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Todos",
    val selectedTab: ProductTab = ProductTab.CATALOG,
    val cart: Map<String, Int> = emptyMap() // Map<ProductCode, Quantity>
) {
    val cartItemCount: Int get() = cart.values.sum()
    val cartTotal: Double get() = cart.entries.sumOf { (productCode, quantity) ->
        (allProducts.find { it.code == productCode }?.price ?: 0.0) * quantity
    }
    val cartProducts: List<Pair<Product, Int>> get() = cart.mapNotNull { (productCode, quantity) ->
        allProducts.find { it.code == productCode }?.let { product ->
            product to quantity
        }
    }
}

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Ya no es necesario llamar a seedProductsIfEmpty().
            // El repositorio se encarga de esto automáticamente en su bloque init.
            productRepository.getProducts().collect { products ->
                _uiState.update { it.copy(isLoading = false, allProducts = products) }
                filterProducts()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterProducts()
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        filterProducts()
    }

    fun onTabSelected(tab: ProductTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onQuantityChanged(productCode: String, change: Int) {
        val currentCart = _uiState.value.cart.toMutableMap()
        val currentQuantity = currentCart[productCode] ?: 0
        val newQuantity = currentQuantity + change

        if (newQuantity > 0) {
            currentCart[productCode] = newQuantity
        } else {
            currentCart.remove(productCode)
        }
        _uiState.update { it.copy(cart = currentCart) }
    }

    fun onProcessOrder() {
        if (_uiState.value.cart.isNotEmpty()) {
            println("Procesando pedido: ${_uiState.value.cart}")
            _uiState.update { it.copy(cart = emptyMap()) }
        }
    }

    private fun filterProducts() {
        val currentState = _uiState.value
        val filteredList = currentState.allProducts.filter { product ->
            val matchesCategory = currentState.selectedCategory == "Todos" || product.category == currentState.selectedCategory
            val matchesSearch = currentState.searchQuery.isBlank() ||
                    product.name.contains(currentState.searchQuery, ignoreCase = true) ||
                    product.code.contains(currentState.searchQuery, ignoreCase = true) ||
                    product.sku.contains(currentState.searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
        _uiState.update { it.copy(displayedProducts = filteredList) }
    }
}