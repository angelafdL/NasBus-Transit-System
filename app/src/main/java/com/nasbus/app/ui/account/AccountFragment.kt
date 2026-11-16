package com.nasbus.app.ui.account

import android.content.pm.PackageInfo
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nasbus.app.R
import com.nasbus.app.databinding.FragmentAccountBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAppVersion()
        observeViewModel()

        viewModel.loadUserData()

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun setupAppVersion() {
        val versionText = try {
            val pInfo: PackageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            "NasBus v${pInfo.versionName}"
        } catch (e: Exception) {
            "NasBus v1.0"
        }
        binding.tvAppVersion.text = versionText
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AccountUiState.Loading -> {
                            // Loading state
                        }
                        is AccountUiState.Success -> {
                            val user = state.user
                            val initial = user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                            binding.tvAvatarInitial.text = initial
                            binding.tvUsername.text = user.username
                            binding.tvEmail.text = user.email

                            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
                            val memberSinceDate = dateFormat.format(Date(user.createdAt))
                            binding.tvMemberSince.text = "Member since $memberSinceDate"
                        }
                        is AccountUiState.LoggedOut -> {
                            navigateToLogin()
                        }
                    }
                }
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log out")
            .setMessage("Log out of NasBus?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.logout()
                navigateToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToLogin() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.accountFragment) {
            navController.navigate(R.id.action_account_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
