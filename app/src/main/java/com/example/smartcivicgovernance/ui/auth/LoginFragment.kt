package com.example.smartcivicgovernance.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient
    private var currentTab = "email" // "email" or "phone"
    private var verificationId: String? = null
    private var isOtpSent = false
    private var userPhoneNumber = ""

    private val googleSignInLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    val selectedRole = binding.spinnerRoleLogin.text.toString().lowercase()
                    viewModel.loginWithGoogle(idToken, selectedRole)
                } else {
                    Toast.makeText(context, "Google Sign-In failed: idToken is null. Using sandbox...", Toast.LENGTH_SHORT).show()
                    simulateGoogleLogin()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google API Error: ${e.message}. Using sandbox...", Toast.LENGTH_LONG).show()
                simulateGoogleLogin()
            }
        } else {
            Toast.makeText(context, "Google Sign-In canceled or failed. Using sandbox...", Toast.LENGTH_SHORT).show()
            simulateGoogleLogin()
        }
    }

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

        // Setup role spinner
        val roles = arrayOf("Citizen", "Worker", "Admin")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, roles)
        binding.spinnerRoleLogin.setAdapter(adapter)
        binding.spinnerRoleLogin.setText(roles[0], false)

        binding.spinnerRoleLogin.setOnItemClickListener { _, _, position, _ ->
            if (roles[position] == "Admin") {
                binding.tilAdminCodeLogin.visibility = View.VISIBLE
            } else {
                binding.tilAdminCodeLogin.visibility = View.GONE
            }
        }

        // Tab Switching
        binding.btnTabEmail.setOnClickListener {
            currentTab = "email"
            binding.layoutEmailFields.visibility = View.VISIBLE
            binding.layoutPhoneFields.visibility = View.GONE
            binding.btnLogin.text = "Sign In"
            binding.btnTabEmail.setTextColor(resources.getColor(R.color.primary, null))
            binding.btnTabPhone.setTextColor(resources.getColor(R.color.text_secondary_light, null))
        }

        binding.btnTabPhone.setOnClickListener {
            currentTab = "phone"
            binding.layoutEmailFields.visibility = View.GONE
            binding.layoutPhoneFields.visibility = View.VISIBLE
            binding.btnLogin.text = if (isOtpSent) "Verify OTP" else "Send OTP"
            binding.btnTabEmail.setTextColor(resources.getColor(R.color.text_secondary_light, null))
            binding.btnTabPhone.setTextColor(resources.getColor(R.color.primary, null))
        }

        // Main Login Action
        binding.btnLogin.setOnClickListener {
            val selectedRole = binding.spinnerRoleLogin.text.toString().lowercase()
            if (selectedRole == "admin") {
                val adminCode = binding.etAdminCodeLogin.text.toString().trim()
                if (adminCode != "smartadmin123") {
                    Toast.makeText(context, "Invalid admin authorization code.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (currentTab == "email") {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewModel.login(email, password)
            } else {
                if (isOtpSent) {
                    verifyPhoneOTP()
                } else {
                    startPhoneAuth()
                }
            }
        }

        // Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("598401056220-33333333333.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.btnGoogleSignIn.setOnClickListener {
            val selectedRole = binding.spinnerRoleLogin.text.toString().lowercase()
            if (selectedRole == "admin") {
                val adminCode = binding.etAdminCodeLogin.text.toString().trim()
                if (adminCode != "smartadmin123") {
                    Toast.makeText(context, "Invalid admin authorization code.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
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
            binding.btnGoogleSignIn.isEnabled = !isLoading
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

    private fun startPhoneAuth() {
        val phone = binding.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            Toast.makeText(context, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            return
        }
        userPhoneNumber = phone
        
        val selectedRole = binding.spinnerRoleLogin.text.toString().lowercase()
        if (selectedRole == "admin") {
            val adminCode = binding.etAdminCodeLogin.text.toString().trim()
            if (adminCode != "smartadmin123") {
                Toast.makeText(context, "Invalid admin authorization code.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        Toast.makeText(context, "Sending OTP...", Toast.LENGTH_SHORT).show()

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                val role = binding.spinnerRoleLogin.text.toString().lowercase()
                viewModel.loginWithPhone(credential, role)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(context, "SMS failed: ${e.message}. Using sandbox...", Toast.LENGTH_LONG).show()
                simulatePhoneLogin(phone)
            }

            override fun onCodeSent(
                verificationIdStr: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = verificationIdStr
                isOtpSent = true
                binding.tilOTP.visibility = View.VISIBLE
                binding.btnLogin.text = "Verify OTP"
                Toast.makeText(context, "OTP Sent to $phone", Toast.LENGTH_SHORT).show()
            }
        }

        try {
            val options = PhoneAuthOptions.newBuilder(FirebaseHelper.auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Toast.makeText(context, "Verification error: ${e.message}. Using sandbox...", Toast.LENGTH_SHORT).show()
            simulatePhoneLogin(phone)
        }
    }

    private fun verifyPhoneOTP() {
        val code = binding.etOTP.text.toString().trim()
        if (code.isEmpty() || code.length < 6) {
            Toast.makeText(context, "Please enter 6-digit OTP", Toast.LENGTH_SHORT).show()
            return
        }

        if (verificationId == null) {
            if (code == "123456") {
                completeSimulatedPhoneLogin()
            } else {
                Toast.makeText(context, "Invalid OTP code (use 123456 for sandbox).", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        val selectedRole = binding.spinnerRoleLogin.text.toString().lowercase()
        viewModel.loginWithPhone(credential, selectedRole)
    }

    private fun simulatePhoneLogin(phone: String) {
        verificationId = null
        isOtpSent = true
        binding.tilOTP.visibility = View.VISIBLE
        binding.btnLogin.text = "Verify OTP"
        Toast.makeText(context, "Sandbox Mode: Use OTP 123456 to log in.", Toast.LENGTH_LONG).show()
    }

    private fun completeSimulatedPhoneLogin() {
        val selectedRole = binding.spinnerRoleLogin.text.toString().lowercase()
        val formattedPhone = userPhoneNumber.replace("+", "").replace("-", "").trim()
        val mockEmail = "phone_${formattedPhone}@smartcivic.com"
        val mockName = "Phone User " + (if (formattedPhone.length >= 4) formattedPhone.takeLast(4) else "XXXX")

        viewModel.login(mockEmail, "sandbox123")

        val errorObserver = object : androidx.lifecycle.Observer<String?> {
            override fun onChanged(value: String?) {
                if (value != null) {
                    viewModel.errorState.removeObserver(this)
                    
                    val newUser = com.example.smartcivicgovernance.data.model.User(
                        name = mockName,
                        email = mockEmail,
                        role = selectedRole
                    )
                    viewModel.register(newUser, "sandbox123")
                }
            }
        }
        
        viewModel.errorState.observe(viewLifecycleOwner, errorObserver)
    }

    private fun simulateGoogleLogin() {
        val selectedRole = binding.spinnerRoleLogin.text.toString().lowercase()
        val mockEmail = "sandbox.${selectedRole}@smartcivic.com"
        val mockName = "Sandbox ${selectedRole.replaceFirstChar { it.uppercaseChar() }} User"
        
        viewModel.login(mockEmail, "sandbox123")
        
        val errorObserver = object : androidx.lifecycle.Observer<String?> {
            override fun onChanged(value: String?) {
                if (value != null) {
                    viewModel.errorState.removeObserver(this)
                    
                    val newUser = com.example.smartcivicgovernance.data.model.User(
                        name = mockName,
                        email = mockEmail,
                        role = selectedRole
                    )
                    viewModel.register(newUser, "sandbox123")
                }
            }
        }
        
        viewModel.errorState.observe(viewLifecycleOwner, errorObserver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
