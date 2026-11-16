package com.nasbus.app.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.nasbus.app.R
import com.nasbus.app.databinding.FragmentHomeBinding
import com.nasbus.app.util.SessionManager
import com.nasbus.app.util.hideKeyboard
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private lateinit var resultAdapter: SegmentResultAdapter

    private var isUpdatingTextProgrammatically = false

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

        setupRecyclerView()
        setupSearchControls()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        resultAdapter = SegmentResultAdapter { result ->
            binding.root.hideKeyboard()
            val bundle = Bundle().apply {
                putInt("routeId", result.routeId)
                putString("fromStopName", result.fromStopName)
                putString("toStopName", result.toStopName)
            }
            findNavController().navigate(R.id.action_home_to_routeDetail, bundle)
        }

        binding.rvResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = resultAdapter
        }
    }

    private fun setupSearchControls() {
        if (viewModel.fromText.isNotEmpty() && binding.actvFrom.text.toString() != viewModel.fromText) {
            isUpdatingTextProgrammatically = true
            binding.actvFrom.setText(viewModel.fromText, false)
            isUpdatingTextProgrammatically = false
        }
        if (viewModel.toText.isNotEmpty() && binding.actvTo.text.toString() != viewModel.toText) {
            isUpdatingTextProgrammatically = true
            binding.actvTo.setText(viewModel.toText, false)
            isUpdatingTextProgrammatically = false
        }

        binding.actvFrom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingTextProgrammatically) {
                    clearErrors()
                    viewModel.onFromTextChanged(s?.toString() ?: "")
                }
            }
        })

        binding.actvTo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingTextProgrammatically) {
                    clearErrors()
                    viewModel.onToTextChanged(s?.toString() ?: "")
                }
            }
        })

        binding.actvFrom.setOnItemClickListener { _, _, _, _ ->
            binding.root.hideKeyboard()
        }

        binding.actvTo.setOnItemClickListener { _, _, _, _ ->
            binding.root.hideKeyboard()
        }

        binding.btnSwap.setOnClickListener {
            binding.root.hideKeyboard()
            clearErrors()

            val targetRotation = binding.btnSwap.rotation + 180f
            binding.btnSwap.animate().rotation(targetRotation).setDuration(250).start()

            val currentFrom = binding.actvFrom.text.toString()
            val currentTo = binding.actvTo.text.toString()

            isUpdatingTextProgrammatically = true
            binding.actvFrom.setText(currentTo, false)
            binding.actvTo.setText(currentFrom, false)
            isUpdatingTextProgrammatically = false

            viewModel.swapStops()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.distinctStops.collect { stops ->
                        if (stops.isNotEmpty() && isAdded) {
                            val adapterFrom = ArrayAdapter(requireContext(), R.layout.item_dropdown_stop, stops)
                            val adapterTo = ArrayAdapter(requireContext(), R.layout.item_dropdown_stop, stops)
                            binding.actvFrom.setAdapter(adapterFrom)
                            binding.actvTo.setAdapter(adapterTo)
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is HomeUiState.Idle -> {
                                binding.progressBar.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.rvResults.visibility = View.GONE
                                clearErrors()
                            }
                            is HomeUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.rvResults.visibility = View.GONE
                                clearErrors()
                            }
                            is HomeUiState.Results -> {
                                binding.progressBar.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.rvResults.visibility = View.VISIBLE
                                clearErrors()
                                resultAdapter.submitList(state.list)
                            }
                            is HomeUiState.Empty -> {
                                binding.progressBar.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.VISIBLE
                                binding.rvResults.visibility = View.GONE
                                clearErrors()
                            }
                            is HomeUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.rvResults.visibility = View.GONE

                                binding.tilFrom.error = state.fromError
                                binding.tilTo.error = state.toError
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clearErrors() {
        binding.tilFrom.error = null
        binding.tilTo.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
