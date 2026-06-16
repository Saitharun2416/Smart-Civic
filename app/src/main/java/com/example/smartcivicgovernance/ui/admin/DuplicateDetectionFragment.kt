package com.example.smartcivicgovernance.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.databinding.FragmentDuplicateDetectionBinding
import com.example.smartcivicgovernance.ui.adapter.ComplaintAdapter

class DuplicateDetectionFragment : Fragment() {

    private var _binding: FragmentDuplicateDetectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var complaintAdapter: ComplaintAdapter
    private var rawComplaintsList: List<Complaint> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDuplicateDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()

        viewModel.loadAllComplaints()
    }

    private fun setupRecyclerView() {
        complaintAdapter = ComplaintAdapter { complaint ->
            showDuplicateActionsDialog(complaint)
        }
        binding.rvDuplicatesList.layoutManager = LinearLayoutManager(context)
        binding.rvDuplicatesList.adapter = complaintAdapter
    }

    private fun setupObservers() {
        viewModel.complaints.observe(viewLifecycleOwner) { complaints ->
            rawComplaintsList = complaints
            applyFilters()
        }

        viewModel.actionSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Action recorded successfully", Toast.LENGTH_SHORT).show()
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
        val filtered = rawComplaintsList.filter { it.isDuplicate }
        complaintAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.tvDuplicatesEmpty.visibility = View.VISIBLE
            binding.rvDuplicatesList.visibility = View.GONE
        } else {
            binding.tvDuplicatesEmpty.visibility = View.GONE
            binding.rvDuplicatesList.visibility = View.VISIBLE
        }
    }

    private fun showDuplicateActionsDialog(complaint: Complaint) {
        AlertDialog.Builder(requireContext())
            .setTitle("Resolve Duplicate Complaint")
            .setMessage("Select an action for: ${complaint.title}\nAddress: ${complaint.address}")
            .setPositiveButton("Merge (Delete Duplicate)") { _, _ ->
                viewModel.resolveDuplicateComplaint(complaint.complaintId, dismiss = false)
            }
            .setNeutralButton("Dismiss (Keep as Separate Issue)") { _, _ ->
                viewModel.resolveDuplicateComplaint(complaint.complaintId, dismiss = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
