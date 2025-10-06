package com.example.tesisapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tesisapp.domain.model.Location

@Composable
fun LocationValidationCard(
    onButtonClick: () -> Unit,
    activeVisitLocation: Location?,
    hasPendingLocations: Boolean
) {
    val isVisitActive = activeVisitLocation != null

    // El botón está habilitado si hay una visita activa O si hay ubicaciones pendientes.
    val isButtonEnabled = isVisitActive || hasPendingLocations

    // Configuraciones dinámicas del botón
    val buttonText = if (isVisitActive) "Terminar Visita" else "Validar Ubicación"
    val buttonIcon = if (isVisitActive) Icons.Default.CheckCircleOutline else Icons.Default.LocationOn
    val buttonColors = if (isVisitActive) {
        ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)) // Verde para "Terminar"
    } else {
        ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)) // Oscuro para "Validar"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isVisitActive) "Visita en curso en:" else "Validar Ubicación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Mostramos el nombre de la tienda si la visita está activa
            if (isVisitActive) {
                Text(
                    text = activeVisitLocation?.name ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onButtonClick,
                enabled = isButtonEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = buttonColors
            ) {
                Icon(
                    imageVector = buttonIcon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(buttonText)
            }
        }
    }
}