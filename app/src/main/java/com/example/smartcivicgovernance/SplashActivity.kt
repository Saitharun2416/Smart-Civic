package com.example.smartcivicgovernance

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcivicgovernance.data.remote.FirebaseHelper
import com.example.smartcivicgovernance.ui.auth.AuthActivity
import com.example.smartcivicgovernance.ui.citizen.CitizenDashboardActivity
import com.example.smartcivicgovernance.ui.worker.WorkerDashboardActivity
import com.example.smartcivicgovernance.ui.admin.AdminDashboardActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Give a short delay to show the animated layout
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 1500)
    }

    private fun checkUserSession() {
        val currentUser = FirebaseHelper.auth.currentUser
        if (currentUser != null) {
            FirebaseHelper.checkUserStatus { role, disabled ->
                if (role != null) {
                    if (disabled) {
                        val msg = if (role == "worker") {
                            "Your registration is pending admin approval. You will be able to access the app once approved."
                        } else {
                            "Your account is disabled. Please contact support."
                        }
                        Toast.makeText(this@SplashActivity, msg, Toast.LENGTH_LONG).show()
                        FirebaseHelper.auth.signOut()
                        navigateToAuth()
                    } else {
                        navigateToDashboard(role)
                    }
                } else {
                    Toast.makeText(this, "Session expired. Please sign in again.", Toast.LENGTH_LONG).show()
                    navigateToAuth()
                }
            }
        } else {
            navigateToAuth()
        }
    }

    private fun navigateToDashboard(role: String) {
        val intent = when (role) {
            "citizen" -> Intent(this, CitizenDashboardActivity::class.java)
            "worker" -> Intent(this, WorkerDashboardActivity::class.java)
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            else -> Intent(this, AuthActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
}
