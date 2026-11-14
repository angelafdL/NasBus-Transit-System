package com.nasbus.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nasbus.app.R
import com.nasbus.app.databinding.FragmentRegisterBinding
import com.nasbus.app.util.hideKeyboard
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            performRegister()
        }

        binding.btnToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        binding.etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performRegister()
                true
            } else false
        }
    }

    private fun performRegister() {
        binding.root.hideKeyboard()
        clearErrors()

        val username = binding.etUsername.text.toString()
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        viewModel.register(username, email, password, confirmPassword)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RegisterUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnRegister.isEnabled = true
                        }
                        is RegisterUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnRegister.isEnabled = false
                        }
                        is RegisterUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnRegister.isEnabled = true
                            viewModel.resetState()
                            navigateToHome()
                        }
                        is RegisterUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnRegister.isEnabled = true

                            if (state.usernameError != null) {
                                binding.tilUsername.error = state.usernameError
                            }
                            if (state.emailError != null) {
                                binding.tilEmail.error = state.emailError
                            }
                            if (state.passwordError != null) {
                                binding.tilPassword.error = state.passwordError
                            }
                            if (state.confirmPasswordError != null) {
                                binding.tilConfirmPassword.error = state.confirmPasswordError
                            }
                            if (state.genericError != null) {
                                Toast.makeText(requireContext(), state.genericError, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clearErrors() {
        binding.tilUsername.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_register_to_home)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
