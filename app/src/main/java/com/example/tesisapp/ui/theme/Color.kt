package com.example.tesisapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de color de TesisApp (F4). Son los mismos que usan los mockups aprobados de la F2
 * (`Mockups/comun/app.css`), tomados a su vez del estilo que ya tenía la app: negro sobre blanco,
 * gris para lo secundario y colores de estado con su fondo claro.
 */
object TesisColors {
    val Negro = Color(0xFF000000)
    val Blanco = Color(0xFFFFFFFF)
    val Texto = Color(0xFF1C1B1F)
    val Texto2 = Color(0xFF757575)
    val Fondo = Color(0xFFF7F5FA)
    val Superficie = Color(0xFFFFFFFF)
    val Gris50 = Color(0xFFFAFAFA)
    val Gris100 = Color(0xFFF5F5F5)
    val Gris200 = Color(0xFFEEEEEE)
    val Gris300 = Color(0xFFE0E0E0)
    val Gris400 = Color(0xFFBDBDBD)

    // Estados (texto / fondo / borde)
    val Pendiente = Color(0xFFE67E22); val PendienteBg = Color(0xFFFFF0E0); val PendienteBorde = Color(0xFFFFD9B3)
    val Ok = Color(0xFF2E7D32); val OkFuerte = Color(0xFF4CAF50); val OkBg = Color(0xFFE8F5E9); val OkBorde = Color(0xFFC8E6C9)
    val Error = Color(0xFFD32F2F); val ErrorBg = Color(0xFFFFEBEE); val ErrorBorde = Color(0xFFFFCDD2)
    val Aviso = Color(0xFF8D6E00); val AvisoFuerte = Color(0xFFF9A825); val AvisoBg = Color(0xFFFFF8E1); val AvisoBorde = Color(0xFFFFE082)
    val Info = Color(0xFF1565C0); val InfoBg = Color(0xFFE3F2FD); val InfoBorde = Color(0xFFBBDEFB)

    // Mapa
    val Ruta = Color(0xFF1A73E8)
}
