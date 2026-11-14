package com.nasbus.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nasbus.app.R
import com.nasbus.app.data.local.entity.StopEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

class MapRenderer(
    private val context: Context,
    private val maplibreMap: MapLibreMap,
    private val style: Style
) {

    private val gson = Gson()

    fun renderRoute(
        routeId: Int,
        stops: List<StopEntity>,
        pathPolylineJson: String,
        fromStopName: String? = null,
        toStopName: String? = null,
        onStopClicked: (StopEntity) -> Unit
    ) {
        if (stops.isEmpty()) return

        val sortedStops = stops.sortedBy { it.sequenceOrder }

        // Parse polyline and segment cutting on background thread (Dispatchers.Default)
        CoroutineScope(Dispatchers.Default).launch {
            val (routePoints, usedPolyline) = parsePolylineWithSource(pathPolylineJson, sortedStops)
            Log.d(TAG, "route $routeId: polyline points=${routePoints.size}, source=${if (usedPolyline) "pathPolyline" else "stops"}")

            val segmentPoints = if (!fromStopName.isNullOrBlank() && !toStopName.isNullOrBlank()) {
                val fromStop = sortedStops.find { it.stopName.equals(fromStopName, ignoreCase = true) }
                val toStop = sortedStops.find { it.stopName.equals(toStopName, ignoreCase = true) }
                if (fromStop != null && toStop != null && fromStop.sequenceOrder < toStop.sequenceOrder) {
                    extractSegmentPoints(routePoints, fromStop, toStop)
                } else emptyList()
            } else emptyList()

            withContext(Dispatchers.Main) {
                updateMapOnMainThread(routeId, sortedStops, routePoints, segmentPoints, onStopClicked)
            }
        }
    }

    private fun updateMapOnMainThread(
        routeId: Int,
        sortedStops: List<StopEntity>,
        routePoints: List<Point>,
        segmentPoints: List<Point>,
        onStopClicked: (StopEntity) -> Unit
    ) {
        if (!style.isFullyLoaded) return

        clearOldMapElements()

        val firstStop = sortedStops.first()
        val lastStop = sortedStops.last()
        val firstStopId = firstStop.stopId
        val lastStopId = lastStop.stopId

        // 1. Add Origin and Destination pin images drawn with numbers
        val originBitmap = createPinBitmapWithNumber(R.drawable.ic_origin_pin, "1")
        val destinationBitmap = createPinBitmapWithNumber(R.drawable.ic_destination_pin, sortedStops.size.toString())
        if (originBitmap != null) style.addImage(ORIGIN_ICON_ID, originBitmap)
        if (destinationBitmap != null) style.addImage(DESTINATION_ICON_ID, destinationBitmap)

        // 2. Create and add canvas-drawn numbered stop bitmaps (~18dp) and 2-line wrapped label bitmaps
        var circleCount = 0
        var labelCount = 0

        sortedStops.forEach { stop ->
            val stopIconName = "stop-${stop.sequenceOrder}"
            val numberedBitmap = createNumberedStopBitmap(stop.sequenceOrder)
            style.addImage(stopIconName, numberedBitmap)
            circleCount++

            val isEndpoint = (stop.stopId == firstStopId || stop.stopId == lastStopId)
            val labelIconName = "label-${stop.stopId}"
            val labelBitmap = createStopNameLabelBitmap(stop.stopName, isBold = isEndpoint)
            style.addImage(labelIconName, labelBitmap)
            labelCount++
        }

        Log.d(TAG, "Registered $circleCount stop circle images and $labelCount label images for route $routeId")

        // Layer Order 1 (Bottom): Full Route Line
        val routeLineString = LineString.fromLngLats(routePoints)
        val routeFeatureCollection = FeatureCollection.fromFeature(Feature.fromGeometry(routeLineString))
        style.addSource(GeoJsonSource("route-source", routeFeatureCollection))
        val routeLineLayer = LineLayer("route-line-layer", "route-source").withProperties(
            PropertyFactory.lineColor(Color.parseColor("#1976D2")),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
        style.addLayer(routeLineLayer)

        // Layer Order 2: Segment Highlight (if available)
        if (segmentPoints.size >= 2) {
            val segmentLineString = LineString.fromLngLats(segmentPoints)
            val segmentFeatureCollection = FeatureCollection.fromFeature(Feature.fromGeometry(segmentLineString))
            style.addSource(GeoJsonSource("segment-source", segmentFeatureCollection))
            val segmentLayer = LineLayer("segment-line-layer", "segment-source").withProperties(
                PropertyFactory.lineColor(Color.parseColor("#E65100")),
                PropertyFactory.lineWidth(8f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
            style.addLayer(segmentLayer)
        }

        // Feature Lists for Intermediate Stops
        val stopFeatures = mutableListOf<Feature>()
        val stopLabelFeatures = mutableListOf<Feature>()

        sortedStops.forEach { stop ->
            if (stop.stopId != firstStopId && stop.stopId != lastStopId) {
                val point = Point.fromLngLat(stop.longitude, stop.latitude)

                val circleFeature = Feature.fromGeometry(point).apply {
                    addStringProperty("icon", "stop-${stop.sequenceOrder}")
                    addNumberProperty("stop_id", stop.stopId)
                    addStringProperty("stop_name", stop.stopName)
                    addNumberProperty("sequence_order", stop.sequenceOrder)
                    addNumberProperty("distance_km", stop.distanceFromOriginKm)
                }
                stopFeatures.add(circleFeature)

                val labelFeature = Feature.fromGeometry(point).apply {
                    addStringProperty("label_icon", "label-${stop.stopId}")
                    addNumberProperty("stop_id", stop.stopId)
                }
                stopLabelFeatures.add(labelFeature)
            }
        }

        // Layer Order 3: Intermediate Stop Name Labels (minZoom = 10f, centered directly below circle)
        val stopLabelsFeatureCollection = FeatureCollection.fromFeatures(stopLabelFeatures)
        style.addSource(GeoJsonSource("stop-labels-source", stopLabelsFeatureCollection))
        val stopLabelsLayer = SymbolLayer("stop-labels-layer", "stop-labels-source").withProperties(
            PropertyFactory.iconImage(Expression.get("label_icon")),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_TOP),
            PropertyFactory.iconOffset(arrayOf(0f, 12f)),
            PropertyFactory.iconAllowOverlap(false),
            PropertyFactory.iconIgnorePlacement(false)
        )
        stopLabelsLayer.minZoom = 10f
        style.addLayer(stopLabelsLayer)

        // Layer Order 4: Intermediate Stop Circles (iconPadding = 0f, symbolSortKey = sequence_order) -> WINS COLLISION OVER LABELS!
        val stopsFeatureCollection = FeatureCollection.fromFeatures(stopFeatures)
        style.addSource(GeoJsonSource("stops-source", stopsFeatureCollection))
        val stopsLayer = SymbolLayer("stops-layer", "stops-source").withProperties(
            PropertyFactory.iconImage(Expression.get("icon")),
            PropertyFactory.iconPadding(0f),
            PropertyFactory.symbolSortKey(Expression.get("sequence_order")),
            PropertyFactory.iconAllowOverlap(false),
            PropertyFactory.iconIgnorePlacement(false)
        )
        style.addLayer(stopsLayer)

        // Layer Order 5: Origin and Destination Name Labels (bold, always visible)
        val originLabelFeature = Feature.fromGeometry(Point.fromLngLat(firstStop.longitude, firstStop.latitude)).apply {
            addStringProperty("label_icon", "label-${firstStop.stopId}")
        }
        val destinationLabelFeature = Feature.fromGeometry(Point.fromLngLat(lastStop.longitude, lastStop.latitude)).apply {
            addStringProperty("label_icon", "label-${lastStop.stopId}")
        }
        val endpointLabelsCollection = FeatureCollection.fromFeatures(listOf(originLabelFeature, destinationLabelFeature))
        style.addSource(GeoJsonSource("endpoint-labels-source", endpointLabelsCollection))

        val endpointLabelsLayer = SymbolLayer("endpoint-labels-layer", "endpoint-labels-source").withProperties(
            PropertyFactory.iconImage(Expression.get("label_icon")),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_TOP),
            PropertyFactory.iconOffset(arrayOf(0f, 4f)),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
        )
        style.addLayer(endpointLabelsLayer)

        // Layer Order 6 (Top): Origin and Destination Pins with numbers inside (always visible)
        val originFeature = Feature.fromGeometry(Point.fromLngLat(firstStop.longitude, firstStop.latitude)).apply {
            addNumberProperty("stop_id", firstStop.stopId)
        }
        val destinationFeature = Feature.fromGeometry(Point.fromLngLat(lastStop.longitude, lastStop.latitude)).apply {
            addNumberProperty("stop_id", lastStop.stopId)
        }

        style.addSource(GeoJsonSource("origin-source", FeatureCollection.fromFeature(originFeature)))
        style.addSource(GeoJsonSource("destination-source", FeatureCollection.fromFeature(destinationFeature)))

        val originLayer = SymbolLayer("origin-layer", "origin-source").withProperties(
            PropertyFactory.iconImage(ORIGIN_ICON_ID),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM)
        )
        style.addLayer(originLayer)

        val destinationLayer = SymbolLayer("destination-layer", "destination-source").withProperties(
            PropertyFactory.iconImage(DESTINATION_ICON_ID),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM)
        )
        style.addLayer(destinationLayer)

        // Fit Camera Bounds
        fitCameraToBounds(routePoints)

        // Marker Click Listener
        maplibreMap.addOnMapClickListener { latLng ->
            val point = maplibreMap.projection.toScreenLocation(latLng)
            val rect = RectF(point.x - 25, point.y - 25, point.x + 25, point.y + 25)
            val features = maplibreMap.queryRenderedFeatures(rect, "origin-layer", "destination-layer", "stops-layer")

            if (features.isNotEmpty()) {
                val clickedFeature = features.first()
                val stopId = clickedFeature.getNumberProperty("stop_id")?.toInt()
                val clickedStop = sortedStops.find { it.stopId == stopId }
                if (clickedStop != null) {
                    onStopClicked(clickedStop)
                    true
                } else false
            } else false
        }
    }

    private fun clearOldMapElements() {
        style.removeLayer("origin-layer")
        style.removeLayer("destination-layer")
        style.removeLayer("endpoint-labels-layer")
        style.removeLayer("stops-layer")
        style.removeLayer("stop-labels-layer")
        style.removeLayer("segment-line-layer")
        style.removeLayer("route-line-layer")

        style.removeSource("origin-source")
        style.removeSource("destination-source")
        style.removeSource("endpoint-labels-source")
        style.removeSource("stops-source")
        style.removeSource("stop-labels-source")
        style.removeSource("segment-source")
        style.removeSource("route-source")

        style.removeImage(ORIGIN_ICON_ID)
        style.removeImage(DESTINATION_ICON_ID)
        for (i in 1..200) {
            style.removeImage("stop-$i")
            style.removeImage("label-$i")
        }
    }

    private fun createPinBitmapWithNumber(drawableResId: Int, numberText: String): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableResId) ?: return null
        val density = context.resources.displayMetrics.density
        val widthPx = drawable.intrinsicWidth.coerceAtLeast((36 * density).toInt())
        val heightPx = drawable.intrinsicHeight.coerceAtLeast((36 * density).toInt())

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, widthPx, heightPx)
        drawable.draw(canvas)

        val centerX = widthPx / 2f
        val centerY = heightPx * (9f / 24f)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (if (numberText.length > 2) 9f else 11f) * density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val fontMetrics = textPaint.fontMetrics
        val yOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f
        canvas.drawText(numberText, centerX, centerY - yOffset, textPaint)

        return bitmap
    }

    private fun createNumberedStopBitmap(number: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (18 * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f
        val radius = center - (1.5f * density)

        // Blue filled circle background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1565C0")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, radius, bgPaint)

        // White border outline
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        canvas.drawCircle(center, center, radius, strokePaint)

        // Centered bold white 10sp number text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10 * density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = textPaint.fontMetrics
        val yOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f
        canvas.drawText(number.toString(), center, center - yOffset, textPaint)

        return bitmap
    }

    private fun createStopNameLabelBitmap(name: String, isBold: Boolean = false): Bitmap {
        val density = context.resources.displayMetrics.density
        val lines = wrapTextToLines(name, maxCharsPerLine = 14, maxLines = 2)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#222222")
            textSize = 11 * density
            typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        val paddingH = 6 * density
        val paddingV = 4 * density
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = fontMetrics.descent - fontMetrics.ascent + (1 * density)

        var maxLineWidth = 0f
        lines.forEach { line ->
            val w = textPaint.measureText(line)
            if (w > maxLineWidth) maxLineWidth = w
        }

        val widthPx = (maxLineWidth + paddingH * 2).toInt().coerceAtLeast(1)
        val heightPx = (lineHeight * lines.size + paddingV * 2).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        val cornerRadius = 4 * density

        // Semi-transparent white background card
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(240, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Thin gray border
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC")
            style = Paint.Style.STROKE
            strokeWidth = 1 * density
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        // Draw wrapped lines centered
        val centerX = widthPx / 2f
        var startY = paddingV - fontMetrics.ascent

        lines.forEach { line ->
            canvas.drawText(line, centerX, startY, textPaint)
            startY += lineHeight
        }

        return bitmap
    }

    private fun wrapTextToLines(text: String, maxCharsPerLine: Int = 14, maxLines: Int = 2): List<String> {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return listOf(text)

        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine = word
            } else if (currentLine.length + 1 + word.length <= maxCharsPerLine) {
                currentLine += " $word"
            } else {
                lines.add(currentLine)
                currentLine = word
                if (lines.size == maxLines - 1) break
            }
        }

        if (currentLine.isNotEmpty() && lines.size < maxLines) {
            lines.add(currentLine)
        }

        if (lines.isNotEmpty()) {
            val lastIdx = lines.lastIndex
            var lastLine = lines[lastIdx]
            if (lastLine.length > maxCharsPerLine) {
                lastLine = lastLine.take(maxCharsPerLine - 1) + "…"
                lines[lastIdx] = lastLine
            }
        }

        return lines
    }

    private fun parsePolylineWithSource(jsonStr: String, sortedStops: List<StopEntity>): Pair<List<Point>, Boolean> {
        return try {
            if (jsonStr.isNotBlank()) {
                val type = object : TypeToken<List<List<Double>>>() {}.type
                val rawCoords: List<List<Double>>? = gson.fromJson(jsonStr, type)
                if (!rawCoords.isNullOrEmpty()) {
                    val points = rawCoords.mapNotNull { pair ->
                        if (pair.size >= 2) Point.fromLngLat(pair[0], pair[1]) else null
                    }
                    if (points.isNotEmpty()) {
                        Pair(points, true)
                    } else {
                        Pair(sortedStops.map { Point.fromLngLat(it.longitude, it.latitude) }, false)
                    }
                } else {
                    Pair(sortedStops.map { Point.fromLngLat(it.longitude, it.latitude) }, false)
                }
            } else {
                Pair(sortedStops.map { Point.fromLngLat(it.longitude, it.latitude) }, false)
            }
        } catch (e: Exception) {
            Pair(sortedStops.map { Point.fromLngLat(it.longitude, it.latitude) }, false)
        }
    }

    private fun extractSegmentPoints(
        routePoints: List<Point>,
        fromStop: StopEntity,
        toStop: StopEntity
    ): List<Point> {
        val startIndex = findClosestPointIndex(routePoints, fromStop.longitude, fromStop.latitude)
        val endIndex = findClosestPointIndex(routePoints, toStop.longitude, toStop.latitude)

        return if (startIndex <= endIndex) {
            routePoints.subList(startIndex, (endIndex + 1).coerceAtMost(routePoints.size))
        } else {
            routePoints.subList(endIndex, (startIndex + 1).coerceAtMost(routePoints.size))
        }
    }

    private fun findClosestPointIndex(points: List<Point>, lng: Double, lat: Double): Int {
        var minDistance = Double.MAX_VALUE
        var minIndex = 0
        points.forEachIndexed { index, point ->
            val dist = Math.hypot(point.longitude() - lng, point.latitude() - lat)
            if (dist < minDistance) {
                minDistance = dist
                minIndex = index
            }
        }
        return minIndex
    }

    private fun fitCameraToBounds(routePoints: List<Point>) {
        if (routePoints.isEmpty()) return
        val density = context.resources.displayMetrics.density
        val paddingLeft = (72 * density).toInt()
        val paddingRight = (72 * density).toInt()
        val paddingTop = (120 * density).toInt() // Accommodates header card
        val paddingBottom = (56 * density).toInt()

        val builder = LatLngBounds.Builder()
        routePoints.forEach { point ->
            builder.include(LatLng(point.latitude(), point.longitude()))
        }
        val bounds = builder.build()
        maplibreMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, paddingLeft, paddingTop, paddingRight, paddingBottom)
        )
    }

    companion object {
        private const val TAG = "MapRenderer"
        private const val ORIGIN_ICON_ID = "origin_icon"
        private const val DESTINATION_ICON_ID = "destination_icon"
    }
}
