package com.example.tesisapp.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.tesisapp.ui.navigation.BottomNavItem

@Composable
fun BottomNavigationBar(navController: NavController, isProductsTabEnabled: Boolean) {
    val items = listOf(
        BottomNavItem.Routes,
        BottomNavItem.Tasks,
        BottomNavItem.Products,
        BottomNavItem.Summary,
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->

            val isEnabled = if (item is BottomNavItem.Products) {
                isProductsTabEnabled
            } else {
                true // Todos los demás items siempre están habilitados
            }

            NavigationBarItem(
                enabled = isEnabled,
                selected = currentRoute == item.route,
                label = { Text(text = item.title) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                onClick = {
                    navController.navigate(item.route) {
                        // Vuelve al inicio del grafo de navegación para evitar acumular pantallas
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        // Evita lanzar múltiples copias de la misma pantalla
                        launchSingleTop = true
                        // Restaura el estado al volver a seleccionar un item
                        restoreState = true
                    }
                }
            )
        }
    }
}