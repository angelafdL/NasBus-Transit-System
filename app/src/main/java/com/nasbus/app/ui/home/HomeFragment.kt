package com.nasbus.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nasbus.app.R
import com.nasbus.app.data.local.model.RouteListItem
import com.nasbus.app.databinding.FragmentHomeBinding
import com.nasbus.app.util.SessionManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private var selectedRouteId: Int = 1
    private var routeItems: List<RouteListItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        setupToolbar()
        observeViewModel()

        binding.btnOpenMap.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("routeId", selectedRouteId)
            }
            findNavController().navigate(R.id.action_home_to_routeMap, bundle)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
    }

    private fun logout() {
        sessionManager.clearSession()
        findNavController().navigate(R.id.action_home_to_login)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.routes.collect { items ->
                    routeItems = items
                    if (items.isNotEmpty()) {
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            items
                        )
                        binding.actvRouteSelector.setAdapter(adapter)

                        val defaultItem = items.find { it.routeId == 1 } ?: items.first()
                        binding.actvRouteSelector.setText(defaultItem.displayText, false)
                        selectedRouteId = defaultItem.routeId

                        binding.actvRouteSelector.setOnItemClickListener { _, _, position, _ ->
                            val selected = adapter.getItem(position)
                            if (selected != null) {
                                selectedRouteId = selected.routeId
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
}
