package com.nasbus.app.ui.schedule

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.nasbus.app.R
import com.nasbus.app.databinding.FragmentScheduleBinding
import com.nasbus.app.util.hideKeyboard
import kotlinx.coroutines.launch

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduleViewModel by viewModels()
    private lateinit var scheduleAdapter: ScheduleListAdapter

    private var fromAdapter: ArrayAdapter<String>? = null
    private var toAdapter: ArrayAdapter<String>? = null
    private var companyAdapter: ArrayAdapter<String>? = null

    private var isUpdatingTextProgrammatically = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterControls()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleListAdapter { item ->
            binding.root.hideKeyboard()
            val bundle = Bundle().apply {
                putInt("scheduleId", item.scheduleId)
            }
            findNavController().navigate(R.id.action_schedule_to_tripDetail, bundle)
        }

        binding.rvSchedules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }
    }

    private fun setupFilterControls() {
        // Collapsible Filter Card Header
        binding.layoutFilterHeader.setOnClickListener {
            binding.root.hideKeyboard()
            viewModel.toggleFilterExpanded()
        }

        // Clear Filters Buttons
        binding.btnClearFilters.setOnClickListener {
            binding.root.hideKeyboard()
            viewModel.clearAllFilters()
        }
        binding.btnEmptyClearFilters.setOnClickListener {
            binding.root.hideKeyboard()
            viewModel.clearAllFilters()
        }

        // Swap Terminals Button (200ms duration, multi-tap cancel)
        binding.btnSwap.setOnClickListener {
            binding.root.hideKeyboard()
            Log.d("Schedule", "swap tapped")

            binding.btnSwap.animate().cancel()
            val targetRotation = binding.btnSwap.rotation + 180f
            binding.btnSwap.animate().rotation(targetRotation).setDuration(200).start()

            viewModel.swapFromTo()
        }

        // Dropdowns Click Listeners
        binding.actvCompany.setOnItemClickListener { _, _, position, _ ->
            binding.root.hideKeyboard()
            val adapter = companyAdapter ?: return@setOnItemClickListener
            if (position in 0 until adapter.count) {
                val companyName = adapter.getItem(position).toString()
                viewModel.onCompanySelected(companyName)
            }
        }

        binding.actvFrom.setOnItemClickListener { _, _, position, _ ->
            binding.root.hideKeyboard()
            val adapter = fromAdapter ?: return@setOnItemClickListener
            if (position in 0 until adapter.count) {
                val from = adapter.getItem(position).toString()
                viewModel.onFromSelected(from)
            }
        }

        binding.actvTo.setOnItemClickListener { _, _, position, _ ->
            binding.root.hideKeyboard()
            val adapter = toAdapter ?: return@setOnItemClickListener
            if (position in 0 until adapter.count) {
                val to = adapter.getItem(position).toString()
                viewModel.onToSelected(to)
            }
        }

        // Time Pickers
        binding.etDepartAfter.setOnClickListener {
            binding.root.hideKeyboard()
            showTimePicker("Depart after") { hour, minute ->
                viewModel.setDepartAfter(hour, minute)
            }
        }

        binding.tilDepartAfter.setEndIconOnClickListener {
            binding.root.hideKeyboard()
            viewModel.clearDepartAfter()
        }

        binding.etArriveBefore.setOnClickListener {
            binding.root.hideKeyboard()
            showTimePicker("Arrive before") { hour, minute ->
                viewModel.setArriveBefore(hour, minute)
            }
        }

        binding.tilArriveBefore.setEndIconOnClickListener {
            binding.root.hideKeyboard()
            viewModel.clearArriveBefore()
        }
    }

    private fun showTimePicker(title: String, onTimeSet: (hour: Int, minute: Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(8)
            .setMinute(0)
            .setTitleText(title)
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSet(picker.hour, picker.minute)
        }
        picker.show(childFragmentManager, "time_picker")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect Metadata Dropdowns efficiently
                launch {
                    viewModel.allCompanies
                        .collect { companies ->
                            if (isAdded) {
                                val companyNames = listOf("All companies") + companies.map { it.companyName }
                                if (companyAdapter == null) {
                                    companyAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_stop, ArrayList(companyNames))
                                    binding.actvCompany.setAdapter(companyAdapter)
                                } else {
                                    companyAdapter?.clear()
                                    companyAdapter?.addAll(companyNames)
                                }
                            }
                        }
                }

                launch {
                    viewModel.availableFromStops
                        .collect { stops ->
                            if (isAdded) {
                                if (fromAdapter == null) {
                                    fromAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_stop, ArrayList(stops))
                                    binding.actvFrom.setAdapter(fromAdapter)
                                } else {
                                    fromAdapter?.clear()
                                    fromAdapter?.addAll(stops)
                                }
                            }
                        }
                }

                launch {
                    viewModel.availableToStops
                        .collect { stops ->
                            if (isAdded) {
                                if (toAdapter == null) {
                                    toAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_stop, ArrayList(stops))
                                    binding.actvTo.setAdapter(toAdapter)
                                } else {
                                    toAdapter?.clear()
                                    toAdapter?.addAll(stops)
                                }
                            }
                        }
                }

                // Render Filter UI State instantly on state changes
                launch {
                    viewModel.filterState
                        .collect { state ->
                            renderFilterState(state)
                        }
                }

                // Collect Results UI State independently
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is ScheduleUiState.Loading -> {
                                binding.progressLoadingThin.visibility = View.VISIBLE
                                binding.tvValidationError.visibility = View.GONE
                            }
                            is ScheduleUiState.ValidationError -> {
                                binding.progressLoadingThin.visibility = View.GONE
                                binding.rvSchedules.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.tvValidationError.visibility = View.VISIBLE
                                binding.tvValidationError.text = state.message
                                binding.tvResultCount.text = "0 trips found"
                            }
                            is ScheduleUiState.Error -> {
                                binding.progressLoadingThin.visibility = View.GONE
                                binding.tvValidationError.visibility = View.GONE
                                binding.rvSchedules.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.VISIBLE
                            }
                            is ScheduleUiState.Success -> {
                                binding.progressLoadingThin.visibility = View.GONE
                                binding.tvValidationError.visibility = View.GONE

                                val list = state.list
                                val count = state.totalCount
                                binding.tvResultCount.text = "$count trip${if (count == 1) "" else "s"} found"

                                scheduleAdapter.submitList(list)
                                Log.d("Schedule", "swap results ready at ${SystemClock.elapsedRealtime()} (${list.size} rows)")

                                if (list.isNotEmpty()) {
                                    binding.rvSchedules.visibility = View.VISIBLE
                                    binding.layoutEmptyState.visibility = View.GONE
                                } else {
                                    binding.rvSchedules.visibility = View.GONE
                                    binding.layoutEmptyState.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderFilterState(state: ScheduleFilterState) {
        isUpdatingTextProgrammatically = true

        val companyText = state.selectedCompany?.companyName ?: "All companies"
        if (binding.actvCompany.text.toString() != companyText) {
            binding.actvCompany.setText(companyText, false)
        }

        val fromText = state.selectedFrom ?: "Any"
        val currentFromDisplayed = binding.actvFrom.text.toString()
        if (currentFromDisplayed != fromText) {
            binding.actvFrom.setText(fromText, false)
            Log.d("Schedule", "swap text rendered at ${SystemClock.elapsedRealtime()}")
        }

        val toText = state.selectedTo ?: "Any"
        val currentToDisplayed = binding.actvTo.text.toString()
        if (currentToDisplayed != toText) {
            binding.actvTo.setText(toText, false)
            Log.d("Schedule", "swap text rendered at ${SystemClock.elapsedRealtime()}")
        }

        binding.etDepartAfter.setText(state.departAfter12h ?: "")
        binding.etArriveBefore.setText(state.arriveBefore12h ?: "")

        val isExpanded = state.isFilterExpanded
        binding.layoutFilterContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        binding.tvFilterArrow.text = if (isExpanded) "▲" else "▼"

        isUpdatingTextProgrammatically = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
