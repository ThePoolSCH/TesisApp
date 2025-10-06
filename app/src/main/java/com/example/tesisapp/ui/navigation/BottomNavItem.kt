package com.example.tesisapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector

// sealed class para definir los items de la barra de navegación
sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String) {
    object Routes : BottomNavItem("routes", Icons.Default.Map, "Rutas")
    object Tasks : BottomNavItem("tasks", Icons.Default.CheckBox, "Tareas")
    object Products : BottomNavItem("products", Icons.Default.Inventory2, "Productos")
    object Summary : BottomNavItem("summary", Icons.Default.BarChart, "Resumen")
}