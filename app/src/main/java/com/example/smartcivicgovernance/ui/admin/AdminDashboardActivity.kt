package com.example.smartcivicgovernance.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.repository.UserRepository
import com.example.smartcivicgovernance.databinding.ActivityAdminDashboardBinding
import com.example.smartcivicgovernance.ui.auth.AuthActivity
import com.google.android.material.navigation.NavigationView

class AdminDashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val userRepo = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.adminNavHost) as NavHostFragment
        val navController = navHostFragment.navController

        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(R.id.adminOverviewFragment)
        navController.graph = navGraph

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.adminOverviewFragment,
                R.id.complaintsManagementFragment,
                R.id.complaintVerificationFragment,
                R.id.userManagementFragment,
                R.id.analyticsFragment,
                R.id.duplicateDetectionFragment
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
        binding.navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.app_name,
            R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.adminNavHost) as NavHostFragment
        val navController = navHostFragment.navController

        when (item.itemId) {
            R.id.adminOverviewFragment -> navController.navigate(R.id.adminOverviewFragment)
            R.id.complaintsManagementFragment -> navController.navigate(R.id.complaintsManagementFragment)
            R.id.complaintVerificationFragment -> navController.navigate(R.id.complaintVerificationFragment)
            R.id.userManagementFragment -> navController.navigate(R.id.userManagementFragment)
            R.id.analyticsFragment -> navController.navigate(R.id.analyticsFragment)
            R.id.duplicateDetectionFragment -> navController.navigate(R.id.duplicateDetectionFragment)
            R.id.adminLogout -> {
                userRepo.logout()
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
