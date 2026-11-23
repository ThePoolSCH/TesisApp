package com.example.tesisapp.ui.screens.main

import androidx.compose.foundation.lazy.LazyRow
import com.example.tesisapp.domain.model.RouteStop
import com.example.tesisapp.domain.model.SuggestedOrder
import com.example.tesisapp.ui.theme.TesisColors
import com.example.tesisapp.ui.theme.TesisShapes
import com.example.tesisapp.utils.Formato

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
    activeStop: RouteStop?,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    // La visita en curso: el pedido queda ligado a ella y se carga su pedido sugerido
    LaunchedEffect(activeStop?.id) { activeStop?.let(viewModel::setStop) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Manejo de Toasts
    LaunchedEffect(uiState.orderStatus) {
        if (uiState.orderStatus == OrderStatus.SUCCESS) {
            Toast.makeText(context, "Pedido guardado. Se enviará a Odoo en cuanto haya conexión.", Toast.LENGTH_LONG).show()
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
            .background(TesisColors.Fondo) // Fondo general gris muy suave
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
            .background(TesisColors.Gris200) // Gris claro similar a la imagen
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

        // Filtros (mockup A4): Todos / Sugeridos / Solo metas / categorías
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.filtros) { f ->
                FilterButton(text = f, isSelected = uiState.filtro == f, onClick = { viewModel.onFiltro(f) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            val sug = uiState.suggested
            if (sug != null && sug.items.isNotEmpty() && uiState.filtro == FILTRO_TODOS && uiState.searchQuery.isBlank()) {
                item { SugeridoCard(sug, uiState.stop?.partnerName.orEmpty(), viewModel::onAgregarSugerido) }
            }
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
        border = if (!isSelected) BorderStroke(1.dp, TesisColors.Gris300) else null,
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
            border = BorderStroke(1.dp, TesisColors.Gris200)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Resumen del Pedido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total de artículos:", color = Color.Gray)
                    Text("${uiState.cartItemCount}", fontWeight = FontWeight.Bold)
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = TesisColors.Gris200)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total a Pagar:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = Formato.pen(uiState.cartTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
                val ticket = uiState.suggested?.avgTicket ?: 0.0
                if (ticket > 0 && uiState.cartTotal > 0) {
                    val dif = (uiState.cartTotal / ticket - 1) * 100
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ticket medio del cliente: ${Formato.pen(ticket)} · este pedido ${if (dif >= 0) "+" else ""}${Formato.pct(dif)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dif >= 0) TesisColors.Ok else TesisColors.Texto2
                    )
                }
                Text("Precios sin IGV", style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
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
            Text("Confirmar pedido", fontWeight = FontWeight.Bold)
        }
    }
}

/** Bloque negro "Pedido sugerido" (mockup A4): qué suele comprar el cliente y por qué; "Agregar todo". */
@Composable
private fun SugeridoCard(sug: SuggestedOrder, cliente: String, onAgregarTodo: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = TesisColors.Negro, contentColor = TesisColors.Blanco)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Pedido sugerido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Lo que $cliente suele comprar", style = MaterialTheme.typography.bodySmall, color = TesisColors.Gris400)
            Spacer(Modifier.height(10.dp))
            sug.items.take(6).forEach { it ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${it.qty} ×", fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                    Column(Modifier.weight(1f)) {
                        Text(it.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(if (it.campaign) "${it.reason} · meta de campaña" else it.reason,
                            style = MaterialTheme.typography.labelSmall, color = TesisColors.Gris400)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAgregarTodo, modifier = Modifier.fillMaxWidth().height(44.dp), shape = TesisShapes.M,
                colors = ButtonDefaults.buttonColors(containerColor = TesisColors.Blanco, contentColor = TesisColors.Negro)
            ) { Text("Agregar todo", fontWeight = FontWeight.Bold) }
        }
    }
}
