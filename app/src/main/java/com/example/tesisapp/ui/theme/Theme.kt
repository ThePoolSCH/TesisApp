package com.example.tesisapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Radios de esquina estándar (8 / 12 / 16 dp, como `--r-s/m/l` de los mockups). */
object TesisShapes {
    val S = RoundedCornerShape(8.dp)
    val M = RoundedCornerShape(12.dp)
    val L = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(999.dp)
}

// Un solo esquema claro: la app es una herramienta de trabajo en la calle (sol directo) y su estilo es
// negro sobre blanco. Se quitó el color dinámico (Android 12+) y la paleta morada de la plantilla.
private val Esquema = lightColorScheme(
    primary = TesisColors.Negro,
    onPrimary = TesisColors.Blanco,
    primaryContainer = TesisColors.Gris200,
    onPrimaryContainer = TesisColors.Negro,
    secondary = TesisColors.Texto2,
    onSecondary = TesisColors.Blanco,
    secondaryContainer = TesisColors.Gris100,
    onSecondaryContainer = TesisColors.Negro,
    background = TesisColors.Fondo,
    onBackground = TesisColors.Texto,
    surface = TesisColors.Superficie,
    onSurface = TesisColors.Texto,
    surfaceVariant = TesisColors.Gris100,
    onSurfaceVariant = TesisColors.Texto2,
    surfaceContainer = TesisColors.Superficie,
    surfaceContainerLow = TesisColors.Superficie,
    surfaceContainerHigh = TesisColors.Superficie,
    outline = TesisColors.Gris300,
    outlineVariant = TesisColors.Gris200,
    error = TesisColors.Error,
    errorContainer = TesisColors.ErrorBg,
)

@Composable
fun TesisAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Esquema,
        typography = Typography,
        shapes = Shapes(small = TesisShapes.S, medium = TesisShapes.M, large = TesisShapes.L),
        content = content
    )
}
