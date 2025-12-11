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
fun BottomNavigationBar(
    navController: NavController,
    isProductsTabEnabled: Boolean
) {
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

            // Lógica actualizada: Habilita tanto Productos como Tareas
            // solo si hay una visita activa (isProductsTabEnabled es true).
            // Los demás items (Rutas, Resumen) siempre están habilitados.
            val isEnabled = when (item) {
                is BottomNavItem.Products, is BottomNavItem.Tasks -> isProductsTabEnabled
                else -> true
            }

            NavigationBarItem(
                // Aplicamos el flag de habilitación al componente
                enabled = isEnabled,

                // El resto de la lógica de selección y apariencia se mantiene
                selected = currentRoute == item.route,
                label = { Text(text = item.title) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                onClick = {
                    // Importante: Solo ejecutar la navegación si el item está habilitado
                    if (isEnabled) {
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
                }
            )
        }
    }
}