package com.example.tesisapp.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tesisapp.ui.components.OrderItemCard
import com.example.tesisapp.ui.components.ProductCard

@Composable
fun ProductsScreen(viewModel: ProductsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("Catálogo", "Pedido")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab.ordinal == index,
                    onClick = { viewModel.onTabSelected(if (index == 0) ProductTab.CATALOG else ProductTab.ORDER) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title)
                            if (title == "Catálogo" && uiState.cartItemCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge { Text("${uiState.cartItemCount}") }
                            }
                        }
                    }
                )
            }
        }

        when (uiState.selectedTab) {
            ProductTab.CATALOG -> CatalogContent(uiState, viewModel)
            ProductTab.ORDER -> OrderContent(uiState, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogContent(uiState: ProductsUiState, viewModel: ProductsViewModel) {
    // Usamos las categorías de tus datos hardcodeados
    val categories = listOf("Todos", "Electrónica", "Periféricos", "Monitores")

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            label = { Text("Buscar productos...") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = { viewModel.onCategorySelected(category) },
                    label = { Text(category) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.displayedProducts, key = { it.code }) { product ->
                    ProductCard(
                        product = product,
                        quantityInCart = uiState.cart[product.code] ?: 0,
                        onQuantityChanged = { change -> viewModel.onQuantityChanged(product.code, change) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderContent(uiState: ProductsUiState, viewModel: ProductsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Resumen del Pedido", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total de artículos:")
                    Text("${uiState.cartItemCount}")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total:", style = MaterialTheme.typography.bodyLarge)
                    Text("$${String.format("%.2f", uiState.cartTotal)}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (uiState.cartProducts.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("El carrito está vacío.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.cartProducts, key = { it.first.code }) { (product, quantity) ->
                    OrderItemCard( // Asumiendo que tienes este componente
                        product = product,
                        quantity = quantity,
                        onQuantityChanged = { change -> viewModel.onQuantityChanged(product.code, change) }
                    )
                }
            }
        }

        Button(
            onClick = viewModel::onProcessOrder,
            enabled = uiState.cart.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Procesar Pedido")
        }
    }
}