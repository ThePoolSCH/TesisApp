package com.example.tesisapp.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import kotlin.math.pow

@Composable
fun RouteMap(
    modifier: Modifier = Modifier,
    geometry: List<List<Double>>,
    stops: List<RouteStop>,
    isUserLocationEnabled: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCenteredOnUser by remember { mutableStateOf(false) }


    // Estado del mapa para manipularlo luego
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // 1. CONFIGURACIÓN INICIAL (Solo una vez)
    remember {
        Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue = context.packageName

        // Configuración de caché
        val osmPath = File(context.filesDir, "osmdroid")
        osmPath.mkdirs()
        Configuration.getInstance().osmdroidBasePath = osmPath
        Configuration.getInstance().osmdroidTileCache = File(osmPath, "tile")
        true
    }

    // 2. VISTA ANDROID (OSMDROID)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
                controller.setZoom(15.0)
                mapView = this
            }
        },
        update = {
            // Dejar vacío para mejor rendimiento, manejamos actualizaciones en LaunchedEffect
        }
    )

    // 3. MANEJO DEL CICLO DE VIDA (CRÍTICO PARA LA UBICACIÓN)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
        }
    }

    // 4. LÓGICA DE DIBUJO Y ACTUALIZACIÓN
    LaunchedEffect(mapView, geometry, stops, isUserLocationEnabled) {
        val map = mapView ?: return@LaunchedEffect

        // A. LIMPIEZA TOTAL (Para evitar duplicados y controlar el orden Z)
        map.overlays.clear()

        // B. CALCULAR GEOMETRÍA RECORTADA (Lógica solicitada)
        val slicedGeometry = calculateSlicedGeometry(geometry, stops)
        val geoPoints = slicedGeometry.map { point -> GeoPoint(point[1], point[0]) }

        // C. DIBUJAR POLILÍNEA (Capa inferior)
        if (geoPoints.isNotEmpty()) {
            val line = Polyline().apply {
                outlinePaint.color = Color.parseColor("#3B82F6") // Azul Odoo
                outlinePaint.strokeWidth = 15f
                outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                setPoints(geoPoints)
            }
            map.overlays.add(line)
        }

        // --- PREPARACIÓN DE ICONOS ---
        val defaultDrawable = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)
        val baseBitmap = (defaultDrawable as? BitmapDrawable)?.bitmap
        val scaledBitmap = if (baseBitmap != null) {
            val width = (baseBitmap.width * 0.7f).toInt()
            val height = (baseBitmap.height * 0.7f).toInt()
            Bitmap.createScaledBitmap(baseBitmap, width, height, true)
        } else null
        // ----------------------------

        // D. DIBUJAR MARCADORES (Capa media)
        stops.forEach { stop ->
            if (stop.latitude != 0.0 || stop.longitude != 0.0) {

                // Ocultar marcadores de paradas YA visitadas si queremos limpiar el mapa visualmente,
                // O los dejamos para referencia. Aquí los dejamos pero cambiamos color.

                val marker = Marker(map)
                marker.position = GeoPoint(stop.latitude, stop.longitude)
                marker.title = "${stop.sequence}. ${stop.partnerName}"
                marker.snippet = stop.address ?: ""
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                val iconDrawable = if (scaledBitmap != null) BitmapDrawable(context.resources, scaledBitmap) else defaultDrawable
                val coloredIcon = iconDrawable?.constantState?.newDrawable()?.mutate()

                when (stop.visitState) {
                    "Visitada", "done" ->
                        coloredIcon?.setColorFilter(Color.parseColor("#10B981"), PorterDuff.Mode.SRC_IN) // Verde
                    "En visita", "arrived" ->
                        coloredIcon?.setColorFilter(Color.parseColor("#F59E0B"), PorterDuff.Mode.SRC_IN) // Naranja
                    "No visitada", "skipped" ->
                        coloredIcon?.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
                    else ->
                        coloredIcon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN) // Pendiente
                }
                marker.icon = coloredIcon
                map.overlays.add(marker)
            }
        }

        // E. UBICACIÓN DEL USUARIO (Capa Superior - Z-Index más alto)
        if (isUserLocationEnabled) {
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
            locationOverlay.enableMyLocation() // Solo activa ubicación, sin seguir
            locationOverlay.isDrawAccuracyEnabled = true

            map.overlays.add(locationOverlay)

            // Centrar SOLO UNA VEZ
            locationOverlay.runOnFirstFix {
                if (!hasCenteredOnUser) {
                    hasCenteredOnUser = true
                    map.post {
                        map.controller.animateTo(locationOverlay.myLocation)
                    }
                }
            }
        }

        // F. ZOOM AUTOMÁTICO
        if (geoPoints.isNotEmpty()) {
            try {
                // Hacemos zoom a la ruta restante
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                // Agregamos un padding de 150px para que no quede pegado a los bordes
                map.post { map.zoomToBoundingBox(boundingBox, true, 150) }
            } catch (e: Exception) { e.printStackTrace() }
        }

        map.invalidate() // Forzar repintado
    }
}

/**
 * Función auxiliar para cortar la geometría.
 * Encuentra el punto más cercano a la última parada visitada y devuelve la lista desde ahí.
 */
private fun calculateSlicedGeometry(
    fullGeometry: List<List<Double>>,
    stops: List<RouteStop>
): List<List<Double>> {
    if (fullGeometry.isEmpty()) return emptyList()

    // 1. Encontrar la última parada que ya se completó o se está visitando
    // Buscamos paradas con estado 'done' o 'arrived'
    // Tomamos la última de la lista (asumiendo orden secuencial)
    val lastVisitedStop = stops.lastOrNull {
        it.visitState == "done" || it.visitState == "Visitada" ||
                it.visitState == "arrived" || it.visitState == "En visita"
    }

    // Si no se ha visitado ninguna, devolvemos todo
    if (lastVisitedStop == null) return fullGeometry

    val targetLat = lastVisitedStop.latitude
    val targetLon = lastVisitedStop.longitude

    // 2. Encontrar el índice en la lista de geometría más cercano a esa parada
    var minDistance = Double.MAX_VALUE
    var closestIndex = 0

    // Iteramos (esto es rápido para <10k puntos)
    fullGeometry.forEachIndexed { index, coord ->
        // coord es [lon, lat] según GeoJSON standard, Odoo lo manda así.
        // Asegúrate de verificar si Odoo manda [lat, lon] o [lon, lat].
        // En tu código original: GeoPoint(point[1], point[0]) -> Odoo manda [lon, lat].
        val gLon = coord[0]
        val gLat = coord[1]

        // Distancia euclidiana aproximada (suficiente para esto)
        val dist = (gLat - targetLat).pow(2) + (gLon - targetLon).pow(2)

        if (dist < minDistance) {
            minDistance = dist
            closestIndex = index
        }
    }

    // 3. Devolver la sublista desde ese índice hasta el final
    return fullGeometry.drop(closestIndex)
}