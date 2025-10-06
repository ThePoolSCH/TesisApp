package com.example.tesisapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tesisapp.ui.screens.login.LoginScreen
import com.example.tesisapp.ui.screens.main.MainScreen // Importar la nueva pantalla principal
import com.example.tesisapp.ui.screens.profile.ProfileScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main" // Nueva ruta para el contenedor principal
    const val PROFILE = "profile" // Nueva ruta de perfil
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    // Acción para navegar a la pantalla de perfil
                    navController.navigate(Routes.PROFILE)
                }
            )
        }

        // Añadimos la nueva pantalla al grafo de navegación principal
        composable(Routes.PROFILE) {
            ProfileScreen()
        }
    }
}