package com.nasbus.app.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nasbus.app.R
import com.nasbus.app.databinding.FragmentTripDetailBinding
import com.nasbus.app.util.FareCalculator
import com.nasbus.app.util.TravelTimeEstimator
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class TripDetailFragment : Fragment() {

    private var _binding: FragmentTripDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripDetailViewModel by viewModels()
    private lateinit var stopAdapter: TripStopTimeAdapter

    private var scheduleId: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            scheduleId = args.getInt(ARG_SCHEDULE_ID, 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        viewModel.loadTripDetail(scheduleId)

        binding.btnErrorBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        stopAdapter = TripStopTimeAdapter()
        binding.rvStopTimes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stopAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TripDetailUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.scrollContent.visibility = View.GONE
                            binding.layoutBottomButton.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                        }
                        is TripDetailUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.scrollContent.visibility = View.GONE
                            binding.layoutBottomButton.visibility = View.GONE
                            binding.layoutError.visibility = View.VISIBLE
                            binding.tvErrorMessage.text = state.message
                        }
                        is TripDetailUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                            binding.scrollContent.visibility = View.VISIBLE
                            binding.layoutBottomButton.visibility = View.VISIBLE

                            val data = state.data
                            val sch = data.schedule
                            val route = data.route
                            val stops = data.stops

                            // a. Header card
                            binding.tvBusId.text = sch.busId
                            binding.tvCompanyName.text = sch.companyName

                            val depMins = TravelTimeEstimator.parseClockToMinutes(sch.departureTime)
                            val cal = Calendar.getInstance()
                            val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

                            if (depMins > nowMins) {
                                binding.badgeTripStatus.text = "Upcoming today"
                                binding.badgeTripStatus.setBackgroundResource(R.drawable.bg_badge_next_bus)
                                binding.badgeTripStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                            } else {
                                binding.badgeTripStatus.text = "Already departed today"
                                binding.badgeTripStatus.setBackgroundResource(R.drawable.bg_chip_bus)
                                binding.badgeTripStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                            }

                            // b. Trip summary
                            binding.tvRouteName.text = route.routeName
                            binding.tvOriginDestination.text = "${route.originName} → ${route.destinationName}"
                            binding.tvDeparts.text = "Departs ${route.originName} at ${TravelTimeEstimator.formatClock(depMins)}"

                            val arrMins = TravelTimeEstimator.parseClockToMinutes(sch.arrivalTime) + if (sch.arrivalNextDay) 1440 else 0
                            binding.tvArrives.text = "Arrives ${route.destinationName} ~ ${TravelTimeEstimator.formatClock(arrMins)}"
                            binding.tvTravelTime.text = "Estimated travel time: ${route.estimatedTravelTime}"
                            binding.tvDistance.text = "Distance: ${String.format(Locale.US, "%.1f", route.distanceKm)} km"

                            // c. Fare card
                            val regFare = if (route.distanceKm > 0.0 && route.farePerKm > 0.0) {
                                FareCalculator.regularFare(route.distanceKm, route.farePerKm)
                            } else {
                                sch.estimatedFare.toInt()
                            }
                            val discFare = FareCalculator.discountedFare(regFare)

                            binding.tvRegularFare.text = "Estimated fare ${FareCalculator.formatPeso(regFare)}"
                            binding.tvDiscountFare.text = "${FareCalculator.DISCOUNT_LABEL} ${FareCalculator.formatPeso(discFare)}"

                            // d. Estimated stop times RecyclerView
                            stopAdapter.setScheduleInfo(
                                sch.departureTime,
                                sch.arrivalTime,
                                sch.arrivalNextDay,
                                route.distanceKm,
                                route.estimatedTravelTime
                            )
                            stopAdapter.submitList(stops)

                            // e. View route on map button
                            binding.btnViewRouteOnMap.setOnClickListener {
                                val bundle = Bundle().apply {
                                    putInt("routeId", route.routeId)
                                    putString("fromStopName", route.originName)
                                    putString("toStopName", route.destinationName)
                                }
                                findNavController().navigate(R.id.action_tripDetail_to_routeMap, bundle)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SCHEDULE_ID = "scheduleId"
    }
}
