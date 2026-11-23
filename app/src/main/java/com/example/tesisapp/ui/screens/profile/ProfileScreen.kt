package com.example.tesisapp.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.BuildConfig
import com.example.tesisapp.data.repository.ClientesRepository
import com.example.tesisapp.data.repository.Perfil
import com.example.tesisapp.data.sync.OutboxRepository
import com.example.tesisapp.data.sync.SyncStatus
import com.example.tesisapp.domain.repository.UserRepository
import com.example.tesisapp.ui.components.*
import com.example.tesisapp.ui.theme.TesisColors
import com.example.tesisapp.utils.Formato
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PerfilUiState(
    val perfil: Perfil? = null,
    val actualizado: Long = 0L,
    val cargando: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ClientesRepository,
    private val userRepository: UserRepository,
    outbox: OutboxRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(PerfilUiState())
    val ui = _ui.asStateFlow()
    val sync: StateFlow<SyncStatus?> = outbox.estado.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch { repo.perfil.collect { (p, t) -> _ui.update { it.copy(perfil = p, actualizado = t) } } }
        actualizar()
    }

    fun actualizar() = viewModelScope.launch {
        _ui.update { it.copy(cargando = true, error = null) }
        val error = repo.actualizarPerfil()
        _ui.update { it.copy(cargando = false, error = error) }
    }

    fun cerrarSesion(onDone: () -> Unit) = viewModelScope.launch { userRepository.logout(); onDone() }
}

/** Perfil del vendedor (D4): territorio y supervisor, resumen del mes, estado de la app y cierre de sesión. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val s by vm.ui.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmarSalida by remember { mutableStateOf(false) }

    if (confirmarSalida) {
        val pendientes = (sync?.pendientes ?: 0) + (sync?.errores ?: 0)
        AlertDialog(
            onDismissRequest = { confirmarSalida = false }, containerColor = TesisColors.Blanco,
            title = { Text("¿Cerrar sesión?") },
            text = {
                Text(if (pendientes > 0) "Tienes $pendientes operación(es) sin enviar. Se quedan en el teléfono y se enviarán " +
                    "cuando vuelvas a iniciar sesión con este mismo usuario." else "Tendrás que volver a ingresar tu usuario y contraseña.")
            },
            confirmButton = { TextButton({ confirmarSalida = false; vm.cerrarSesion(onLogout) }) { Text("Cerrar sesión", color = TesisColors.Error) } },
            dismissButton = { TextButton({ confirmarSalida = false }) { Text("Cancelar", color = TesisColors.Negro) } }
        )
    }

    Scaffold(
        containerColor = TesisColors.Fondo,
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    IconButton(onClick = { vm.actualizar() }, enabled = !s.cargando) {
                        if (s.cargando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = TesisColors.Negro)
                        else Icon(Icons.Default.Refresh, "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TesisColors.Fondo)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val p = s.perfil
            if (p == null) {
                TesisCard {
                    if (s.cargando) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TesisColors.Negro)
                    } else EmptyState(Icons.Default.CloudOff, "No se pudo cargar tu perfil", s.error)
                }
            } else {
                // Encabezado
                TesisCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).background(TesisColors.Negro, CircleShape), contentAlignment = Alignment.Center) {
                            Text(p.name.take(1).uppercase(), color = TesisColors.Blanco, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(p.login, color = TesisColors.Texto2, style = MaterialTheme.typography.bodySmall)
                            Text(p.company, color = TesisColors.Texto2, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (s.error != null && s.actualizado > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text("Sin conexión · datos del ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(s.actualizado))}",
                            style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
                    }
                }

                // Mi mes
                TesisCard {
                    Text("Mi mes", fontWeight = FontWeight.Bold)
                    Text("Desde el ${Formato.fechaCorta(p.month.start)}", style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Dato("Visitas", "${p.month.visitsDone} de ${p.month.visitsPlanned}", Modifier.weight(1f))
                        val efect = if (p.month.visitsDone > 0) p.month.visitsWithOrder * 100.0 / p.month.visitsDone else 0.0
                        Dato("Con pedido", Formato.pct(efect), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Dato("Pedidos", Formato.numero(p.month.orders.toDouble()), Modifier.weight(1f))
                        Dato("Ventas (sin IGV)", Formato.pen(p.month.sales, 0), Modifier.weight(1f))
                    }
                }

                // Territorio
                TesisCard {
                    Text("Mi territorio", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Fila(Icons.Default.Map, "Zona", p.district.ifBlank { "Sin zona asignada" })
                    Fila(Icons.Default.Public, "Región", p.region.ifBlank { "—" })
                    Fila(Icons.Default.People, "Clientes en cartera", Formato.numero(p.clients.toDouble()))
                    Fila(Icons.Default.Route, "Visitas por día (capacidad)", Formato.numero(p.dailyCapacity.toDouble()))
                    if (p.supervisor.isNotBlank()) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = TesisColors.Gris200)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Supervisor", style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
                                Text(p.supervisor, fontWeight = FontWeight.SemiBold)
                            }
                            if (p.supervisorPhone.isNotBlank()) {
                                SecondaryButton("Llamar", {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${p.supervisorPhone.replace(" ", "")}")))
                                }, icon = Icons.Default.Call, height = 40)
                            }
                        }
                    }
                }
            }

            // App
            TesisCard {
                Text("Aplicación", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Fila(Icons.Default.Info, "Versión", BuildConfig.VERSION_NAME)
                Fila(Icons.Default.Dns, "Servidor", "${BuildConfig.API_BASE_URL.removeSuffix("/")} · base ${BuildConfig.ODOO_DB_NAME}")
                sync?.let { st ->
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sincronización", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
                        SyncChip(st) {}
                    }
                }
            }

            SecondaryButton("Cerrar sesión", { confirmarSalida = true }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Filled.Logout)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Dato(titulo: String, valor: String, modifier: Modifier) {
    Column(modifier.background(TesisColors.Gris100, MaterialTheme.shapes.small).padding(10.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun Fila(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, Modifier.size(18.dp), tint = TesisColors.Texto2)
        Spacer(Modifier.width(10.dp))
        Text(titulo, Modifier.weight(1f), color = TesisColors.Texto2, style = MaterialTheme.typography.bodyMedium)
        Text(valor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}
