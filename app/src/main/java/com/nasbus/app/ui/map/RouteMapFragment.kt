package com.nasbus.app.ui.map

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nasbus.app.data.local.entity.StopEntity
import com.nasbus.app.databinding.BottomSheetStopInfoBinding
import com.nasbus.app.databinding.FragmentRouteMapBinding
import kotlinx.coroutines.launch
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

class RouteMapFragment : Fragment() {

    private var _binding: FragmentRouteMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RouteMapViewModel by viewModels()
    private var maplibreMap: MapLibreMap? = null
    private var mapRenderer: MapRenderer? = null

    private var routeId: Int = 1
    private var fromStopName: String? = null
    private var toStopName: String? = null

    private var isOnlineStyleLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            routeId = args.getInt(ARG_ROUTE_ID, 1)
            fromStopName = args.getString(ARG_FROM_STOP_NAME)
            toStopName = args.getString(ARG_TO_STOP_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.getMapAsync { map ->
            maplibreMap = map
            map.uiSettings.isAttributionEnabled = true
            loadMapStyle(map)
        }

        observeViewModel()
        viewModel.loadRouteData(routeId, fromStopName, toStopName)
    }

    private fun loadMapStyle(map: MapLibreMap) {
        val context = context
        val hasInternet = context != null && isNetworkAvailable(context)

        if (hasInternet) {
            val onlineStyleUri = "https://tiles.openfreemap.org/styles/liberty"

            binding.mapView.addOnDidFailLoadingMapListener {
                if (!isOnlineStyleLoaded) {
                    loadOfflineFallbackStyle(map)
                }
            }

            map.setStyle(Style.Builder().fromUri(onlineStyleUri)) { style ->
                isOnlineStyleLoaded = true
                onStyleReady(map, style)
            }
        } else {
            loadOfflineFallbackStyle(map)
        }
    }

    private fun loadOfflineFallbackStyle(map: MapLibreMap) {
        if (!isAdded || _binding == null) return
        Toast.makeText(requireContext(), "Map offline: showing route only", Toast.LENGTH_LONG).show()

        val offlineJson = readAssetStyleJson()
        map.setStyle(Style.Builder().fromJson(offlineJson)) { style ->
            onStyleReady(map, style)
        }
    }

    private fun onStyleReady(map: MapLibreMap, style: Style) {
        mapRenderer = MapRenderer(requireContext(), map, style)
        renderIfDataReady()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun readAssetStyleJson(): String {
        return try {
            requireContext().assets.open("offline_style.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load offline_style.json", e)
            """{"version": 8, "name": "Fallback", "layers": [{"id": "background", "type": "background", "paint": {"background-color": "#EFEFEF"}}]}"""
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    binding.progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE

                    if (uiState.error != null) {
                        Toast.makeText(context, uiState.error, Toast.LENGTH_LONG).show()
                    }

                    uiState.route?.let { route ->
                        val from = uiState.fromStopName
                        val to = uiState.toStopName
                        if (!from.isNullOrBlank() && !to.isNullOrBlank()) {
                            binding.tvRouteTitle.text = "$from → $to"
                        } else {
                            binding.tvRouteTitle.text = "${route.originName} → ${route.destinationName}"
                        }

                        val company = if (uiState.companyName.isNotBlank()) uiState.companyName else "Bus Company"
                        binding.tvRouteSubtitle.text = "${route.routeName} • $company"
                    }

                    renderIfDataReady()
                }
            }
        }
    }

    private fun renderIfDataReady() {
        val uiState = viewModel.uiState.value
        val renderer = mapRenderer
        val route = uiState.route
        if (renderer != null && route != null && uiState.stops.isNotEmpty()) {
            renderer.renderRoute(
                routeId = route.routeId,
                stops = uiState.stops,
                pathPolylineJson = route.pathPolyline,
                fromStopName = uiState.fromStopName,
                toStopName = uiState.toStopName
            ) { clickedStop ->
                showStopBottomSheet(clickedStop)
            }
        }
    }

    private fun showStopBottomSheet(stop: StopEntity) {
        val context = context ?: return
        val dialog = BottomSheetDialog(context)
        val sheetBinding = BottomSheetStopInfoBinding.inflate(layoutInflater)

        sheetBinding.tvStopName.text = stop.stopName
        sheetBinding.tvStopSequence.text = "Stop #${stop.sequenceOrder}"
        sheetBinding.tvStopDistance.text = "${stop.distanceFromOriginKm} km from origin"
        sheetBinding.tvStopCoords.text = "Lat: ${stop.latitude}, Lng: ${stop.longitude}"

        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    // MapView Lifecycle methods required by MapLibre
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "RouteMapFragment"
        const val ARG_ROUTE_ID = "routeId"
        const val ARG_FROM_STOP_NAME = "fromStopName"
        const val ARG_TO_STOP_NAME = "toStopName"
    }
}
