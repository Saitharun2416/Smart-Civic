package com.example.smartcivicgovernance.ui.citizen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.smartcivicgovernance.data.model.User
import com.example.smartcivicgovernance.data.remote.FirebaseHelper
import com.example.smartcivicgovernance.data.repository.UserRepository
import com.example.smartcivicgovernance.data.repository.WorkerRepository
import com.example.smartcivicgovernance.databinding.FragmentProfileBinding
import com.example.smartcivicgovernance.ui.auth.AuthActivity
import com.example.smartcivicgovernance.ui.auth.AuthViewModel
import com.google.android.material.chip.Chip

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val userRepo = UserRepository()
    private val workerRepo = WorkerRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogout.setOnClickListener {
            userRepo.logout()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireActivity(), AuthActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        // Setup Dark Mode Switch status based on current mode
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        binding.switchDarkMode.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_YES
        
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Setup Support click listeners
        binding.btnSupportEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@smartcivic.gov")
                putExtra(Intent.EXTRA_SUBJECT, "Smart Civic Support Request")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSupportCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:+18001234567")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Dialer not found", Toast.LENGTH_SHORT).show()
            }
        }

        setupObservers()

        val uid = FirebaseHelper.getCurrentUid()
        if (uid != null) {
            authViewModel.fetchProfile(uid)
        }
    }

    private fun setupObservers() {
        authViewModel.userProfile.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvProfileName.text = user.name
                binding.tvProfileEmail.text = user.email
                binding.chipRole.text = user.role.uppercase()

                if (user.role == "worker") {
                    loadWorkerBadges(user.uid)
                }
            }
        }
    }

    private fun loadWorkerBadges(workerId: String) {
        binding.cardBadges.visibility = View.VISIBLE
        workerRepo.fetchWorkerDetails(workerId) { result ->
            result.fold(
                onSuccess = { worker ->
                    binding.chipGroupBadges.removeAllViews()
                    if (worker.badges.isEmpty()) {
                        val chip = Chip(requireContext()).apply {
                            text = "No Badges Yet"
                            isClickable = false
                        }
                        binding.chipGroupBadges.addView(chip)
                    } else {
                        for (badge in worker.badges) {
                            val chip = Chip(requireContext()).apply {
                                text = badge
                                isCheckable = false
                            }
                            binding.chipGroupBadges.addView(chip)
                        }
                    }
                },
                onFailure = {
                    binding.cardBadges.visibility = View.GONE
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
