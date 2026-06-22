package com.example.smartcivicgovernance.ui.complaint

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.data.model.toComplaintsSafe
import com.example.smartcivicgovernance.databinding.ActivityMapViewBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.smartcivicgovernance.data.remote.FirebaseHelper

class MapViewActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapViewBinding
    private val viewModel: ComplaintViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private val markerComplaintMap = HashMap<String, String>() // markerId -> complaintId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Default center
        val center = LatLng(20.5937, 78.9629) // Central India / General area
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 5f))

        setupObservers()

        googleMap?.setOnInfoWindowClickListener { marker ->
            val complaintId = markerComplaintMap[marker.id]
            if (complaintId != null) {
                val intent = Intent(this, ComplaintDetailActivity::class.java).apply {
                    putExtra("COMPLAINT_ID", complaintId)
                }
                startActivity(intent)
            }
        }

        // Fetch all complaints from Firestore
        FirebaseHelper.getQueryWithTimeout(FirebaseHelper.db.collection("complaints"), 3000) { result ->
            result.fold(
                onSuccess = { snapshot ->
                    val complaints = snapshot.toComplaintsSafe()
                    plotComplaints(complaints)
                },
                onFailure = { e ->
                    Toast.makeText(this, "Failed to load complaints: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setupObservers() {
        // Observers can be added if using a ViewModel to stream complaints
    }

    private fun plotComplaints(complaints: List<Complaint>) {
        googleMap?.clear()
        markerComplaintMap.clear()

        if (complaints.isEmpty()) return

        for (complaint in complaints) {
            val position = LatLng(complaint.latitude, complaint.longitude)
            val hue = when (complaint.status) {
                "Pending" -> BitmapDescriptorFactory.HUE_ORANGE
                "In Progress" -> BitmapDescriptorFactory.HUE_AZURE
                "Resolved" -> BitmapDescriptorFactory.HUE_GREEN
                else -> BitmapDescriptorFactory.HUE_RED
            }

            val marker = googleMap?.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(complaint.title)
                    .snippet("Status: ${complaint.status} | Category: ${complaint.category}")
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
            )

            if (marker != null) {
                markerComplaintMap[marker.id] = complaint.complaintId
            }
        }

        // Zoom to the first marker if available
        val first = complaints.first()
        val firstLatLng = LatLng(first.latitude, first.longitude)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLatLng, 12f))
    }
}
