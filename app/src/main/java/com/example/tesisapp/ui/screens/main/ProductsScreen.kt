package com.example.tesisapp.ui.screens.main

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tesisapp.ui.components.OrderItemCard
import com.example.tesisapp.ui.components.ProductCard

@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel = hiltViewModel(),
    activeRouteLineId: Int?
) {
    // Pasar ID al ViewModel
    LaunchedEffect(activeRouteLineId) {
        if (activeRouteLineId != null) {
            viewModel.setRouteLineId(activeRouteLineId)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Manejo de Toasts
    LaunchedEffect(uiState.orderStatus) {
        if (uiState.orderStatus == OrderStatus.SUCCESS) {
            Toast.makeText(context, "Pedido enviado correctamente", Toast.LENGTH_LONG).show()
            viewModel.resetOrderStatus()
            viewModel.onTabSelected(ProductTab.CATALOG)
        } else if (uiState.orderStatus == OrderStatus.ERROR) {
            Toast.makeText(context, uiState.errorMessage ?: "Error", Toast.LENGTH_LONG).show()
            viewModel.resetOrderStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // Fondo general gris muy suave
            .padding(16.dp)
    ) {

        // --- NUEVO DISEÑO DE PESTAÑAS (SEGMENTED CONTROL) ---
        CustomSegmentedTab(
            selectedTab = uiState.selectedTab,
            onTabSelected = viewModel::onTabSelected,
            cartCount = uiState.cartItemCount
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contenido
        if (uiState.isLoading && uiState.rawProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Black)
            }
        } else {
            when (uiState.selectedTab) {
                ProductTab.CATALOG -> CatalogContent(uiState, viewModel)
                ProductTab.ORDER -> OrderContent(uiState, viewModel)
            }
        }

        // Loader Full Screen
        if (uiState.orderStatus == OrderStatus.PROCESSING) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = Color.White,
                title = { Text("Procesando", color = Color.Black) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Enviando a Odoo...", color = Color.Black)
                    }
                },
                confirmButton = {}
            )
        }
    }
}

// --- COMPOSABLE DE PESTAÑAS PERSONALIZADO ---
@Composable
fun CustomSegmentedTab(
    selectedTab: ProductTab,
    onTabSelected: (ProductTab) -> Unit,
    cartCount: Int
) {
    // Contenedor Gris (Track)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(50)) // Forma de píldora completa
            .background(Color(0xFFEEF0F2)) // Gris claro similar a la imagen
            .padding(4.dp), // Espacio interno para que el seleccionado "flote"
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 1: Catálogo
        TabItem(
            text = "Catálogo",
            icon = Icons.Outlined.Inventory2,
            isSelected = selectedTab == ProductTab.CATALOG,
            onClick = { onTabSelected(ProductTab.CATALOG) },
            modifier = Modifier.weight(1f)
        )

        // Tab 2: Pedido
        TabItem(
            text = "Pedido",
            icon = Icons.Outlined.ShoppingCart,
            isSelected = selectedTab == ProductTab.ORDER,
            onClick = { onTabSelected(ProductTab.ORDER) },
            modifier = Modifier.weight(1f),
            badgeCount = cartCount
        )
    }
}

@Composable
fun TabItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Transparent,
        animationSpec = tween(durationMillis = 200), label = "color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color.Gray,
        animationSpec = tween(durationMillis = 200), label = "text"
    )

    val shadowElevation = if (isSelected) 2.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .shadow(elevation = shadowElevation, shape = RoundedCornerShape(50), clip = false)
            .clickable(indication = null, interactionSource = null) { onClick() }, // Sin efecto ripple
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Icono
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Texto
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )

            // Badge (Contador)
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color.Black, shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogContent(uiState: ProductsUiState, viewModel: ProductsViewModel) {
    Column {
        // Buscador
        TextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text("Buscar productos...", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            singleLine = true
        )

        // Filtros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterButton(
                text = "Todos",
                isSelected = !uiState.showOnlyTargets,
                onClick = { viewModel.onToggleTargetsFilter(false) }
            )

            FilterButton(
                text = "Solo Metas",
                isSelected = uiState.showOnlyTargets,
                onClick = { viewModel.onToggleTargetsFilter(true) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.displayedProducts, key = { it.product.id }) { item ->
                ProductCard(
                    productModel = item,
                    quantityInCart = uiState.cart[item.product.id] ?: 0,
                    onQuantityChanged = { change -> viewModel.onQuantityChanged(item.product.id, change) }
                )
            }
        }
    }
}

@Composable
fun FilterButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.Black else Color.White,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
        elevation = ButtonDefaults.buttonElevation(0.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OrderContent(uiState: ProductsUiState, viewModel: ProductsViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Resumen del Pedido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total de artículos:", color = Color.Gray)
                    Text("${uiState.cartItemCount}", fontWeight = FontWeight.Bold)
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total a Pagar:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$ ${String.format("%.2f", uiState.cartTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }

        if (uiState.cartList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tu carrito está vacío", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.cartList, key = { it.first.product.id }) { (uiModel, quantity) ->
                    OrderItemCard(
                        productModel = uiModel,
                        quantity = quantity,
                        onQuantityChanged = { change -> viewModel.onQuantityChanged(uiModel.product.id, change) }
                    )
                }
            }
        }

        Button(
            onClick = viewModel::onProcessOrder,
            enabled = uiState.cart.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                disabledContainerColor = Color.Gray
            )
        ) {
            Text("Confirmar y Enviar", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
        }
    }
}