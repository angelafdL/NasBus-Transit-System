package com.nasbus.app.ui.schedule

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
                putInt("routeId", item.routeId)
                putString("fromStopName", item.originName)
                putString("toStopName", item.destinationName)
            }
            findNavController().navigate(R.id.action_schedule_to_routeDetail, bundle)
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
            viewModel.isFilterExpanded = !viewModel.isFilterExpanded
            updateFilterCardExpandState()
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

        // Swap Terminals Button
        binding.btnSwap.setOnClickListener {
            binding.root.hideKeyboard()
            val targetRotation = binding.btnSwap.rotation + 180f
            binding.btnSwap.animate().rotation(targetRotation).setDuration(250).start()
            viewModel.swapTerminals()
        }

        // Dropdowns Click Listeners
        binding.actvCompany.setOnItemClickListener { _, _, position, _ ->
            binding.root.hideKeyboard()
            val companyName = binding.actvCompany.adapter.getItem(position).toString()
            viewModel.onCompanySelected(companyName)
        }

        binding.actvFrom.setOnItemClickListener { _, _, position, _ ->
            binding.root.hideKeyboard()
            val from = binding.actvFrom.adapter.getItem(position).toString()
            viewModel.onFromSelected(from)
        }

        binding.actvTo.setOnItemClickListener { _, _, position, _ ->
            binding.root.hideKeyboard()
            val to = binding.actvTo.adapter.getItem(position).toString()
            viewModel.onToSelected(to)
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

    private fun updateFilterCardExpandState() {
        val isExpanded = viewModel.isFilterExpanded
        binding.layoutFilterContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        binding.tvFilterArrow.text = if (isExpanded) "▲" else "▼"
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCompanies.collect { companies ->
                        if (isAdded) {
                            val companyNames = listOf("All companies") + companies.map { it.companyName }
                            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_stop, companyNames)
                            binding.actvCompany.setAdapter(adapter)
                        }
                    }
                }

                launch {
                    viewModel.availableFromStops.collect { stops ->
                        if (isAdded) {
                            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_stop, stops)
                            binding.actvFrom.setAdapter(adapter)
                        }
                    }
                }

                launch {
                    viewModel.availableToStops.collect { stops ->
                        if (isAdded) {
                            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_stop, stops)
                            binding.actvTo.setAdapter(adapter)
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        updateUiFromViewModelState()

                        when (state) {
                            is ScheduleUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.rvSchedules.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.tvValidationError.visibility = View.GONE
                            }
                            is ScheduleUiState.ValidationError -> {
                                binding.progressBar.visibility = View.GONE
                                binding.rvSchedules.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.tvValidationError.visibility = View.VISIBLE
                                binding.tvValidationError.text = state.message
                                binding.tvResultCount.text = "0 trips found"
                            }
                            is ScheduleUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.rvSchedules.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.VISIBLE
                                binding.tvValidationError.visibility = View.GONE
                            }
                            is ScheduleUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvValidationError.visibility = View.GONE

                                val list = state.list
                                val count = state.totalCount
                                binding.tvResultCount.text = "$count trip${if (count == 1) "" else "s"} found"

                                if (list.isNotEmpty()) {
                                    binding.rvSchedules.visibility = View.VISIBLE
                                    binding.layoutEmptyState.visibility = View.GONE
                                    scheduleAdapter.submitList(list)
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

    private fun updateUiFromViewModelState() {
        isUpdatingTextProgrammatically = true

        val companyText = viewModel.selectedCompany?.companyName ?: "All companies"
        if (binding.actvCompany.text.toString() != companyText) {
            binding.actvCompany.setText(companyText, false)
        }

        val fromText = viewModel.selectedFrom ?: "Any"
        if (binding.actvFrom.text.toString() != fromText) {
            binding.actvFrom.setText(fromText, false)
        }

        val toText = viewModel.selectedTo ?: "Any"
        if (binding.actvTo.text.toString() != toText) {
            binding.actvTo.setText(toText, false)
        }

        binding.etDepartAfter.setText(viewModel.departAfter12h ?: "")
        binding.etArriveBefore.setText(viewModel.arriveBefore12h ?: "")

        updateFilterCardExpandState()

        isUpdatingTextProgrammatically = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
