package com.example.smartcivicgovernance.ui.citizen

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.databinding.ActivityCitizenDashboardBinding
import com.example.smartcivicgovernance.ui.complaint.ReportComplaintActivity
import com.example.smartcivicgovernance.ui.complaint.MapViewActivity

class CitizenDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitizenDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitizenDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.citizenNavHost) as NavHostFragment
        val navController = navHostFragment.navController

        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(R.id.citizenHomeFragment)
        navController.graph = navGraph

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.citizenHomeFragment,
                R.id.myComplaintsFragment,
                R.id.citizenLeaderboardFragment,
                R.id.citizenProfileFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        binding.fabReport.setOnClickListener {
            val intent = Intent(this, ReportComplaintActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Add Map Icon to Toolbar
        val mapItem = menu?.add(Menu.NONE, 101, Menu.NONE, "Map")
        mapItem?.setIcon(R.drawable.ic_map)
        mapItem?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 101) {
            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
