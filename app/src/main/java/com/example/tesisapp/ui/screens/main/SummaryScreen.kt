package com.example.tesisapp.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tesisapp.data.repository.CampanaComision
import com.example.tesisapp.data.repository.Liquidacion
import com.example.tesisapp.ui.components.*
import com.example.tesisapp.ui.theme.TesisColors
import com.example.tesisapp.ui.theme.TesisShapes
import com.example.tesisapp.utils.Formato
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SummaryScreen(viewModel: SummaryViewModel = hiltViewModel()) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().background(TesisColors.Fondo),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mis Objetivos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    val t = s.actualizado
                    Text(
                        when {
                            s.error != null && t > 0 -> "Sin conexión · datos del ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(t))}"
                            t > 0 -> "Actualizado ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(t))}"
                            else -> ""
                        }, style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2
                    )
                }
                IconButton(onClick = viewModel::refreshData) {
                    if (s.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = TesisColors.Negro)
                    else Icon(Icons.Default.Refresh, "Actualizar")
                }
            }
        }
        val datos = s.datos
        if (datos == null || datos.campaigns.isEmpty()) {
            item {
                TesisCard {
                    EmptyState(Icons.Outlined.EmojiEvents, "No tienes campañas activas",
                        s.error ?: "Cuando tu supervisor te asigne una meta, verás aquí tu avance y tu comisión estimada.")
                }
            }
        } else {
            items(datos.campaigns, key = { it.campaignId }) { CampanaCard(it) }
        }
        if (!datos?.settlements.isNullOrEmpty()) {
            item { Text("Mis liquidaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(datos!!.settlements) { LiquidacionCard(it) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CampanaCard(c: CampanaComision) {
    TesisCard {
        Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Quedan ${c.daysLeft} días · cierra ${Formato.fechaCorta(c.endDate)}", style = MaterialTheme.typography.bodySmall,
            color = TesisColors.Texto2)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Formato.pct(c.achievementPercent), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(8.dp))
            Text("${Formato.numero(c.achieved)} de ${Formato.numero(c.target)}", color = TesisColors.Texto2,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        LinearProgressIndicator(
            progress = { (c.achievementPercent / 100).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp), color = TesisColors.Negro, trackColor = TesisColors.Gris200,
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(8.dp))
        val estado = when {
            c.projectionPercent >= 100 -> ChipKind.OK
            c.projectionPercent >= 85 -> ChipKind.AVISO
            else -> ChipKind.ERROR
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Proyección ${Formato.pct(c.projectionPercent)}", estado)
            val falta = (c.target - c.achieved).coerceAtLeast(0.0)
            if (falta > 0 && c.daysLeft > 0) {
                Text("${Formato.numero(falta / c.daysLeft, 1)} por día para el 100 %", style = MaterialTheme.typography.bodySmall,
                    color = TesisColors.Texto2)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = TesisColors.Gris200)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Comisión estimada", color = TesisColors.Texto2, style = MaterialTheme.typography.bodySmall)
                Text(Formato.pen(c.estimatedAmount, 0), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text("${Formato.pct(c.estimatedPayoutPct)} de un bono de ${Formato.pen(c.baseBonus, 0)}",
                style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
        }
        val regla = c.rule
        if (regla != null && regla.tiers.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Cómo se calcula (${regla.name})", style = MaterialTheme.typography.labelMedium, color = TesisColors.Texto2)
            Spacer(Modifier.height(6.dp))
            val tramos = regla.tiers.sortedBy { it.min }
            val actual = tramos.lastOrNull { c.projectionPercent >= it.min }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tramos.forEach { t ->
                    val sel = t == actual
                    Column(
                        Modifier.weight(1f)
                            .background(if (sel) TesisColors.Negro else TesisColors.Gris100, TesisShapes.S)
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("≥ ${Formato.numero(t.min)} %", style = MaterialTheme.typography.labelSmall,
                            color = if (sel) TesisColors.Blanco else TesisColors.Texto2)
                        Text("paga ${Formato.numero(t.pay)} %", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium,
                            color = if (sel) TesisColors.Blanco else TesisColors.Negro)
                    }
                }
            }
            tramos.firstOrNull { it.min > c.projectionPercent }?.let { sig ->
                val faltaUnid = c.target * sig.min / 100 - c.achieved
                Text("Siguiente tramo (${Formato.numero(sig.min)} %): te faltan ${Formato.numero(faltaUnid.coerceAtLeast(0.0))}",
                    style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun LiquidacionCard(l: Liquidacion) {
    var abierta by remember { mutableStateOf(false) }
    TesisCard(Modifier.clickable { abierta = !abierta }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(l.settlement, fontWeight = FontWeight.Bold)
                Text(Formato.fechaCorta(l.date), style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (l.amount > 0) Formato.pen(l.amount, 0) else "Sin pago", fontWeight = FontWeight.Bold)
                when (l.state) {
                    "paid" -> StatusChip("Pagada", ChipKind.OK)
                    "approved" -> StatusChip("Aprobada", ChipKind.INFO)
                    else -> StatusChip("Calculada", ChipKind.LINEA)
                }
            }
        }
        if (abierta) l.details.forEach {
            Text("• $it", style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
