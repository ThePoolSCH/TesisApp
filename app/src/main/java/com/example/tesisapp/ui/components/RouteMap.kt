package com.example.tesisapp.ui.components

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tesisapp.domain.model.RouteStop
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@Composable
fun RouteMap(
    modifier: Modifier = Modifier,
    geometry: List<List<Double>>, // [[lon, lat], ...]
    stops: List<RouteStop>,
    isUserLocationEnabled: Boolean = false
) {
    val context = LocalContext.current

    // --- CONFIGURACIÓN DE OSMDROID (CRUCIAL PARA EVITAR ERRORES DE PERMISO) ---
    remember {
        // 1. Cargar preferencias
        Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
        // 2. User Agent para evitar baneos de servidores de mapas
        Configuration.getInstance().userAgentValue = context.packageName

        // 3. FIX IMPORTANTE: Redireccionar caché al almacenamiento interno de la app
        // Esto evita el error de WRITE_EXTERNAL_STORAGE en Android 10+
        val osmPath = File(context.filesDir, "osmdroid")
        osmPath.mkdirs()
        Configuration.getInstance().osmdroidBasePath = osmPath
        Configuration.getInstance().osmdroidTileCache = File(osmPath, "tile")

        true
    }

    // Configuración del Overlay de ubicación del usuario
    var myLocationOverlay: MyLocationNewOverlay? by remember { mutableStateOf(null) }


    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                // Evita que el mapa repita el mundo horizontalmente infinitamente
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false

                // Zoom inicial por defecto
                controller.setZoom(15.0)
                myLocationOverlay = MyLocationNewOverlay(
                    GpsMyLocationProvider(ctx),
                    this  // 'this' es el MapView
                ).apply {
                    enableMyLocation()
                    enableFollowLocation()
                    isDrawAccuracyEnabled = true
                }
                // Añadir capa de ubicación (se mostrará/ocultará en update)
                overlays.add(myLocationOverlay)
            }
        },
        update = { mapView ->
            // --- GESTIÓN DE UBICACIÓN ---
            myLocationOverlay?.let { overlay ->
                if (isUserLocationEnabled) {
                    if (!overlay.isMyLocationEnabled) {
                        overlay.enableMyLocation()
                    }
                } else {
                    overlay.disableMyLocation()
                }
            }

            // --- LIMPIEZA Y DIBUJO DE RUTA ---
            // Borramos todo excepto la capa de "Mi Ubicación"
            mapView.overlays.removeAll { it !is MyLocationNewOverlay }

            // 1. DIBUJAR LÍNEA AZUL (RUTA)
            // Convertimos [Lon, Lat] (Odoo) a GeoPoint(Lat, Lon) (OSM)
            val geoPoints = geometry.map { point -> GeoPoint(point[1], point[0]) }

            if (geoPoints.isNotEmpty()) {
                val line = Polyline().apply {
                    outlinePaint.color = Color.BLUE
                    outlinePaint.strokeWidth = 15f // Un poco más grueso para visibilidad
                    // Hacemos que sea redondeado en las esquinas
                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                }
                line.setPoints(geoPoints)
                mapView.overlays.add(line)
            }

            // 2. DIBUJAR MARCADORES (PARADAS)
            stops.forEach { stop ->
                if (stop.latitude != 0.0 || stop.longitude != 0.0) {
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(stop.latitude, stop.longitude)
                        title = "${stop.sequence}. ${stop.partnerName}"
                        snippet = stop.address
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        // Opcional: Si quieres iconos personalizados según estado
                        // if (stop.visitState == "arrived") ...
                    }
                    mapView.overlays.add(marker)
                }
            }

            // 3. ENFOQUE DE CÁMARA (ZOOM AUTOMÁTICO)
            if (geoPoints.isNotEmpty()) {
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                mapView.post {
                    mapView.zoomToBoundingBox(boundingBox, true, 150)
                }
            } else if (isUserLocationEnabled && myLocationOverlay?.myLocation != null) {
                mapView.controller.animateTo(myLocationOverlay?.myLocation)
            }

            // Forzar redibujado
            mapView.invalidate()
        }
    )

    // Limpieza de recursos al destruir el composable
    DisposableEffect(Unit) {
        onDispose {
            myLocationOverlay?.disableMyLocation()
        }
    }
}