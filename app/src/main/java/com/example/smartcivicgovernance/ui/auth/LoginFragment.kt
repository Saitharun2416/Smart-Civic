package com.example.smartcivicgovernance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.remote.FirebaseHelper
import com.example.smartcivicgovernance.databinding.FragmentLoginBinding
import com.example.smartcivicgovernance.ui.citizen.CitizenDashboardActivity
import com.example.smartcivicgovernance.ui.worker.WorkerDashboardActivity
import com.example.smartcivicgovernance.ui.admin.AdminDashboardActivity

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        binding.tvRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.loadingState.observe(viewLifecycleOwner) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
        }

        viewModel.errorState.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.authSuccess.observe(viewLifecycleOwner) { authResult ->
            val user = authResult.user
            if (user != null) {
                FirebaseHelper.checkUserStatus { role, disabled ->
                    if (role != null) {
                        if (disabled) {
                            val msg = "Your account is disabled. Please contact support."
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            FirebaseHelper.auth.signOut()
                        } else {
                            navigateToDashboard(role)
                        }
                    } else {
                        Toast.makeText(context, "Error fetching user role. Connection timeout, please try again or check your network.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewModel.resetSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Password reset email sent", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDashboard(role: String) {
        val intent = when (role) {
            "citizen" -> Intent(requireActivity(), CitizenDashboardActivity::class.java)
            "worker" -> Intent(requireActivity(), WorkerDashboardActivity::class.java)
            "admin" -> Intent(requireActivity(), AdminDashboardActivity::class.java)
            else -> Intent(requireActivity(), AuthActivity::class.java)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showForgotPasswordDialog() {
        val input = EditText(requireContext())
        input.hint = "Enter email address"
        
        AlertDialog.Builder(requireContext())
            .setTitle("Forgot Password")
            .setMessage("We will send a password reset link to your email.")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    viewModel.forgotPassword(email)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
