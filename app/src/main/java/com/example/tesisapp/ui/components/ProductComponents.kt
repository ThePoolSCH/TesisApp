package com.example.tesisapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tesisapp.ui.screens.main.ProductUiModel

@Composable
fun ProductCard(
    productModel: ProductUiModel,
    quantityInCart: Int,
    onQuantityChanged: (Int) -> Unit
) {
    val product = productModel.product
    val target = productModel.targetInfo

    // Usamos un Card con borde muy sutil y fondo blanco
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        //elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            // CONTENIDO PRINCIPAL
            Column(modifier = Modifier.padding(16.dp)) {

                // Fila Superior: Imagen + Textos
                Row(verticalAlignment = Alignment.Top) {
                    // Imagen (Mantenemos la imagen porque la tenemos, pero la hacemos sutil)
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Textos (Titulo y Código)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.code, // Usamos el código como subtítulo
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fila Inferior: Precio y Botón
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Precio Grande
                    Text(
                        text = "$${product.price}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // Botón de Acción
                    if (quantityInCart > 0) {
                        // Estado: Ya agregado (Controles +/-)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onQuantityChanged(-1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Remove", tint = Color.Black)
                            }
                            Text(
                                text = "$quantityInCart",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { onQuantityChanged(1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                            }
                        }
                    } else {
                        // Estado: Botón Negro "+ Agregar"
                        Button(
                            onClick = { onQuantityChanged(1) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // TAG DE META (Posicionado Absolutamente a la derecha)
            if (target != null) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer, // Color pastel (lila/rosa)
                    shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 16.dp), // Esquina redondeada opuesta
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "Meta: ${target.currentAmount.toInt()}/${target.targetAmount.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(
    productModel: ProductUiModel,
    quantity: Int,
    onQuantityChanged: (Int) -> Unit
) {
    val product = productModel.product
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp).background(Color.LightGray, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.bodyLarge)
                Text("$ ${product.price} x $quantity", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text("Subtotal: $ ${String.format("%.2f", product.price * quantity)}", style = MaterialTheme.typography.titleSmall)
            }
            IconButton(onClick = { onQuantityChanged(-1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Quitar")
            }
            Text("$quantity", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onQuantityChanged(1) }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
    }
}