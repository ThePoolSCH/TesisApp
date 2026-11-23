package com.example.tesisapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tesisapp.ui.theme.TesisColors
import com.example.tesisapp.ui.theme.TesisShapes

/** Tipos de chip de estado (mismos que `.chip.*` de los mockups). */
enum class ChipKind { PENDIENTE, OK, ERROR, AVISO, INFO, NEGRO, LINEA }

private fun chipColores(kind: ChipKind): Triple<Color, Color, Color> = when (kind) {
    ChipKind.PENDIENTE -> Triple(TesisColors.Pendiente, TesisColors.PendienteBg, TesisColors.PendienteBorde)
    ChipKind.OK -> Triple(TesisColors.Ok, TesisColors.OkBg, TesisColors.OkBorde)
    ChipKind.ERROR -> Triple(TesisColors.Error, TesisColors.ErrorBg, TesisColors.ErrorBorde)
    ChipKind.AVISO -> Triple(TesisColors.Aviso, TesisColors.AvisoBg, TesisColors.AvisoBorde)
    ChipKind.INFO -> Triple(TesisColors.Info, TesisColors.InfoBg, TesisColors.InfoBorde)
    ChipKind.NEGRO -> Triple(TesisColors.Blanco, TesisColors.Negro, TesisColors.Negro)
    ChipKind.LINEA -> Triple(TesisColors.Negro, TesisColors.Blanco, TesisColors.Gris300)
}

@Composable
fun StatusChip(text: String, kind: ChipKind, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val (fg, bg, borde) = chipColores(kind)
    Row(
        modifier = modifier
            .background(bg, TesisShapes.Pill)
            .border(1.dp, borde, TesisShapes.Pill)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null,
                  enabled: Boolean = true, loading: Boolean = false) {
    Button(
        onClick = onClick, enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(50.dp), shape = TesisShapes.M,
        colors = ButtonDefaults.buttonColors(containerColor = TesisColors.Negro, contentColor = TesisColors.Blanco,
            disabledContainerColor = TesisColors.Gris300, disabledContentColor = TesisColors.Texto2),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = TesisColors.Blanco, strokeWidth = 2.dp)
        } else {
            if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null,
                    enabled: Boolean = true, height: Int = 50) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.height(height.dp), shape = TesisShapes.M,
        border = BorderStroke(1.5.dp, if (enabled) TesisColors.Negro else TesisColors.Gris300),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = TesisColors.Blanco, contentColor = TesisColors.Negro),
        contentPadding = PaddingValues(horizontal = 10.dp)
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        // softWrap = false: con maxLines = 1 el corte por palabra ocultaba el final ("Tareas 3/3" → "Tareas")
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** Tarjeta blanca estándar (radio 16, sombra leve). */
@Composable
fun TesisCard(modifier: Modifier = Modifier, padding: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(), shape = TesisShapes.L,
        colors = CardDefaults.cardColors(containerColor = TesisColors.Superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) { Column(Modifier.padding(padding.dp), content = content) }
}

/** Opción seleccionable de las hojas inferiores (resultado de la visita, fuera de rango...). */
@Composable
fun OptionRow(icon: ImageVector, title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit,
              trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (selected) 2.dp else 1.dp, if (selected) TesisColors.Negro else TesisColors.Gris300, TesisShapes.M)
            .background(if (selected) TesisColors.Gris50 else TesisColors.Blanco, TesisShapes.M)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TesisColors.Negro)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = TesisColors.Negro)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
        }
        trailing?.invoke()
    }
}

@Composable
fun campoColores() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = TesisColors.Blanco, unfocusedContainerColor = TesisColors.Gris50,
    focusedBorderColor = TesisColors.Negro, unfocusedBorderColor = TesisColors.Gris300,
    cursorColor = TesisColors.Negro, focusedLabelColor = TesisColors.Negro, unfocusedLabelColor = TesisColors.Texto2,
    focusedTextColor = TesisColors.Negro, unfocusedTextColor = TesisColors.Negro
)

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = TesisColors.Gris400, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TesisColors.Negro)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
    }
}
