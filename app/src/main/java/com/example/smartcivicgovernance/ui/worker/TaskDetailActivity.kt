package com.example.smartcivicgovernance.ui.worker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.data.remote.FirebaseHelper
import com.example.smartcivicgovernance.databinding.ActivityTaskDetailBinding
import com.example.smartcivicgovernance.ui.complaint.ComplaintViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class TaskDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityTaskDetailBinding
    private val viewModel: ComplaintViewModel by viewModels()
    private val workerViewModel: WorkerViewModel by viewModels()

    private var complaintId: String = ""
    private var googleMap: GoogleMap? = null
    private var complaintLatLng: LatLng? = null
    private var workerName: String = "Worker"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        complaintId = intent.getStringExtra("COMPLAINT_ID") ?: ""

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fetchWorkerName()
        setupObservers()
    }

    private fun fetchWorkerName() {
        val uid = FirebaseHelper.getCurrentUid() ?: return
        FirebaseHelper.getDocWithTimeout(FirebaseHelper.db.collection("users").document(uid), 3000) { result ->
            result.fold(
                onSuccess = { doc ->
                    workerName = doc.getString("name") ?: "Worker"
                },
                onFailure = {
                    workerName = "Worker"
                }
            )
        }
    }

    private fun setupObservers() {
        viewModel.complaintDetails.observe(this) { complaint ->
            if (complaint != null) {
                bindDetails(complaint)
            }
        }

        workerViewModel.actionSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Task accepted!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        workerViewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindDetails(complaint: Complaint) {
        binding.tvTaskTitle.text = complaint.title
        binding.tvTaskAddress.text = complaint.address
        binding.tvTaskDescription.text = complaint.description
        binding.tvTaskCitizen.text = "Reported by: ${complaint.citizenName}"
        binding.chipCategory.text = complaint.category

        if (complaint.imageUrl.isNotEmpty()) {
            binding.ivTaskImage.visibility = View.VISIBLE
            Glide.with(this).load(complaint.imageUrl).into(binding.ivTaskImage)
        } else {
            binding.ivTaskImage.visibility = View.GONE
        }

        complaintLatLng = LatLng(complaint.latitude, complaint.longitude)
        plotMarker()

        // Configure Context-Aware Action Button
        val uid = FirebaseHelper.getCurrentUid()
        if (complaint.status == "Pending") {
            binding.btnAction.text = "Accept Task"
            binding.btnAction.setOnClickListener {
                workerViewModel.acceptTask(complaint.complaintId, workerName)
            }
        } else if (complaint.status == "In Progress" && complaint.workerId == uid) {
            binding.btnAction.text = "Mark Complete"
            binding.btnAction.setOnClickListener {
                val intent = Intent(this, SubmitProofActivity::class.java).apply {
                    putExtra("COMPLAINT_ID", complaint.complaintId)
                }
                startActivity(intent)
                finish()
            }
        } else {
            binding.btnAction.visibility = View.GONE
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        plotMarker()

        if (complaintId.isNotEmpty()) {
            viewModel.loadComplaintDetails(complaintId)
        }
    }

    private fun plotMarker() {
        val latLng = complaintLatLng ?: return
        val map = googleMap ?: return
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title("Issue Location"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }
}
