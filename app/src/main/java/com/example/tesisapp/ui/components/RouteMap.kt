package com.example.tesisapp.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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

@Composable
fun RouteMap(
    modifier: Modifier = Modifier,
    geometry: List<List<Double>>,
    stops: List<RouteStop>,
    isUserLocationEnabled: Boolean = false
) {
    val context = LocalContext.current

    var mapView by remember { mutableStateOf<MapView?>(null) }

    // 1. CONFIGURACIÓN INICIAL
    remember {
        Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue = context.packageName

        val osmPath = File(context.filesDir, "osmdroid")
        osmPath.mkdirs()
        Configuration.getInstance().osmdroidBasePath = osmPath
        Configuration.getInstance().osmdroidTileCache = File(osmPath, "tile")
        true
    }

    // 2. VISTA
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
                controller.setZoom(15.0)

                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlay.enableMyLocation()
                locationOverlay.isDrawAccuracyEnabled = true
                overlays.add(locationOverlay)

                mapView = this
            }
        },
        update = { } // Dejar vacío para rendimiento
    )

    // 3. LÓGICA DE DIBUJO (Se ejecuta solo al cambiar datos)
    LaunchedEffect(mapView, geometry, stops) {
        val map = mapView ?: return@LaunchedEffect

        // A. Limpiar
        val overlaysToRemove = map.overlays.filter { it !is MyLocationNewOverlay }
        map.overlays.removeAll(overlaysToRemove)

        // B. Ruta
        val geoPoints = geometry.map { point -> GeoPoint(point[1], point[0]) }
        if (geoPoints.isNotEmpty()) {
            val line = Polyline().apply {
                outlinePaint.color = Color.parseColor("#3B82F6")
                outlinePaint.strokeWidth = 15f
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                setPoints(geoPoints)
            }
            map.overlays.add(line)
        }

        // --- PRE-CALCULO DEL ICONO PEQUEÑO ---
        // Hacemos esto FUERA del bucle para ahorrar memoria y CPU
        val defaultDrawable = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)
        val baseBitmap = (defaultDrawable as? BitmapDrawable)?.bitmap

        val scaledBitmap = if (baseBitmap != null) {
            // REDUCIR TAMAÑO A LA MITAD (0.5)
            val width = (baseBitmap.width * 0.7f).toInt()
            val height = (baseBitmap.height * 0.7f).toInt()
            Bitmap.createScaledBitmap(baseBitmap, width, height, true)
        } else {
            null
        }
        // -------------------------------------

        // C. Marcadores
        stops.forEach { stop ->
            if (stop.latitude != 0.0 || stop.longitude != 0.0) {
                val marker = Marker(map)
                marker.position = GeoPoint(stop.latitude, stop.longitude)
                marker.title = "${stop.sequence}. ${stop.partnerName}"
                marker.snippet = stop.address ?: ""
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // Crear icono a partir del bitmap escalado
                val iconDrawable = if (scaledBitmap != null) {
                    BitmapDrawable(context.resources, scaledBitmap)
                } else {
                    defaultDrawable // Fallback si falla el escalado
                }

                // Mutar para poder colorear individualmente
                val coloredIcon = iconDrawable?.constantState?.newDrawable()?.mutate()

                when (stop.visitState) {
                    "Visitada", "done", "arrived" ->
                        coloredIcon?.setColorFilter(Color.parseColor("#10B981"), PorterDuff.Mode.SRC_IN)
                    "En visita", "arrived_current" ->
                        coloredIcon?.setColorFilter(Color.parseColor("#F59E0B"), PorterDuff.Mode.SRC_IN)
                    "No visitada", "skipped" ->
                        coloredIcon?.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
                    else ->
                        coloredIcon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                }
                marker.icon = coloredIcon
                map.overlays.add(marker)
            }
        }

        // D. Zoom
        if (geoPoints.isNotEmpty()) {
            try {
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                map.post { map.zoomToBoundingBox(boundingBox, true, 150) }
            } catch (e: Exception) { e.printStackTrace() }
        }

        map.invalidate()
    }

    // 4. LÓGICA DE UBICACIÓN
    LaunchedEffect(mapView, isUserLocationEnabled) {
        val map = mapView ?: return@LaunchedEffect
        val locationOverlay = map.overlays.firstOrNull { it is MyLocationNewOverlay } as? MyLocationNewOverlay

        if (isUserLocationEnabled) {
            if (locationOverlay?.isMyLocationEnabled == false) {
                locationOverlay.enableMyLocation()
            }
            if (geometry.isEmpty() && locationOverlay?.myLocation != null) {
                map.controller.animateTo(locationOverlay.myLocation)
            }
        } else {
            locationOverlay?.disableMyLocation()
        }
    }
}