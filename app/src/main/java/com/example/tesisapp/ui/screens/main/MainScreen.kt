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
    val isVisitActive = routesUiState.activeVisitLocation != null

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
                isProductsTabEnabled = isVisitActive // <-- NUEVO
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Routes.route
            ) {
                composable(BottomNavItem.Routes.route) { RoutesScreen() }
                composable(BottomNavItem.Tasks.route) { TasksScreen() }
                composable(BottomNavItem.Products.route) { ProductsScreen() }
                composable(BottomNavItem.Summary.route) { SummaryScreen() }
            }
        }
    }
}