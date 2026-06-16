package com.example.smartcivicgovernance.ui.complaint

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.smartcivicgovernance.R
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.databinding.ActivityComplaintDetailBinding

class ComplaintDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComplaintDetailBinding
    private val viewModel: ComplaintViewModel by viewModels()
    private var complaintId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComplaintDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        complaintId = intent.getStringExtra("COMPLAINT_ID") ?: ""

        setupListeners()
        setupObservers()

        if (complaintId.isNotEmpty()) {
            viewModel.loadComplaintDetails(complaintId)
        } else {
            Toast.makeText(this, "Invalid Complaint ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupListeners() {
        binding.btnSubmitRating.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val feedback = binding.etFeedback.text.toString().trim()

            if (rating == 0) {
                Toast.makeText(this, "Please select at least 1 star rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update rating in database
            val updates = hashMapOf<String, Any?>(
                "citizenRating" to rating,
                "citizenFeedback" to feedback
            )
            com.example.smartcivicgovernance.data.remote.FirebaseHelper.db.collection("complaints")
                .document(complaintId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show()
                    binding.cardRatingForm.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error submitting rating: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupObservers() {
        viewModel.complaintDetails.observe(this) { complaint ->
            if (complaint != null) {
                bindDetails(complaint)
            }
        }
    }

    private fun bindDetails(complaint: Complaint) {
        binding.tvDetailTitle.text = complaint.title
        binding.tvDetailAddress.text = complaint.address
        binding.tvDetailDescription.text = complaint.description
        binding.tvDetailDate.text = "Reported: ${complaint.createdAt?.toDate()?.toString() ?: ""}"
        binding.chipCategory.text = complaint.category
        binding.chipPriority.text = "Priority: ${complaint.priority}"

        // Set category icon
        val iconRes = when (complaint.category) {
            "Pothole" -> R.drawable.ic_pothole
            "Garbage" -> R.drawable.ic_garbage
            "Water Leakage" -> R.drawable.ic_water
            "Drainage" -> R.drawable.ic_drainage
            "Streetlight" -> R.drawable.ic_streetlight
            else -> R.drawable.ic_traffic
        }
        binding.chipCategory.setChipIconResource(iconRes)

        // Load reported image
        if (complaint.imageUrl.isNotEmpty()) {
            binding.ivDetailImage.visibility = View.VISIBLE
            Glide.with(this).load(complaint.imageUrl).into(binding.ivDetailImage)
        } else {
            binding.ivDetailImage.visibility = View.GONE
        }

        // Draw custom timeline
        updateTimeline(complaint.status)

        // Worker Info Card
        if (complaint.workerId != null) {
            binding.cardWorkerInfo.visibility = View.VISIBLE
            binding.tvWorkerName.text = complaint.workerName
            binding.tvWorkerStatus.text = "Status: ${complaint.status}"
        } else {
            binding.cardWorkerInfo.visibility = View.GONE
        }

        // Proof of Work Card
        if (complaint.status == "Resolved" && !complaint.proofImageUrl.isNullOrEmpty()) {
            binding.cardProof.visibility = View.VISIBLE
            Glide.with(this).load(complaint.proofImageUrl).into(binding.ivProofImage)
        } else {
            binding.cardProof.visibility = View.GONE
        }

        // Rating Form Visibility
        val currentUid = com.example.smartcivicgovernance.data.remote.FirebaseHelper.getCurrentUid()
        if (complaint.status == "Resolved" && 
            complaint.citizenId == currentUid && 
            complaint.citizenRating == null) {
            binding.cardRatingForm.visibility = View.VISIBLE
        } else {
            binding.cardRatingForm.visibility = View.GONE
        }
    }

    private fun updateTimeline(status: String) {
        val activeColor = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.divider_light)
        val pendingColor = ContextCompat.getColor(this, R.color.status_pending)
        val resolvedColor = ContextCompat.getColor(this, R.color.status_resolved)

        when (status) {
            "Pending" -> {
                binding.ivStep1.setColorFilter(pendingColor)
                binding.ivStep2.setColorFilter(inactiveColor)
                binding.ivStep3.setColorFilter(inactiveColor)
            }
            "In Progress" -> {
                binding.ivStep1.setColorFilter(activeColor)
                binding.ivStep2.setColorFilter(activeColor)
                binding.ivStep3.setColorFilter(inactiveColor)
            }
            "Resolved" -> {
                binding.ivStep1.setColorFilter(activeColor)
                binding.ivStep2.setColorFilter(activeColor)
                binding.ivStep3.setColorFilter(resolvedColor)
            }
            "Rejected" -> {
                binding.ivStep1.setColorFilter(ContextCompat.getColor(this, R.color.status_rejected))
                binding.ivStep2.setColorFilter(inactiveColor)
                binding.ivStep3.setColorFilter(inactiveColor)
            }
        }
    }
}
