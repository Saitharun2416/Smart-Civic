package com.example.smartcivicgovernance.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.databinding.FragmentComplaintVerificationBinding
import com.example.smartcivicgovernance.databinding.DialogVerifyProofBinding
import com.example.smartcivicgovernance.ui.adapter.ComplaintAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog

class ComplaintVerificationFragment : Fragment() {

    private var _binding: FragmentComplaintVerificationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var complaintAdapter: ComplaintAdapter
    private var rawComplaintsList: List<Complaint> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentComplaintVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()

        binding.swipeRefreshVerification.setOnRefreshListener {
            viewModel.loadAllComplaints()
        }

        viewModel.loadAllComplaints()
    }

    private fun setupRecyclerView() {
        complaintAdapter = ComplaintAdapter { complaint ->
            showVerificationDialog(complaint)
        }
        binding.rvVerificationList.layoutManager = LinearLayoutManager(context)
        binding.rvVerificationList.adapter = complaintAdapter
    }

    private fun setupObservers() {
        viewModel.complaints.observe(viewLifecycleOwner) { complaints ->
            rawComplaintsList = complaints
            applyFilters()
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshVerification.isRefreshing = isLoading
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Verification action recorded", Toast.LENGTH_SHORT).show()
                viewModel.loadAllComplaints()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilters() {
        // Filter complaints with status "Verification Pending"
        val filtered = rawComplaintsList.filter { it.status == "Verification Pending" }
        complaintAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.tvVerifyEmpty.visibility = View.VISIBLE
            binding.rvVerificationList.visibility = View.GONE
        } else {
            binding.tvVerifyEmpty.visibility = View.GONE
            binding.rvVerificationList.visibility = View.VISIBLE
        }
    }

    private fun showVerificationDialog(complaint: Complaint) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogVerifyProofBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvProofTitle.text = complaint.title
        dialogBinding.tvProofWorker.text = "Resolved by: ${complaint.workerName ?: "Unknown"}"

        Glide.with(this)
            .load(complaint.proofImageUrl)
            .into(dialogBinding.ivProofImage)

        dialogBinding.btnApprove.setOnClickListener {
            viewModel.verifyComplaintResolution(complaint.complaintId, approve = true)
            dialog.dismiss()
        }

        dialogBinding.btnReject.setOnClickListener {
            viewModel.verifyComplaintResolution(complaint.complaintId, approve = false)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
