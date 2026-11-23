package com.example.tesisapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tesisapp.domain.model.RouteStop
import com.example.tesisapp.ui.theme.TesisColors
import com.example.tesisapp.utils.Formato
import kotlinx.coroutines.delay

/** Parada de la lista "Ubicaciones por Visitar" (mockup A1): hora estimada, dirección real y estado. */
@Composable
fun StopCard(stop: RouteStop, index: Int, isNext: Boolean) {
    val hecha = stop.visitState == "done"
    val enCurso = stop.visitState == "arrived"
    Row(
        Modifier
            .fillMaxWidth()
            .border(if (isNext || enCurso) 2.dp else 1.dp, if (hecha) TesisColors.Gris300 else TesisColors.Negro,
                MaterialTheme.shapes.medium)
            .background(if (hecha) TesisColors.Gris50 else TesisColors.Blanco, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(28.dp).background(if (hecha) TesisColors.OkFuerte else TesisColors.Negro, CircleShape),
            contentAlignment = Alignment.Center) {
            if (hecha) Icon(Icons.Default.Check, null, tint = TesisColors.Blanco, modifier = Modifier.size(18.dp))
            else Text("${index + 1}", color = TesisColors.Blanco, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stop.partnerName, fontWeight = FontWeight.Bold, color = TesisColors.Negro, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detalle = listOf(stop.street.ifBlank { stop.address }, Formato.tipoCliente(stop.clientType))
                .filter { it.isNotBlank() }.joinToString(" · ")
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (stop.unplanned) Text("No planificada", style = MaterialTheme.typography.labelSmall, color = TesisColors.Info)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (stop.plannedArrival.isNotBlank()) Text(stop.plannedArrival, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            when {
                hecha && stop.visitResult == "order" ->
                    StatusChip(if (stop.totalSold > 0) "Pedido ${Formato.pen(stop.totalSold, 0)}" else "Pedido", ChipKind.OK)
                hecha -> StatusChip(Formato.resultado(stop.visitResult).substringBefore(":").ifBlank { "Visitada" }, ChipKind.LINEA)
                enCurso -> StatusChip("En visita", ChipKind.NEGRO)
                isNext -> StatusChip("Siguiente", ChipKind.NEGRO)
                else -> StatusChip("Pendiente", ChipKind.PENDIENTE)
            }
        }
    }
}

/** Progreso del día (n de m visitadas). */
@Composable
fun ProgresoRuta(hechas: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$hechas de $total visitadas", fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(
        progress = { if (total > 0) hechas / total.toFloat() else 0f },
        modifier = Modifier.fillMaxWidth().height(8.dp), color = TesisColors.Negro, trackColor = TesisColors.Gris200,
        strokeCap = StrokeCap.Round
    )
}

/** Cronómetro "EN CURSO · mm:ss" desde la llegada. */
@Composable
fun Cronometro(desde: Long) {
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(desde) { while (true) { ahora = System.currentTimeMillis(); delay(1000) } }
    val seg = if (desde > 0) ((ahora - desde) / 1000).coerceAtLeast(0) else 0
    StatusChip("EN CURSO · %02d:%02d".format(seg / 60, seg % 60), ChipKind.NEGRO, icon = Icons.Default.Timer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FueraDeRangoSheet(nombre: String, distanciaM: Double, radioM: Double, onAcercarme: () -> Unit, onRegistrar: (String) -> Unit) {
    var registrar by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onAcercarme, containerColor = TesisColors.Blanco) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WrongLocation, null, tint = TesisColors.Error)
                Spacer(Modifier.width(8.dp))
                Text("Estás lejos del cliente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text("Tu ubicación está a ${Formato.numero(distanciaM)} m de $nombre. El máximo permitido es ${Formato.numero(radioM)} m.",
                color = TesisColors.Texto2)
            OptionRow(Icons.Outlined.NearMe, "Acercarme e intentar de nuevo", "Recomendado", !registrar, { registrar = false })
            OptionRow(Icons.Outlined.EditLocationAlt, "Registrar de todas formas", "Pide un motivo; el supervisor lo verá marcado",
                registrar, { registrar = true })
            if (registrar) {
                OutlinedTextField(motivo, { motivo = it }, Modifier.fillMaxWidth(), minLines = 2, colors = campoColores(),
                    placeholder = { Text("Motivo (p. ej., el GPS marca mal en esta zona)") }, shape = MaterialTheme.shapes.medium)
            }
            PrimaryButton("Continuar", onClick = { if (registrar) onRegistrar(motivo.trim()) else onAcercarme() },
                enabled = !registrar || motivo.isNotBlank())
            SecondaryButton("Cancelar", onAcercarme, Modifier.fillMaxWidth())
        }
    }
}

private val RESULTADOS = listOf(
    Triple("order", Icons.Outlined.ShoppingCart, "Se registró un pedido en esta visita"),
    Triple("stock", Icons.Outlined.Inventory2, "El cliente no necesita reponer"),
    Triple("closed", Icons.Outlined.Storefront, "Se reprograma automáticamente"),
    Triple("absent", Icons.Outlined.PersonOff, "No había quien decida la compra"),
    Triple("other", Icons.Outlined.MoreHoriz, "Describe en la nota"),
)

/** Hoja "Terminar visita" (mockup A1, estado 4): resultado obligatorio, nota y estado de las tareas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminarVisitaSheet(
    nombre: String, minutos: Long, vendido: Double, tareasHechas: Int, tareasTotal: Int, obligatoriasFaltan: Int,
    onCancelar: () -> Unit, onFinalizar: (String, String) -> Unit
) {
    var sel by remember { mutableStateOf(if (vendido > 0) "order" else "") }
    var nota by remember { mutableStateOf("") }
    // Sin hablar con el cliente (cerrado, ausente) no se le pueden exigir las tareas
    val exigeTareas = sel !in listOf("closed", "absent")
    val bloqueado = sel.isBlank() || (exigeTareas && obligatoriasFaltan > 0) || (sel == "other" && nota.isBlank())
    ModalBottomSheet(onDismissRequest = onCancelar, containerColor = TesisColors.Blanco) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Terminar visita", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("$nombre · $minutos min en el local", color = TesisColors.Texto2)
            Text("Elige el resultado de la visita (obligatorio):", style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
            RESULTADOS.forEach { (codigo, icono, sub) ->
                OptionRow(icono, Formato.resultado(codigo), sub, sel == codigo, { sel = codigo },
                    trailing = if (codigo == "order" && vendido > 0) ({ StatusChip(Formato.pen(vendido, 0), ChipKind.OK) }) else null)
            }
            OutlinedTextField(nota, { nota = it }, Modifier.fillMaxWidth(), minLines = 2, colors = campoColores(),
                placeholder = { Text(if (sel == "other") "Describe el motivo (obligatorio)" else "Nota (opcional)") },
                shape = MaterialTheme.shapes.medium)
            if (tareasTotal > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckBox, null, Modifier.size(16.dp),
                        tint = if (obligatoriasFaltan > 0 && exigeTareas) TesisColors.Error else TesisColors.Texto2)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (obligatoriasFaltan > 0 && exigeTareas) "Tareas: faltan $obligatoriasFaltan obligatorias (pestaña Tareas)"
                        else "Tareas: $tareasHechas de $tareasTotal completas",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (obligatoriasFaltan > 0 && exigeTareas) TesisColors.Error else TesisColors.Texto2
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton("Finalizar", onClick = { onFinalizar(sel, nota.trim()) }, enabled = !bloqueado)
            SecondaryButton("Cancelar", onCancelar, Modifier.fillMaxWidth())
        }
    }
}

/** Hoja con un motivo de texto obligatorio (visita no planificada). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivoSheet(titulo: String, detalle: String, placeholder: String, boton: String, cargando: Boolean,
                onCancelar: () -> Unit, onConfirmar: (String) -> Unit) {
    var motivo by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onCancelar, containerColor = TesisColors.Blanco) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(detalle, color = TesisColors.Texto2, textAlign = TextAlign.Start)
            OutlinedTextField(motivo, { motivo = it }, Modifier.fillMaxWidth(), minLines = 2, colors = campoColores(),
                placeholder = { Text(placeholder) }, shape = MaterialTheme.shapes.medium)
            PrimaryButton(boton, onClick = { onConfirmar(motivo.trim()) }, enabled = motivo.isNotBlank(), loading = cargando)
            SecondaryButton("Cancelar", onCancelar, Modifier.fillMaxWidth())
        }
    }
}
