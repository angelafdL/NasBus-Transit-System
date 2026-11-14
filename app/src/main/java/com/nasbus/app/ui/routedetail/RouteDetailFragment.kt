package com.nasbus.app.ui.routedetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nasbus.app.R
import com.nasbus.app.databinding.FragmentRouteDetailBinding

class RouteDetailFragment : Fragment() {

    private var _binding: FragmentRouteDetailBinding? = null
    private val binding get() = _binding!!

    private var routeId: Int = 1
    private var fromStopName: String = ""
    private var toStopName: String = ""

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

        binding.tvRouteId.text = "Route ID: $routeId"
        binding.tvFromStop.text = "From: $fromStopName"
        binding.tvToStop.text = "To: $toStopName"

        binding.btnOpenRouteMap.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("routeId", routeId)
                putString("fromStopName", fromStopName)
                putString("toStopName", toStopName)
            }
            findNavController().navigate(R.id.action_routeDetail_to_routeMap, bundle)
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
