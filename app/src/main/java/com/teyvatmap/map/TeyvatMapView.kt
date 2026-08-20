package com.teyvatmap.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teyvatmap.data.MapArea
import com.teyvatmap.data.MapPoint
import com.teyvatmap.ui.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTile
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmMapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.projection.Projection
import kotlinx.coroutines.launch

class TeyvatMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : OsmMapView(context, attrs, defStyleAttr) {

    private var markersOverlay: Overlay? = null
    private var zonesOverlay: Overlay? = null
    private var mapInfo: com.teyvatmap.data.MapInfoDetail? = null
    private var markerIcons: MutableMap<Int, Drawable> = mutableMapOf()
    private val defaultMarker by lazy {
        createDefaultMarker()
    }

    init {
        initMap()
    }

    private fun initMap() {
        // Configure osmdroid
        Configuration.getInstance().userAgentValue = "TeyvatMap/1.0"

        // Set custom tile source
        val tileSource = object : XYTileSource(
            "Teyvat",
            0, 4, 256, ".png",
            arrayOf("https://act-webstatic.hoyoverse.com/map_manage/map/")
        ) {
            override fun getTileURLString(mapTile: MapTile): String {
                val version = mapInfo?.detailV2?.mapVersion ?: "4.5"
                val suffix = if (mapTile.zoomLevel < 0) "N${Math.abs(mapTile.zoomLevel)}" else "P${mapTile.zoomLevel}"
                return "https://act-webstatic.hoyoverse.com/map_manage/map/2/$version/${mapTile.x}_${mapTile.y}_$suffix.png"
            }
        }

        setTileSource(tileSource)
        setMultiTouchControls(true)
        setBuiltInZoomControls(true)
        setBuiltInZoomControls(true)

        // Set initial view to Teyvat center
        val center = GeoPoint(-9216.0, 18432.0)
        controller.setCenter(center)
        controller.setZoom(1.0)

        // Enable hardware acceleration
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
    }

    fun setMapInfo(info: com.teyvatmap.data.MapInfoDetail) {
        mapInfo = info
        fitContent()
    }

    fun updateMarkers(points: List<MapPoint>, markedIds: Set<Int>, showOnlyUncollected: Boolean) {
        clearMarkers()

        val overlay = object : Overlay(this) {
            override fun draw(canvas: Canvas, projection: Projection, drawShadow: Boolean) {
                // Custom drawing if needed
            }
        }

        points.forEach { point ->
            val isCollected = markedIds.contains(point.id)
            if (showOnlyUncollected && isCollected) return@forEach

            val marker = Marker(this)
            val geoPoint = toGeoPoint(point)
            marker.position = geoPoint
            marker.title = point.extAttrsMap?.get("title") ?: point.extAttrsMap?.get("name") ?: "Point ${point.id}"
            marker.snippet = if (isCollected) "✓ Collected" else "○ Uncollected"
            marker.icon = getMarkerIcon(point)
            marker.setInfoWindow(BasicInfoWindow(InfoWindow.DEFAULT_TITLE_MAX_WIDTH, this))
            overlay.add(marker)
        }

        overlays.add(overlay)
        markersOverlay = overlay
        invalidate()
    }

    private fun toGeoPoint(point: MapPoint): GeoPoint {
        val origin = mapInfo?.detailV2?.origin ?: listOf(24206, 8918)
        val lat = -(point.yPos + origin[1].toDouble())
        val lon = point.xPos + origin[0].toDouble()
        return GeoPoint(lat, lon)
    }

    private fun getMarkerIcon(point: MapPoint): Drawable {
        // Use icon_sign or label_id to determine icon
        val iconKey = point.iconSign ?: point.labelId
        return markerIcons.getOrPut(iconKey) {
            createMarkerForLabel(point.labelId, iconKey)
        }
    }

    private fun createMarkerForLabel(labelId: Int, iconSign: Int): Drawable {
        // Generate a simple colored marker based on label type
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // Color based on label category
        val color = when {
            labelId in 1..10 -> Color.parseColor("#FF6B6B")      // Red for resources
            labelId in 11..20 -> Color.parseColor("#4ECDC4")     // Teal for chests
            labelId in 21..30 -> Color.parseColor("#FFD93D")     // Yellow for quests
            labelId in 31..40 -> Color.parseColor("#6BCB77")     // Green for enemies
            labelId in 41..50 -> Color.parseColor("#A8D0E6")     // Blue for NPCs
            else -> Color.parseColor("#4FC3F7")                  // Default teal
        }

        // Draw circle
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

        // Draw white border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)

        // Draw inner dot for collected state
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun createDefaultMarker(): Drawable {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = MaterialTheme.colorScheme.primary.toArgb()
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, paint)
        return BitmapDrawable(resources, bitmap)
    }

    fun clearMarkers() {
        markersOverlay?.let { overlays.remove(it) }
        markersOverlay = null
    }

    fun updateZones(areas: List<MapArea>, show: Boolean) {
        if (!show) {
            zonesOverlay?.let { overlays.remove(it) }
            zonesOverlay = null
            invalidate()
            return
        }

        val overlay = object : Overlay(this) {
            override fun draw(canvas: Canvas, projection: Projection, drawShadow: Boolean) {
                val paint = Paint().apply {
                    color = MaterialTheme.colorScheme.primary.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    isAntiAlias = true
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                }

                mapInfo?.let { info ->
                    val origin = info.detailV2.origin
                    areas.forEach { area ->
                        val path = Path()
                        val points = arrayOf(
                            toGeoPoint(area.ry, area.lx, origin),
                            toGeoPoint(area.ly, area.lx, origin),
                            toGeoPoint(area.ly, area.rx, origin),
                            toGeoPoint(area.ry, area.rx, origin)
                        )
                        val screenPts = points.map { projection.toPixels(it, Point()) }
                        path.moveTo(screenPts[0].x.toFloat(), screenPts[0].y.toFloat())
                        screenPts.drop(1).forEach { pt ->
                            path.lineTo(pt.x.toFloat(), pt.y.toFloat())
                        }
                        path.close()
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }

        overlays.add(overlay)
        zonesOverlay = overlay
        invalidate()
    }

    private fun toGeoPoint(y: Int, x: Int, origin: List<Int>): GeoPoint {
        return GeoPoint(
            -(y + origin[1]).toDouble(),
            (x + origin[0]).toDouble()
        )
    }

    fun fitContent() {
        mapInfo?.let { info ->
            val origin = info.detailV2.origin
            val w = info.detailV2.totalSize[0].toDouble()
            val h = info.detailV2.totalSize[1].toDouble()
            val pad = info.detailV2.padding

            val cx = (pad[0] + (w - pad[0])) / 2
            val cy = (pad[1] + (h - pad[1])) / 2

            controller.setCenter(GeoPoint(-cy, cx))
            controller.setZoom(1.0)
        }
    }

    fun onResume() {
        onResume()
    }

    fun onPause() {
        onPause()
    }

    fun onDestroy() {
        clearMarkers()
        zonesOverlay?.let { overlays.remove(it) }
        zonesOverlay = null
        markerIcons.clear()
    }
}

// Composable wrapper for the native map view
@Composable
fun TeyvatMapView(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    viewModel: MapViewModel,
    onMapViewCreated: (TeyvatMapView) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mapView = remember { TeyvatMapView(context) }

    // Observe view model data and update map
    androidx.compose.runtime.DisposableEffect(viewModel) {
        onMapViewCreated(mapView)

        // Observe map info
        val mapInfoJob = androidx.lifecycle.lifecycleScope.launch {
            viewModel.mapInfo.collect { result ->
                result?.fold(
                    onSuccess = { mapInfo ->
                        mapView.setMapInfo(mapInfo)
                    },
                    onFailure = { _, _ -> },
                    onLoading = { },
                    onIdle = { }
                )
            }
        }

        // Observe areas
        val areasJob = androidx.lifecycle.lifecycleScope.launch {
            viewModel.areas.collect { result ->
                result?.fold(
                    onSuccess = { areas ->
                        mapView.updateZones(areas, viewModel.showZones.value)
                    },
                    onFailure = { _, _ -> },
                    onLoading = { },
                    onIdle = { }
                )
            }
        }

        // Observe points and marks
        val pointsJob = androidx.lifecycle.lifecycleScope.launch {
            val pointsFlow = viewModel.points
            val marksFlow = viewModel.markedPoints
            val uncollectedFlow = viewModel.onlyUncollected

            androidx.compose.runtime.snapshotFlow { pointsFlow.value }.combine(
                androidx.compose.runtime.snapshotFlow { marksFlow.value },
                androidx.compose.runtime.snapshotFlow { uncollectedFlow.value }
            ) { pointsState, marksState, showOnlyUncollected ->
                Triple(pointsState, marksState, showOnlyUncollected)
            }.collect { (pointsState, marksState, showOnlyUncollected) ->
                val points = pointsState.fold(
                    onSuccess = { it },
                    onFailure = { emptyList() },
                    onLoading = { emptyList() },
                    onIdle = { emptyList() }
                )
                val marks = marksState.fold(
                    onSuccess = { it },
                    onFailure = { emptySet() },
                    onLoading = { emptySet() },
                    onIdle = { emptySet() }
                )
                mapView.updateMarkers(points, marks, showOnlyUncollected)
            }
        }

        onDispose {
            mapInfoJob.cancel()
            areasJob.cancel()
            pointsJob.cancel()
            mapView.onDestroy()
        }
    }

    // Lifecycle handling
    androidx.compose.runtime.DisposableEffect(androidx.lifecycle.LocalLifecycleOwner.current.lifecycle) {
        val observer = androidx.lifecycle.DefaultLifecycleObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        androidx.lifecycle.LocalLifecycleOwner.current.lifecycle.addObserver(observer)
        onDispose {
            androidx.lifecycle.LocalLifecycleOwner.current.lifecycle.removeObserver(observer)
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { it.onResume() }
    )
}