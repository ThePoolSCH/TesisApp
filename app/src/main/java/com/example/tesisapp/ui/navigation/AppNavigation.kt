package com.example.tesisapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tesisapp.ui.screens.login.LoginScreen
import com.example.tesisapp.ui.screens.main.MainScreen
import com.example.tesisapp.ui.screens.main.MainViewModel
import com.example.tesisapp.ui.screens.profile.ProfileScreen
import com.example.tesisapp.ui.theme.TesisColors

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val PROFILE = "profile"
}

@Composable
fun AppNavigation(sesion: MainViewModel = hiltViewModel()) {
    val estado by sesion.uiState.collectAsStateWithLifecycle()
    if (estado.isLoading) {
        Box(Modifier.fillMaxSize().background(TesisColors.Fondo))
        return
    }
    // F4: con usuario guardado (y su cookie de sesión) la app abre directo en la ruta, también sin conexión
    val inicio = remember { if (estado.user != null) Routes.MAIN else Routes.LOGIN }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = inicio) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.MAIN) {
            MainScreen(
                onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.MAIN) { inclusive = true } } },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = { navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } }
            )
        }
    }
}
