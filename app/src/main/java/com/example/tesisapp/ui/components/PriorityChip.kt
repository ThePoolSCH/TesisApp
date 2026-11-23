package com.example.tesisapp.ui.components

import com.example.tesisapp.ui.theme.TesisColors

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun PriorityChip(priority: String) {
    val (backgroundColor, textColor, icon) = when (priority) {
        "Alta" -> Triple(TesisColors.ErrorBg, TesisColors.Error, Icons.Default.ErrorOutline)
        "Media" -> Triple(TesisColors.AvisoBg, TesisColors.AvisoFuerte, Icons.Default.WatchLater)
        "Baja" -> Triple(TesisColors.OkBg, TesisColors.Ok, Icons.Default.CheckCircleOutline)
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.ErrorOutline)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = "Prioridad $priority", tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = priority, color = textColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}