package com.example.tesisapp.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tesisapp.ui.components.BottomNavigationBar
import com.example.tesisapp.ui.components.CustomTopAppBar
import com.example.tesisapp.ui.navigation.BottomNavItem
import com.example.tesisapp.ui.screens.clientes.CarteraScreen
import com.example.tesisapp.ui.screens.clientes.FichaClienteScreen
import com.example.tesisapp.ui.screens.sync.SyncScreen
import com.example.tesisapp.ui.theme.TesisColors

private const val SYNC = "sync"
private const val CARTERA = "cartera"
private const val CLIENTE = "cliente/{id}"

private fun NavHostController.irPestana(ruta: String) = navigate(ruta) {
    popUpTo(BottomNavItem.Routes.route) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val nav = rememberNavController()
    val routesViewModel: RoutesViewModel = hiltViewModel()
    val routesUiState by routesViewModel.uiState.collectAsStateWithLifecycle()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val sync by mainViewModel.sync.collectAsStateWithLifecycle()
    val activeStop = routesUiState.activeStop
    val rutaActual = nav.currentBackStackEntryAsState().value?.destination?.route
    val esPestana = rutaActual in listOf(BottomNavItem.Routes.route, BottomNavItem.Tasks.route,
        BottomNavItem.Products.route, BottomNavItem.Summary.route, null)

    LaunchedEffect(Unit) { routesViewModel.ensureInitialSync() }

    Scaffold(
        containerColor = TesisColors.Fondo,
        topBar = {
            if (esPestana) CustomTopAppBar(
                user = mainUiState.user, sync = sync, onSyncClick = { nav.navigate(SYNC) },
                onViewProfileClick = onNavigateToProfile,
                onLogoutClick = { mainViewModel.logout(onLogout) }
            )
        },
        bottomBar = {
            // Tareas y Productos solo durante una visita
            if (esPestana) BottomNavigationBar(navController = nav, isProductsTabEnabled = activeStop != null)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController = nav, startDestination = BottomNavItem.Routes.route) {
                composable(BottomNavItem.Routes.route) {
                    RoutesScreen(
                        viewModel = routesViewModel,
                        onAbrirPedido = { nav.irPestana(BottomNavItem.Products.route) },
                        onAbrirTareas = { nav.irPestana(BottomNavItem.Tasks.route) },
                        onAbrirCliente = { id -> nav.navigate("cliente/$id") },
                        onAbrirCartera = { nav.navigate(CARTERA) }
                    )
                }
                composable(BottomNavItem.Tasks.route) { TasksScreen(activeStop = activeStop) }
                composable(BottomNavItem.Products.route) { ProductsScreen(activeStop = activeStop) }
                composable(BottomNavItem.Summary.route) { SummaryScreen() }
                composable(SYNC) { SyncScreen(onBack = { nav.popBackStack() }) }
                composable(CARTERA) {
                    CarteraScreen(onBack = { nav.popBackStack() }, onAbrirCliente = { id -> nav.navigate("cliente/$id") })
                }
                composable(CLIENTE, arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                    FichaClienteScreen(
                        partnerId = entry.arguments?.getInt("id") ?: 0,
                        onBack = { nav.popBackStack() },
                        onVisitaAgregada = { nav.popBackStack(BottomNavItem.Routes.route, inclusive = false) }
                    )
                }
            }
        }
    }
}
