package com.example.tesisapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TaskProgressCard(
    completed: Int,
    total: Int,
    progress: Float,
    locationName: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // COLOR GRIS CLARO DE FONDO
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(0.dp) // Sin sombra para estilo "flat" minimalista
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Progreso de Visita",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = locationName ?: "Sin ubicación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Contador grande
                Text(
                    text = "$completed/$total",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de progreso NEGRA
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color.Black,
                trackColor = Color.White, // El rastro en blanco para contraste
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (progress == 1f) "¡Todo listo!" else "Completa las tareas pendientes",
                style = MaterialTheme.typography.bodySmall,
                color = if (progress == 1f) Color.Black else Color.Gray
            )
        }
    }
}