package com.example.smartcivicgovernance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.User
import com.example.smartcivicgovernance.data.remote.FirebaseHelper
import com.example.smartcivicgovernance.databinding.FragmentRegisterBinding
import com.example.smartcivicgovernance.ui.citizen.CitizenDashboardActivity
import com.example.smartcivicgovernance.ui.worker.WorkerDashboardActivity
import com.example.smartcivicgovernance.ui.admin.AdminDashboardActivity

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup role spinner
        val roles = arrayOf("Citizen", "Worker", "Admin")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, roles)
        binding.spinnerRole.setAdapter(adapter)
        binding.spinnerRole.setText(roles[0], false)

        binding.spinnerRole.setOnItemClickListener { _, _, position, _ ->
            if (roles[position] == "Admin") {
                binding.tilAdminCode.visibility = View.VISIBLE
            } else {
                binding.tilAdminCode.visibility = View.GONE
            }
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etRegName.text.toString().trim()
            val email = binding.etRegEmail.text.toString().trim()
            val password = binding.etRegPassword.text.toString().trim()
            val confirmPassword = binding.etRegConfirmPassword.text.toString().trim()
            val selectedRole = binding.spinnerRole.text.toString().lowercase()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedRole == "admin") {
                val adminCode = binding.etAdminCode.text.toString().trim()
                if (adminCode != "smartadmin123") {
                    Toast.makeText(context, "Invalid admin authorization code.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val newUser = User(
                name = name,
                email = email,
                role = selectedRole
            )

            viewModel.register(newUser, password)
        }

        binding.tvLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.loadingState.observe(viewLifecycleOwner) { isLoading ->
            binding.btnRegister.isEnabled = !isLoading
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
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        } else {
                            navigateToDashboard(role)
                        }
                    } else {
                        Toast.makeText(context, "Registration succeeded, status error.", Toast.LENGTH_SHORT).show()
                    }
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
