package com.example.tesisapp.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tesisapp.ui.components.BottomNavigationBar
import com.example.tesisapp.ui.components.CustomTopAppBar
import com.example.tesisapp.ui.navigation.BottomNavItem

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit, // Nuevo callback para navegar al perfil
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val bottomNavController = rememberNavController()
    val routesViewModel: RoutesViewModel = hiltViewModel()
    val routesUiState by routesViewModel.uiState.collectAsStateWithLifecycle()

    val activeVisitId = routesUiState.activeVisitLocation?.id
    val isVisitActive = activeVisitId != null

    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CustomTopAppBar(
                user = mainUiState.user,
                onViewProfileClick = onNavigateToProfile,
                onLogoutClick = onLogout
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = bottomNavController,
                isProductsTabEnabled = isVisitActive // Esto ya lo tenías, bloquea el click visualmente
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Routes.route
            ) {
                composable(BottomNavItem.Routes.route) {
                    // Como RoutesViewModel es HiltViewModel y está en el scope de MainScreen
                    // al llamar a RoutesScreen() usará la misma instancia o una nueva
                    // pero para compartir datos es mejor que RoutesScreen use su propio inject
                    // (como ya lo tienes hecho).
                    RoutesScreen()
                }

                composable(BottomNavItem.Tasks.route) { TasksScreen() }

                // 3. PASAMOS EL ID AQUI
                composable(BottomNavItem.Products.route) {
                    ProductsScreen(
                        activeRouteLineId = activeVisitId
                    )
                }

                composable(BottomNavItem.Summary.route) { SummaryScreen() }
            }
        }
    }
}