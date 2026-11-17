package com.nasbus.app.ui.routedetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nasbus.app.R
import com.nasbus.app.data.local.model.RouteDetailDataModel
import com.nasbus.app.databinding.FragmentRouteDetailBinding
import com.nasbus.app.util.FareCalculator
import com.nasbus.app.util.TravelTimeEstimator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class RouteDetailFragment : Fragment() {

    private var _binding: FragmentRouteDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RouteDetailViewModel by viewModels()
    private lateinit var stopAdapter: RouteStopAdapter
    private lateinit var tripAdapter: RouteTripAdapter

    private var routeId: Int = 1
    private var fromStopName: String = ""
    private var toStopName: String = ""

    private var cachedDetailData: RouteDetailDataModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            routeId = args.getInt(ARG_ROUTE_ID, 1)
            fromStopName = args.getString(ARG_FROM_STOP_NAME, "")
            toStopName = args.getString(ARG_TO_STOP_NAME, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
        setupMinuteTicker()

        viewModel.loadRouteDetail(routeId, fromStopName, toStopName)

        binding.btnViewOnMap.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("routeId", routeId)
                putString("fromStopName", fromStopName)
                putString("toStopName", toStopName)
            }
            findNavController().navigate(R.id.action_routeDetail_to_routeMap, bundle)
        }
    }

    override fun onResume() {
        super.onResume()
        cachedDetailData?.let { updateTripsSection(it) }
    }

    private fun setupRecyclerViews() {
        stopAdapter = RouteStopAdapter()
        binding.rvStops.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stopAdapter
        }

        tripAdapter = RouteTripAdapter()
        binding.rvTrips.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tripAdapter
        }
    }

    private fun setupMinuteTicker() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    cachedDetailData?.let { updateTripsSection(it) }
                    delay(60_000L) // Refresh countdowns & relevance every minute
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RouteDetailUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.scrollContent.visibility = View.GONE
                            binding.layoutBottomButton.visibility = View.GONE
                            binding.tvErrorMessage.visibility = View.GONE
                        }
                        is RouteDetailUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.scrollContent.visibility = View.GONE
                            binding.layoutBottomButton.visibility = View.GONE
                            binding.tvErrorMessage.visibility = View.VISIBLE
                            binding.tvErrorMessage.text = state.message
                        }
                        is RouteDetailUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvErrorMessage.visibility = View.GONE
                            binding.scrollContent.visibility = View.VISIBLE
                            binding.layoutBottomButton.visibility = View.VISIBLE

                            val data = state.data
                            cachedDetailData = data
                            val from = state.fromStopName
                            val to = state.toStopName

                            // 1. Header Title & Subtitles
                            if (from.isNotBlank() && to.isNotBlank()) {
                                binding.tvTripTitle.text = "$from → $to"
                            } else {
                                binding.tvTripTitle.text = "${data.route.originName} → ${data.route.destinationName}"
                            }
                            binding.tvRouteCompanySubtitle.text = "${data.route.routeName} • ${data.companyName}"
                            binding.tvFullRouteOriginDest.text = "${data.route.originName} → ${data.route.destinationName}"

                            // 2. Summary Cards Data
                            val fromStop = data.stops.find { it.stopName.equals(from, ignoreCase = true) }
                            val toStop = data.stops.find { it.stopName.equals(to, ignoreCase = true) }

                            val segmentDist = if (fromStop != null && toStop != null && fromStop.sequenceOrder < toStop.sequenceOrder) {
                                (toStop.distanceFromOriginKm - fromStop.distanceFromOriginKm).coerceAtLeast(0.0)
                            } else {
                                data.route.distanceKm
                            }

                            val segmentRegFare = FareCalculator.regularFare(segmentDist, data.route.farePerKm)
                            val segmentDiscFare = FareCalculator.discountedFare(segmentRegFare)

                            val fullRouteRegFare = FareCalculator.regularFare(data.route.distanceKm, data.route.farePerKm)
                            val fullRouteDiscFare = FareCalculator.discountedFare(fullRouteRegFare)

                            val segmentMins = TravelTimeEstimator.estimateSegmentMinutes(
                                segmentDistanceKm = segmentDist,
                                routeDistanceKm = data.route.distanceKm,
                                routeEstimatedTravelTime = data.route.estimatedTravelTime
                            )

                            binding.tvSegmentTravelTime.text = TravelTimeEstimator.formatEstimatedSegmentTime(segmentMins)
                            binding.tvFullRouteTravelTime.text = "Full route: ${data.route.estimatedTravelTime}"

                            binding.tvSegmentFare.text = FareCalculator.formatPeso(segmentRegFare)
                            binding.tvSegmentDistance.text = "${String.format(Locale.US, "%.1f", segmentDist)} km"
                            binding.tvSegmentDiscountFare.text = "${FareCalculator.DISCOUNT_LABEL} (20% off)  ${FareCalculator.formatPeso(segmentDiscFare)}"
                            binding.tvFullRouteFare.text = "Full route: ${FareCalculator.formatPeso(fullRouteRegFare)}  •  ${FareCalculator.DISCOUNT_LABEL} ${FareCalculator.formatPeso(fullRouteDiscFare)}"

                            // 3. Trips Section Processing
                            updateTripsSection(data)

                            // 4. Stops RecyclerView Timeline
                            stopAdapter.submitList(data.stops)
                            stopAdapter.setHighlightSegment(from, to)
                        }
                    }
                }
            }
        }
    }

    private fun updateTripsSection(data: RouteDetailDataModel) {
        val schedules = data.schedules
        if (schedules.isEmpty()) {
            binding.tvEmptyTrips.text = "No schedules available for this route."
            binding.tvEmptyTrips.visibility = View.VISIBLE
            binding.rvTrips.visibility = View.GONE
            return
        }

        val fromStop = data.stops.find { it.stopName.equals(fromStopName, ignoreCase = true) }
        val toStop = data.stops.find { it.stopName.equals(toStopName, ignoreCase = true) }

        val fromDist = fromStop?.distanceFromOriginKm ?: 0.0
        val toDist = toStop?.distanceFromOriginKm ?: data.route.distanceKm

        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val relevantTrips = TravelTimeEstimator.getRelevantTripItems(
            schedules = schedules,
            routeDistanceKm = data.route.distanceKm,
            routeEstimatedTravelTime = data.route.estimatedTravelTime,
            routeOriginName = data.route.originName,
            routeDestinationName = data.route.destinationName,
            fromStopName = fromStopName,
            toStopName = toStopName,
            fromStopDistanceFromOriginKm = fromDist,
            toStopDistanceFromOriginKm = toDist,
            currentMinutes = currentMinutes
        )

        if (relevantTrips.isNotEmpty()) {
            binding.tvEmptyTrips.visibility = View.GONE
            binding.rvTrips.visibility = View.VISIBLE
            tripAdapter.submitList(relevantTrips)
        } else {
            // All trips today have passed the From stop -> show first trip tomorrow
            val firstTomorrowDepMins = TravelTimeEstimator.estimateTimeAtStop(
                schedules.first().departureTime,
                fromDist,
                data.route.distanceKm,
                data.route.estimatedTravelTime
            )
            val firstTomorrowFormatted = TravelTimeEstimator.formatClock(firstTomorrowDepMins)
            binding.tvEmptyTrips.text = "No more buses today for this trip.\nFirst bus tomorrow: $firstTomorrowFormatted"
            binding.tvEmptyTrips.visibility = View.VISIBLE
            binding.rvTrips.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ROUTE_ID = "routeId"
        const val ARG_FROM_STOP_NAME = "fromStopName"
        const val ARG_TO_STOP_NAME = "toStopName"
    }
}
